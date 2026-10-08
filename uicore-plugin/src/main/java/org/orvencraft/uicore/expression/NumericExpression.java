package org.orvencraft.uicore.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A small deterministic numeric expression language for menu coordinates. */
public final class NumericExpression {
   private final String source;
   private final Node root;

   private NumericExpression(String source, Node root) {
      this.source = source;
      this.root = root;
   }

   public static NumericExpression compile(String source) {
      if (source == null || source.isBlank()) {
         throw new IllegalArgumentException("Expression must not be blank");
      }

      Parser parser = new Parser(source);
      Node root = parser.parseExpression();
      parser.skipWhitespace();
      if (!parser.atEnd()) {
         throw parser.error("Unexpected token");
      }
      return new NumericExpression(source, root);
   }

   public double evaluate(long elapsedTicks, ExternalValueResolver externalValues) {
      ExternalValueResolver resolver = externalValues == null ? reference -> {
         throw new IllegalArgumentException("No value resolver for " + reference);
      } : externalValues;
      double value = this.root.evaluate(new EvaluationContext(elapsedTicks, resolver));
      return requireFinite(value, this.source);
   }

   public String source() {
      return this.source;
   }

   @FunctionalInterface
   public interface ExternalValueResolver {
      double resolve(String reference);
   }

   private interface Node {
      double evaluate(EvaluationContext context);
   }

   private record EvaluationContext(long elapsedTicks, ExternalValueResolver externalValues) {
      double variable(String name) {
         return switch (name) {
            case "t" -> this.elapsedTicks / 20.0;
            case "ticks" -> this.elapsedTicks;
            case "pi" -> Math.PI;
            case "e" -> Math.E;
            default -> throw new IllegalArgumentException("Unknown variable: " + name);
         };
      }
   }

   private record ConstantNode(double value) implements Node {
      @Override
      public double evaluate(EvaluationContext context) {
         return this.value;
      }
   }

   private record VariableNode(String name) implements Node {
      @Override
      public double evaluate(EvaluationContext context) {
         return context.variable(this.name);
      }
   }

   private record ExternalNode(String reference) implements Node {
      @Override
      public double evaluate(EvaluationContext context) {
         return requireFinite(context.externalValues.resolve(this.reference), this.reference);
      }
   }

   private record UnaryNode(char operator, Node operand) implements Node {
      @Override
      public double evaluate(EvaluationContext context) {
         double value = this.operand.evaluate(context);
         return this.operator == '-' ? -value : value;
      }
   }

   private record BinaryNode(char operator, Node left, Node right) implements Node {
      @Override
      public double evaluate(EvaluationContext context) {
         double leftValue = this.left.evaluate(context);
         double rightValue = this.right.evaluate(context);
         double result = switch (this.operator) {
            case '+' -> leftValue + rightValue;
            case '-' -> leftValue - rightValue;
            case '*' -> leftValue * rightValue;
            case '/' -> {
               if (rightValue == 0.0) {
                  throw new ArithmeticException("Division by zero");
               }
               yield leftValue / rightValue;
            }
            case '%' -> {
               if (rightValue == 0.0) {
                  throw new ArithmeticException("Modulo by zero");
               }
               yield leftValue % rightValue;
            }
            default -> throw new IllegalStateException("Unsupported operator: " + this.operator);
         };
         return requireFinite(result, Character.toString(this.operator));
      }
   }

   private record FunctionNode(String name, List<Node> arguments) implements Node {
      @Override
      public double evaluate(EvaluationContext context) {
         double[] values = new double[this.arguments.size()];
         for (int index = 0; index < this.arguments.size(); index++) {
            values[index] = this.arguments.get(index).evaluate(context);
         }

         double result = switch (this.name) {
            case "min" -> Math.min(values[0], values[1]);
            case "max" -> Math.max(values[0], values[1]);
            case "clamp" -> clamp(values[0], values[1], values[2]);
            case "normalize" -> normalize(values[0], values[1], values[2]);
            case "map" -> values[3] + normalize(values[0], values[1], values[2]) * (values[4] - values[3]);
            case "lerp" -> values[0] + (values[1] - values[0]) * values[2];
            case "abs" -> Math.abs(values[0]);
            case "sin" -> Math.sin(values[0]);
            case "cos" -> Math.cos(values[0]);
            default -> throw new IllegalStateException("Unsupported function: " + this.name);
         };
         return requireFinite(result, this.name);
      }

      private static double normalize(double value, double minimum, double maximum) {
         if (minimum == maximum) {
            throw new ArithmeticException("normalize range must not be zero");
         }
         return clamp((value - minimum) / (maximum - minimum), 0.0, 1.0);
      }

      private static double clamp(double value, double first, double second) {
         double minimum = Math.min(first, second);
         double maximum = Math.max(first, second);
         return Math.max(minimum, Math.min(maximum, value));
      }
   }

   private static double requireFinite(double value, String source) {
      if (!Double.isFinite(value)) {
         throw new ArithmeticException("Non-finite result in " + source);
      }
      return value;
   }

   private static final class Parser {
      private final String source;
      private int position;

      private Parser(String source) {
         this.source = source;
      }

      private Node parseExpression() {
         Node node = this.parseTerm();
         while (true) {
            this.skipWhitespace();
            if (this.consume('+')) {
               node = new BinaryNode('+', node, this.parseTerm());
            } else if (this.consume('-')) {
               node = new BinaryNode('-', node, this.parseTerm());
            } else {
               return node;
            }
         }
      }

      private Node parseTerm() {
         Node node = this.parseUnary();
         while (true) {
            this.skipWhitespace();
            if (this.consume('*')) {
               node = new BinaryNode('*', node, this.parseUnary());
            } else if (this.consume('/')) {
               node = new BinaryNode('/', node, this.parseUnary());
            } else if (this.consume('%')) {
               node = new BinaryNode('%', node, this.parseUnary());
            } else {
               return node;
            }
         }
      }

      private Node parseUnary() {
         this.skipWhitespace();
         if (this.consume('+')) {
            return new UnaryNode('+', this.parseUnary());
         }
         if (this.consume('-')) {
            return new UnaryNode('-', this.parseUnary());
         }
         return this.parsePrimary();
      }

      private Node parsePrimary() {
         this.skipWhitespace();
         if (this.atEnd()) {
            throw this.error("Expected a value");
         }
         if (this.consume('(')) {
            Node node = this.parseExpression();
            this.skipWhitespace();
            if (!this.consume(')')) {
               throw this.error("Expected ')'");
            }
            return node;
         }
         if (this.peek() == '%') {
            return new ExternalNode(this.parsePercentReference());
         }
         if (this.peek() == '{') {
            return new ExternalNode(this.parseBraceReference());
         }
         if (Character.isDigit(this.peek()) || this.peek() == '.') {
            return new ConstantNode(this.parseNumber());
         }
         if (Character.isLetter(this.peek()) || this.peek() == '_') {
            String identifier = this.parseIdentifier().toLowerCase(Locale.ROOT);
            this.skipWhitespace();
            if (!this.consume('(')) {
               if (!List.of("t", "ticks", "pi", "e").contains(identifier)) {
                  throw this.error("Unknown variable '" + identifier + "'");
               }
               return new VariableNode(identifier);
            }

            List<Node> arguments = new ArrayList<>();
            this.skipWhitespace();
            if (!this.consume(')')) {
               do {
                  arguments.add(this.parseExpression());
                  this.skipWhitespace();
               } while (this.consume(','));
               if (!this.consume(')')) {
                  throw this.error("Expected ')' after function arguments");
               }
            }
            validateFunction(identifier, arguments.size(), this);
            return new FunctionNode(identifier, List.copyOf(arguments));
         }
         throw this.error("Expected a number, variable, placeholder, or function");
      }

      private double parseNumber() {
         int start = this.position;
         boolean digitSeen = false;
         while (!this.atEnd() && Character.isDigit(this.peek())) {
            digitSeen = true;
            this.position++;
         }
         if (!this.atEnd() && this.peek() == '.') {
            this.position++;
            while (!this.atEnd() && Character.isDigit(this.peek())) {
               digitSeen = true;
               this.position++;
            }
         }
         if (!digitSeen) {
            throw this.error("Invalid number");
         }
         if (!this.atEnd() && (this.peek() == 'e' || this.peek() == 'E')) {
            int exponent = this.position++;
            if (!this.atEnd() && (this.peek() == '+' || this.peek() == '-')) {
               this.position++;
            }
            int exponentStart = this.position;
            while (!this.atEnd() && Character.isDigit(this.peek())) {
               this.position++;
            }
            if (this.position == exponentStart) {
               this.position = exponent;
            }
         }
         try {
            return requireFinite(Double.parseDouble(this.source.substring(start, this.position)), this.source);
         } catch (NumberFormatException | ArithmeticException exception) {
            throw this.error("Invalid number");
         }
      }

      private String parseIdentifier() {
         int start = this.position;
         while (!this.atEnd() && (Character.isLetterOrDigit(this.peek()) || this.peek() == '_')) {
            this.position++;
         }
         return this.source.substring(start, this.position);
      }

      private String parsePercentReference() {
         int start = this.position++;
         int end = this.source.indexOf('%', this.position);
         if (end < 0 || end == this.position) {
            throw this.error("Unclosed PlaceholderAPI reference");
         }
         this.position = end + 1;
         return this.source.substring(start, this.position);
      }

      private String parseBraceReference() {
         int start = this.position++;
         int end = this.source.indexOf('}', this.position);
         if (end < 0 || end == this.position) {
            throw this.error("Unclosed state reference");
         }
         this.position = end + 1;
         String reference = this.source.substring(start, this.position);
         if (!reference.startsWith("{state.") && !reference.matches("\\{[a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*\\}")) {
            throw this.error("Expected {state.key} or {namespace:key} reference");
         }
         return reference;
      }

      private char peek() {
         return this.source.charAt(this.position);
      }

      private boolean consume(char character) {
         if (!this.atEnd() && this.source.charAt(this.position) == character) {
            this.position++;
            return true;
         }
         return false;
      }

      private void skipWhitespace() {
         while (!this.atEnd() && Character.isWhitespace(this.source.charAt(this.position))) {
            this.position++;
         }
      }

      private boolean atEnd() {
         return this.position >= this.source.length();
      }

      private IllegalArgumentException error(String message) {
         return new IllegalArgumentException(message + " at position " + this.position + " in '" + this.source + "'");
      }
   }

   private static void validateFunction(String name, int arguments, Parser parser) {
      int expected = switch (name) {
         case "min", "max" -> 2;
         case "clamp", "normalize", "lerp" -> 3;
         case "map" -> 5;
         case "abs", "sin", "cos" -> 1;
         default -> throw parser.error("Unknown function '" + name + "'");
      };
      if (arguments != expected) {
         throw parser.error("Function '" + name + "' expects " + expected + " arguments, got " + arguments);
      }
   }
}
