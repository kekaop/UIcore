package org.orvencraft.uicore.schematic;

import java.util.Map;

public record SpongeSchematic(
   int width, int height, int length,
   int offsetX, int offsetY, int offsetZ,
   Map<Integer, String> palette, int[] blockIds
) {
   public int blockCount() {
      return blockIds.length;
   }

   public Block at(int index) {
      int x = index % width;
      int z = (index / width) % length;
      int y = index / (width * length);
      return new Block(Math.addExact(x, offsetX), Math.addExact(y, offsetY), Math.addExact(z, offsetZ), palette.get(blockIds[index]));
   }

   public record Block(int x, int y, int z, String state) {}
}
