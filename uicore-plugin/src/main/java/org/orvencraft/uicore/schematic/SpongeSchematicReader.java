package org.orvencraft.uicore.schematic;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/** Bounded reader for the block portion of Sponge schematic versions 2 and 3. */
public final class SpongeSchematicReader {
   private static final int MAX_COMPRESSED = 8 * 1024 * 1024;
   private static final int MAX_INFLATED = 16 * 1024 * 1024;
   private static final int MAX_BLOCKS = 65_536;
   private static final int MAX_COLLECTION = 100_000;

   private SpongeSchematicReader() {}

   public static SpongeSchematic read(Path file) throws IOException {
      if (Files.size(file) > MAX_COMPRESSED) throw new IOException("Schematic exceeds 8 MiB compressed");
      byte[] inflated;
      try (GZIPInputStream gzip = new GZIPInputStream(Files.newInputStream(file))) {
         inflated = gzip.readNBytes(MAX_INFLATED + 1);
      }
      if (inflated.length > MAX_INFLATED) throw new IOException("Schematic exceeds 16 MiB uncompressed");
      try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(inflated))) {
         if (input.readUnsignedByte() != 10) throw new IOException("Schematic root must be an NBT compound");
         readString(input);
         Map<String, Object> root = compound(input, 0);
         Object nested = root.get("Schematic");
         Map<String, Object> data = nested instanceof Map<?, ?> ? asCompound(nested, "Schematic") : root;
         int version = number(data, "Version");
         if (version != 2 && version != 3) throw new IOException("Unsupported Sponge schematic version: " + version);
         int width = positiveDimension(data, "Width");
         int height = positiveDimension(data, "Height");
         int length = positiveDimension(data, "Length");
         long volume = (long) width * height * length;
         if (volume > MAX_BLOCKS) throw new IOException("Schematic exceeds " + MAX_BLOCKS + " blocks");
         int[] offset = data.get("Offset") instanceof int[] value ? value : new int[]{0, 0, 0};
         if (offset.length != 3) throw new IOException("Schematic Offset must contain three integers");
         Map<String, Object> blocks = version == 3 ? asCompound(data.get("Blocks"), "Blocks") : data;
         Map<String, Object> rawPalette = asCompound(blocks.get("Palette"), "Palette");
         if (rawPalette.isEmpty() || rawPalette.size() > MAX_BLOCKS) throw new IOException("Invalid schematic palette size");
         Map<Integer, String> palette = new HashMap<>();
         for (Map.Entry<String, Object> entry : rawPalette.entrySet()) {
            if (!(entry.getValue() instanceof Number index) || index.intValue() < 0 || index.intValue() >= MAX_BLOCKS
               || palette.putIfAbsent(index.intValue(), entry.getKey()) != null) {
               throw new IOException("Invalid schematic palette index");
            }
         }
         String dataKey = version == 3 ? "Data" : "BlockData";
         if (!(blocks.get(dataKey) instanceof byte[] encoded)) throw new IOException("Missing schematic " + dataKey);
         int[] ids = decodeVarints(encoded, (int) volume, palette);
         return new SpongeSchematic(width, height, length, offset[0], offset[1], offset[2], Map.copyOf(palette), ids);
      }
   }

   private static int[] decodeVarints(byte[] encoded, int count, Map<Integer, String> palette) throws IOException {
      int[] result = new int[count];
      int position = 0;
      for (int index = 0; index < count; index++) {
         int value = 0;
         boolean complete = false;
         for (int part = 0; part < 5; part++) {
            if (position >= encoded.length) throw new IOException("Truncated schematic block data");
            int next = encoded[position++] & 0xff;
            if (part == 4 && (next & 0xf0) != 0) throw new IOException("Invalid schematic varint");
            value |= (next & 0x7f) << (part * 7);
            if ((next & 0x80) == 0) { complete = true; break; }
         }
         if (!complete || !palette.containsKey(value)) throw new IOException("Unknown schematic palette value: " + value);
         result[index] = value;
      }
      if (position != encoded.length) throw new IOException("Extra schematic block data");
      return result;
   }

   private static int positiveDimension(Map<String, Object> data, String key) throws IOException {
      Object raw = data.get(key);
      if (!(raw instanceof Short value)) throw new IOException("Missing schematic " + key);
      int dimension = value & 0xffff;
      if (dimension == 0 || dimension > MAX_BLOCKS) throw new IOException("Invalid schematic " + key);
      return dimension;
   }

   private static int number(Map<String, Object> data, String key) throws IOException {
      Object raw = data.get(key);
      if (!(raw instanceof Integer value)) throw new IOException("Missing schematic " + key);
      return value;
   }

   @SuppressWarnings("unchecked")
   private static Map<String, Object> asCompound(Object value, String name) throws IOException {
      if (!(value instanceof Map<?, ?>)) throw new IOException("Missing schematic " + name);
      return (Map<String, Object>) value;
   }

   private static Map<String, Object> compound(DataInputStream input, int depth) throws IOException {
      if (depth > 24) throw new IOException("NBT nesting limit exceeded");
      Map<String, Object> values = new HashMap<>();
      for (int count = 0; count < MAX_COLLECTION; count++) {
         int type = input.readUnsignedByte();
         if (type == 0) return values;
         values.put(readString(input), payload(input, type, depth + 1));
      }
      throw new IOException("NBT compound limit exceeded");
   }

   private static Object payload(DataInputStream input, int type, int depth) throws IOException {
      if (depth > 24) throw new IOException("NBT nesting limit exceeded");
      return switch (type) {
         case 1 -> input.readByte();
         case 2 -> input.readShort();
         case 3 -> input.readInt();
         case 4 -> input.readLong();
         case 5 -> input.readFloat();
         case 6 -> input.readDouble();
         case 7 -> {
            byte[] bytes = new byte[length(input, MAX_INFLATED)];
            input.readFully(bytes);
            yield bytes;
         }
         case 8 -> readString(input);
         case 9 -> {
            int elementType = input.readUnsignedByte();
            int count = length(input, MAX_COLLECTION);
            List<Object> list = new ArrayList<>(count);
            for (int index = 0; index < count; index++) list.add(payload(input, elementType, depth + 1));
            yield list;
         }
         case 10 -> compound(input, depth + 1);
         case 11 -> {
            int count = length(input, MAX_COLLECTION);
            int[] values = new int[count];
            for (int index = 0; index < count; index++) values[index] = input.readInt();
            yield values;
         }
         case 12 -> {
            int count = length(input, MAX_COLLECTION);
            long[] values = new long[count];
            for (int index = 0; index < count; index++) values[index] = input.readLong();
            yield values;
         }
         default -> throw new IOException("Unsupported NBT tag: " + type);
      };
   }

   private static int length(DataInputStream input, int max) throws IOException {
      int length = input.readInt();
      if (length < 0 || length > max) throw new IOException("NBT array/list limit exceeded");
      return length;
   }

   private static String readString(DataInputStream input) throws IOException {
      int length = input.readUnsignedShort();
      byte[] bytes = input.readNBytes(length);
      if (bytes.length != length) throw new IOException("Truncated NBT string");
      return new String(bytes, StandardCharsets.UTF_8);
   }
}
