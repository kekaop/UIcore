package org.orvencraft.uicore.schematic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpongeSchematicReaderTest {
   @TempDir Path folder;

   @Test
   void readsV2PaletteAndOffsetsInXZYOrder() throws Exception {
      SpongeSchematic schematic = SpongeSchematicReader.read(write(2, new byte[]{0, 1, 1, 0}));
      assertEquals(4, schematic.blockCount());
      assertEquals(new SpongeSchematic.Block(3, -1, 4, "minecraft:stone"), schematic.at(0));
      assertEquals(new SpongeSchematic.Block(4, -1, 4, "minecraft:air"), schematic.at(1));
      assertEquals(new SpongeSchematic.Block(3, -1, 5, "minecraft:air"), schematic.at(2));
   }

   @Test
   void readsNestedV3BlockContainer() throws Exception {
      SpongeSchematic schematic = SpongeSchematicReader.read(write(3, new byte[]{0, 1, 1, 0}));
      assertEquals(2, schematic.width());
      assertEquals("minecraft:stone", schematic.at(3).state());
   }

   @Test
   void rejectsTruncatedPaletteIndices() throws Exception {
      Path file = write(3, new byte[]{0, 1, 1});
      assertThrows(IOException.class, () -> SpongeSchematicReader.read(file));
   }

   private Path write(int version, byte[] blockData) throws IOException {
      Path file = folder.resolve("fixture-" + version + ".schem");
      try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(java.nio.file.Files.newOutputStream(file)))) {
         out.writeByte(10); string(out, "");
         if (version == 3) { out.writeByte(10); string(out, "Schematic"); }
         intTag(out, "Version", version);
         shortTag(out, "Width", 2);
         shortTag(out, "Height", 1);
         shortTag(out, "Length", 2);
         out.writeByte(11); string(out, "Offset"); out.writeInt(3); out.writeInt(3); out.writeInt(-1); out.writeInt(4);
         if (version == 3) { out.writeByte(10); string(out, "Blocks"); }
         out.writeByte(10); string(out, "Palette");
         intTag(out, "minecraft:stone", 0);
         intTag(out, "minecraft:air", 1);
         out.writeByte(0);
         out.writeByte(7); string(out, version == 3 ? "Data" : "BlockData"); out.writeInt(blockData.length); out.write(blockData);
         if (version == 3) out.writeByte(0);
         if (version == 3) out.writeByte(0);
         out.writeByte(0);
      }
      return file;
   }

   private static void intTag(DataOutputStream out, String name, int value) throws IOException {
      out.writeByte(3); string(out, name); out.writeInt(value);
   }

   private static void shortTag(DataOutputStream out, String name, int value) throws IOException {
      out.writeByte(2); string(out, name); out.writeShort(value);
   }

   private static void string(DataOutputStream out, String value) throws IOException {
      byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
      out.writeShort(bytes.length); out.write(bytes);
   }
}
