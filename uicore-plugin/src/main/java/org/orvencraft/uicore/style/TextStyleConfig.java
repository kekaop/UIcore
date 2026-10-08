package org.orvencraft.uicore.style;

import org.orvencraft.uicore.ColorParser;
import org.orvencraft.uicore.util.TextContentUtils;
import java.util.Locale;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

public final class TextStyleConfig {
   private static final TextStyleConfig EMPTY = new TextStyleConfig(null, null, null, null, null, null, null, "instant", 0);

   private final String text;
   private final String color;
   private final Boolean bold;
   private final Boolean italic;
   private final Boolean underlined;
   private final Boolean strikethrough;
   private final Boolean obfuscated;
   private final String transitionType;
   private final int transitionDuration;

   private TextStyleConfig(
      String text,
      String color,
      Boolean bold,
      Boolean italic,
      Boolean underlined,
      Boolean strikethrough,
      Boolean obfuscated,
      String transitionType,
      int transitionDuration
   ) {
      this.text = blankToNull(text);
      this.color = blankToNull(color);
      this.bold = bold;
      this.italic = italic;
      this.underlined = underlined;
      this.strikethrough = strikethrough;
      this.obfuscated = obfuscated;
      this.transitionType = transitionType == null || transitionType.isBlank() ? "instant" : transitionType.trim().toLowerCase(Locale.ROOT);
      this.transitionDuration = Math.max(0, transitionDuration);
   }

   public static TextStyleConfig empty() {
      return EMPTY;
   }

   public static TextStyleConfig from(ConfigurationSection section) {
      if (section == null) {
         return EMPTY;
      }

      ConfigurationSection decorations = section.getConfigurationSection("decorations");
      ConfigurationSection transition = section.getConfigurationSection("transition");
      return new TextStyleConfig(
         TextContentUtils.normalizeLineBreaks(section.getString("text", null)),
         section.getString("color", null),
         readBoolean(section, decorations, "bold"),
         readBoolean(section, decorations, "italic"),
         firstBoolean(section, decorations, "underlined", "underline"),
         firstBoolean(section, decorations, "strikethrough", "strike"),
         firstBoolean(section, decorations, "obfuscated", "magic"),
         transition == null ? section.getString("transition.type", "instant") : transition.getString("type", "instant"),
         transition == null ? section.getInt("transition.duration", 0) : transition.getInt("duration", 0)
      );
   }

   public boolean isEmpty() {
      return this.text == null
         && this.color == null
         && this.bold == null
         && this.italic == null
         && this.underlined == null
         && this.strikethrough == null
         && this.obfuscated == null;
   }

   public TextStyleConfig merge(TextStyleConfig override) {
      if (override == null || override == EMPTY || override.isEmpty()) {
         return this;
      }

      return new TextStyleConfig(
         override.text != null ? override.text : this.text,
         override.color != null ? override.color : this.color,
         override.bold != null ? override.bold : this.bold,
         override.italic != null ? override.italic : this.italic,
         override.underlined != null ? override.underlined : this.underlined,
         override.strikethrough != null ? override.strikethrough : this.strikethrough,
         override.obfuscated != null ? override.obfuscated : this.obfuscated,
         !"instant".equals(override.transitionType) ? override.transitionType : this.transitionType,
         override.transitionDuration > 0 ? override.transitionDuration : this.transitionDuration
      );
   }

   public TextStyleConfig withColor(String colorOverride) {
      return new TextStyleConfig(
         this.text,
         colorOverride,
         this.bold,
         this.italic,
         this.underlined,
         this.strikethrough,
         this.obfuscated,
         this.transitionType,
         this.transitionDuration
      );
   }

   public String color() {
      return this.color;
   }

   public boolean smoothTransition() {
      return "smooth".equalsIgnoreCase(this.transitionType) && this.transitionDuration > 0;
   }

   public int transitionDuration() {
      return this.transitionDuration;
   }

   public String render(String baseText) {
      return this.render(baseText, null);
   }

   public String render(String baseText, String colorOverride) {
      TextStyleConfig effective = colorOverride == null || colorOverride.isBlank() ? this : this.withColor(colorOverride);
      if (effective.isEmpty() && effective.text == null) {
         return ColorParser.toLegacyString(baseText);
      }

      String body = effective.text == null ? escapeMiniMessage(plainVisibleText(baseText)) : effective.text;
      return ColorParser.toLegacyString(effective.openingTags() + body);
   }

   /** Keeps font/glyph and other Component metadata through style application. */
   public net.kyori.adventure.text.Component renderComponent(String baseText, String colorOverride,
      java.util.function.Function<String, net.kyori.adventure.text.Component> renderer) {
      TextStyleConfig effective = colorOverride == null || colorOverride.isBlank() ? this : this.withColor(colorOverride);
      net.kyori.adventure.text.Component component = renderer.apply(effective.text == null ? baseText : effective.text);
      return applyComponentStyle(component, effective);
   }

   private static net.kyori.adventure.text.Component applyComponentStyle(net.kyori.adventure.text.Component component, TextStyleConfig style) {
      if (style.color != null) {
         net.kyori.adventure.text.format.TextColor color = style.color.startsWith("#")
            ? net.kyori.adventure.text.format.TextColor.fromHexString(style.color)
            : net.kyori.adventure.text.format.NamedTextColor.NAMES.value(style.color.toLowerCase(Locale.ROOT).replace('-', '_'));
         if (color != null) component = component.color(color);
      }
      if (style.bold != null) component = component.decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, style.bold);
      if (style.italic != null) component = component.decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, style.italic);
      if (style.underlined != null) component = component.decoration(net.kyori.adventure.text.format.TextDecoration.UNDERLINED, style.underlined);
      if (style.strikethrough != null) component = component.decoration(net.kyori.adventure.text.format.TextDecoration.STRIKETHROUGH, style.strikethrough);
      if (style.obfuscated != null) component = component.decoration(net.kyori.adventure.text.format.TextDecoration.OBFUSCATED, style.obfuscated);
      java.util.List<net.kyori.adventure.text.Component> children = new java.util.ArrayList<>();
      for (net.kyori.adventure.text.Component child : component.children()) children.add(applyComponentStyle(child, style));
      return component.children(children);
   }

   public static String toHex(int r, int g, int b) {
      return String.format(Locale.ROOT, "#%02x%02x%02x", clamp(r), clamp(g), clamp(b));
   }

   public static int[] parseRgb(String raw) {
      if (raw == null || raw.isBlank()) {
         return null;
      }

      String value = raw.trim().toLowerCase(Locale.ROOT);
      if (value.startsWith("<") && value.endsWith(">")) {
         value = value.substring(1, value.length() - 1).trim();
      }

      if (value.startsWith("#") && value.length() == 7) {
         try {
            return new int[]{
               Integer.parseInt(value.substring(1, 3), 16),
               Integer.parseInt(value.substring(3, 5), 16),
               Integer.parseInt(value.substring(5, 7), 16)
            };
         } catch (NumberFormatException ignored) {
            return null;
         }
      }

      return switch (value) {
         case "black" -> new int[]{0, 0, 0};
         case "dark_blue" -> new int[]{0, 0, 170};
         case "dark_green" -> new int[]{0, 170, 0};
         case "dark_aqua" -> new int[]{0, 170, 170};
         case "dark_red" -> new int[]{170, 0, 0};
         case "dark_purple" -> new int[]{170, 0, 170};
         case "gold" -> new int[]{255, 170, 0};
         case "gray", "grey" -> new int[]{170, 170, 170};
         case "dark_gray", "dark_grey" -> new int[]{85, 85, 85};
         case "blue" -> new int[]{85, 85, 255};
         case "green" -> new int[]{85, 255, 85};
         case "aqua" -> new int[]{85, 255, 255};
         case "red" -> new int[]{255, 85, 85};
         case "light_purple" -> new int[]{255, 85, 255};
         case "yellow" -> new int[]{255, 255, 85};
         case "white" -> new int[]{255, 255, 255};
         default -> null;
      };
   }

   private String openingTags() {
      StringBuilder out = new StringBuilder();
      String colorTag = this.colorTag();
      if (colorTag != null) {
         out.append(colorTag);
      }
      if (Boolean.TRUE.equals(this.bold)) {
         out.append("<bold>");
      }
      if (Boolean.TRUE.equals(this.italic)) {
         out.append("<italic>");
      }
      if (Boolean.TRUE.equals(this.underlined)) {
         out.append("<underlined>");
      }
      if (Boolean.TRUE.equals(this.strikethrough)) {
         out.append("<strikethrough>");
      }
      if (Boolean.TRUE.equals(this.obfuscated)) {
         out.append("<obfuscated>");
      }
      return out.toString();
   }

   private String colorTag() {
      if (this.color == null || this.color.isBlank()) {
         return null;
      }

      String raw = this.color.trim();
      if (raw.startsWith("<") && raw.endsWith(">")) {
         return raw;
      }
      if (raw.startsWith("#") && raw.length() == 7) {
         return "<" + raw + ">";
      }
      return "<" + raw.toLowerCase(Locale.ROOT).replace('-', '_') + ">";
   }

   private static String plainVisibleText(String input) {
      String stripped = ChatColor.stripColor(ColorParser.toLegacyString(input));
      return stripped == null ? "" : stripped;
   }

   private static String escapeMiniMessage(String input) {
      if (input == null) {
         return "";
      }
      return input.replace("\\", "\\\\").replace("<", "\\<");
   }

   private static Boolean readBoolean(ConfigurationSection primary, ConfigurationSection nested, String key) {
      if (primary != null && primary.contains(key)) {
         return primary.getBoolean(key);
      }
      if (nested != null && nested.contains(key)) {
         return nested.getBoolean(key);
      }
      return null;
   }

   private static Boolean firstBoolean(ConfigurationSection primary, ConfigurationSection nested, String... keys) {
      for (String key : keys) {
         Boolean value = readBoolean(primary, nested, key);
         if (value != null) {
            return value;
         }
      }
      return null;
   }

   private static String blankToNull(String value) {
      return value == null || value.isBlank() ? null : value.trim();
   }

   private static int clamp(int value) {
      return Math.max(0, Math.min(255, value));
   }
}
