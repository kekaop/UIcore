package org.orvencraft.uicore.camera;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Server;
import com.github.retrooper.packetevents.protocol.teleport.RelativeFlag;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.orvencraft.uicore.cutscene.model.CameraPose;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/** Viewer-local camera, independent of the server's entity tracking and region migrations. */
public final class MenuClientView {
   private static final AtomicInteger IDS = new AtomicInteger(-2_000_000_000);
   private static final int SHARED_FLAGS_INDEX = 0, INVISIBLE_FLAG = 0x20;
   private final int cameraId = IDS.getAndIncrement(), receiverId = IDS.getAndIncrement();
   private final UUID cameraUuid = UUID.randomUUID(), receiverUuid = UUID.randomUUID();
   private final Consumer<List<PacketWrapper<?>>> output;
   private final Set<Integer> visuals = ConcurrentHashMap.newKeySet();
   private final Set<Integer> knownVisuals = ConcurrentHashMap.newKeySet();
   private volatile boolean synchronizedFrames;
   private volatile boolean closed;
   private final boolean digging;
   private final WrapperPlayServerPlayerAbilities diggingAbilities;
   private final int playerId;
   private byte playerFlags;

   public MenuClientView(Consumer<List<PacketWrapper<?>>> output) { this(output, false, 0, (byte) 0, null); }
   public MenuClientView(Consumer<List<PacketWrapper<?>>> output, boolean digging, int playerId, byte playerFlags,
                         WrapperPlayServerPlayerAbilities diggingAbilities) {
      this.output = output;
      this.digging = digging;
      this.diggingAbilities = diggingAbilities;
      this.playerId = playerId;
      this.playerFlags = playerFlags;
   }

   public boolean acceptsClick(UUID target) { return !closed && receiverUuid.equals(target); }
   // 26.1 uses the entity under the vanilla ray, which may also be a camera
   // carrier or another nearby entity. UI picking uses our committed cursor frame.
   public boolean acceptsEntityClick() { return !closed; }

   public synchronized void open(CameraPose pose) {
      var packets = new ArrayList<PacketWrapper<?>>();
      packets.add(spawn(cameraId, cameraUuid, EntityTypes.ITEM_DISPLAY, pose));
      packets.add(interpolation(cameraId));
      // The vanilla spectator client sends a spectate request on left click only when
      // its ray hits an entity. Enclose the viewpoint in a non-rendered Interaction.
      if (!digging) packets.add(spawn(receiverId, receiverUuid, EntityTypes.INTERACTION, receiverPose(pose)));
      if (!digging) packets.add(new WrapperPlayServerEntityMetadata(receiverId, List.of(
         new EntityData<>(8, EntityDataTypes.FLOAT, 16.0F),
         new EntityData<>(9, EntityDataTypes.FLOAT, 2.0F),
         new EntityData<>(10, EntityDataTypes.BOOLEAN, true))));
      // Vanilla renders the empty first-person arm unless the local player is invisible.
      if (digging) packets.add(new WrapperPlayServerEntityMetadata(playerId, List.of(hiddenPlayerFlags())));
      // On 26.x Creative hides experience while the abilities packet keeps timed mining.
      int clientMode = diggingAbilities != null ? 1 : digging ? 0 : 3;
      packets.add(new WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, clientMode));
      if (diggingAbilities != null) packets.add(diggingAbilities);
      packets.add(new WrapperPlayServerCamera(cameraId));
      send(packets);
   }

   public void beginFrames(Iterable<Integer> existingVisuals) {
      existingVisuals.forEach(id -> { visuals.add(id); knownVisuals.add(id); });
      synchronizedFrames = true;
   }

   public void track(int id) { if (synchronizedFrames) visuals.add(id); }

   public void forget(int id) {
      visuals.remove(id);
      if (knownVisuals.remove(id)) send(List.of(new WrapperPlayServerDestroyEntities(id)));
   }

   /** Resume ordinary tracker updates when switching to a menu without a cutscene. */
   public void endFrames() { synchronizedFrames = false; visuals.clear(); knownVisuals.clear(); }

   public void render(CameraPose camera, Map<Integer, CameraPose> positions) {
      if (closed) return;
      var packets = new ArrayList<PacketWrapper<?>>();
      positions.forEach((id, pose) -> {
         if (visuals.contains(id)) {
            packets.add(interpolation(id));
            packets.add(teleport(id, pose));
         }
      });
      packets.add(teleport(cameraId, camera));
      if (!digging) packets.add(teleport(receiverId, receiverPose(camera)));
      send(packets);
   }

   public synchronized void close(int playerId, int gameMode) {
      if (closed) return;
      closed = true;
      var packets = new ArrayList<PacketWrapper<?>>();
      packets.add(new WrapperPlayServerCamera(playerId));
      packets.add(new WrapperPlayServerChangeGameState(WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE, gameMode));
      if (digging) packets.add(new WrapperPlayServerEntityMetadata(playerId, List.of(sharedFlags(playerFlags))));
      int[] removed = java.util.stream.IntStream.concat(visuals.stream().mapToInt(Integer::intValue),
         java.util.stream.IntStream.of(cameraId, receiverId)).toArray();
      packets.add(new WrapperPlayServerDestroyEntities(removed));
      output.accept(List.copyOf(packets));
      visuals.clear(); knownVisuals.clear();
   }

   /** Runs on the packet thread; uses immutable values/IDs, never Bukkit entity access. */
   public void filter(PacketSendEvent event) {
      if (closed) return;
      var type = event.getPacketType();
      if (type == Server.CAMERA) {
         new WrapperPlayServerCamera(event).setCameraId(cameraId);
         event.markForReEncode(true);
      } else if (type == Server.CHANGE_GAME_STATE) {
         var packet = new WrapperPlayServerChangeGameState(event);
         if (packet.getReason() == WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE) {
            // A game mode packet also resets abilities, even if the mode is unchanged.
            if (diggingAbilities != null) {
               event.setCancelled(true);
            } else {
               packet.setValue(digging ? 0 : 3); event.markForReEncode(true);
            }
         }
      } else if (diggingAbilities != null && type == Server.PLAYER_ABILITIES) {
         var packet = new WrapperPlayServerPlayerAbilities(event);
         packet.setInCreativeMode(false);
         event.markForReEncode(true);
      } else if (synchronizedFrames && type == Server.PLAYER_POSITION_AND_LOOK) {
         var packet = new WrapperPlayServerPlayerPositionAndLook(event);
         // Seat teleports may correct position, but must not reset the mouse baseline.
         packet.setYaw(0); packet.setPitch(0);
         packet.setRelative(RelativeFlag.YAW, true); packet.setRelative(RelativeFlag.PITCH, true);
         event.markForReEncode(true);
      } else if (synchronizedFrames && type == Server.DESTROY_ENTITIES) {
         var packet = new WrapperPlayServerDestroyEntities(event);
         int[] remaining = Arrays.stream(packet.getEntityIds()).filter(id -> !visuals.contains(id)).toArray();
         if (remaining.length == 0) event.setCancelled(true);
         else if (remaining.length != packet.getEntityIds().length) {
            packet.setEntityIds(remaining); event.markForReEncode(true);
         }
      } else if (synchronizedFrames && type == Server.SPAWN_ENTITY) {
         int id = new WrapperPlayServerSpawnEntity(event).getEntityId();
         if (visuals.contains(id) && !knownVisuals.add(id)) event.setCancelled(true);
      } else if (type == Server.ENTITY_METADATA && (digging || synchronizedFrames)) {
         var packet = new WrapperPlayServerEntityMetadata(event);
         if (digging && packet.getEntityId() == playerId) {
            filterPlayerFlags(event, packet);
         } else if (synchronizedFrames && visuals.contains(packet.getEntityId())) {
            var data = new ArrayList<>(packet.getEntityMetadata());
            data.removeIf(entry -> entry.getIndex() == 10);
            data.add(new EntityData<>(10, EntityDataTypes.INT, 1));
            packet.setEntityMetadata(data); event.markForReEncode(true);
         }
      } else if (synchronizedFrames) {
         int id = movementEntityId(event);
         if (visuals.contains(id)) event.setCancelled(true);
      }
   }

   private synchronized void filterPlayerFlags(PacketSendEvent event, WrapperPlayServerEntityMetadata packet) {
      if (closed) return;
      var data = new ArrayList<>(packet.getEntityMetadata());
      for (int i = 0; i < data.size(); i++) {
         var entry = data.get(i);
         if (entry.getIndex() == SHARED_FLAGS_INDEX && entry.getValue() instanceof Byte flags) {
            playerFlags = flags;
            data.set(i, hiddenPlayerFlags());
            packet.setEntityMetadata(data);
            event.markForReEncode(true);
            return;
         }
      }
   }

   public static byte readPlayerFlags(List<EntityData<?>> metadata) {
      for (var entry : metadata) {
         if (entry.getIndex() == SHARED_FLAGS_INDEX && entry.getValue() instanceof Byte flags) return flags;
      }
      return 0;
   }

   private EntityData<Byte> hiddenPlayerFlags() {
      return sharedFlags((byte) (playerFlags | INVISIBLE_FLAG));
   }

   private static EntityData<Byte> sharedFlags(byte flags) {
      return new EntityData<>(SHARED_FLAGS_INDEX, EntityDataTypes.BYTE, flags);
   }

   private int movementEntityId(PacketSendEvent event) {
      var type = event.getPacketType();
      if (type == Server.ENTITY_TELEPORT) return new WrapperPlayServerEntityTeleport(event).getEntityId();
      if (type == Server.ENTITY_POSITION_SYNC) return new WrapperPlayServerEntityPositionSync(event).getId();
      if (type == Server.ENTITY_RELATIVE_MOVE) return new WrapperPlayServerEntityRelativeMove(event).getEntityId();
      if (type == Server.ENTITY_RELATIVE_MOVE_AND_ROTATION) return new WrapperPlayServerEntityRelativeMoveAndRotation(event).getEntityId();
      if (type == Server.ENTITY_ROTATION) return new WrapperPlayServerEntityRotation(event).getEntityId();
      if (type == Server.ENTITY_HEAD_LOOK) return new WrapperPlayServerEntityHeadLook(event).getEntityId();
      if (type == Server.ENTITY_VELOCITY) return new WrapperPlayServerEntityVelocity(event).getEntityId();
      return Integer.MAX_VALUE;
   }

   private void send(List<PacketWrapper<?>> packets) { if (!closed) output.accept(List.copyOf(packets)); }
   private static CameraPose receiverPose(CameraPose pose) {
      // Keep the entity's origin near the viewpoint too: the client searches
      // nearby entity sections before checking bounding-box intersections.
      return new CameraPose(pose.world(), pose.x(), pose.y() - 1, pose.z(), 0, 0);
   }
   private static WrapperPlayServerSpawnEntity spawn(int id, UUID uuid,
      com.github.retrooper.packetevents.protocol.entity.type.EntityType type, CameraPose pose) {
      return new WrapperPlayServerSpawnEntity(id, Optional.of(uuid), type,
         new Vector3d(pose.x(), pose.y(), pose.z()), (float)pose.pitch(), (float)pose.yaw(), (float)pose.yaw(), 0, Optional.empty());
   }
   private static WrapperPlayServerEntityMetadata interpolation(int id) {
      return new WrapperPlayServerEntityMetadata(id, List.of(new EntityData<>(10, EntityDataTypes.INT, 1)));
   }
   private static WrapperPlayServerEntityTeleport teleport(int id, CameraPose pose) {
      return new WrapperPlayServerEntityTeleport(id, new Vector3d(pose.x(), pose.y(), pose.z()), (float)pose.yaw(), (float)pose.pitch(), false);
   }
}
