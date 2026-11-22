package org.orvencraft.uicore.camera;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orvencraft.uicore.cutscene.model.CameraPose;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MenuClientViewTest {
   private static PacketEventsAPI<?> previous;
   private static final CameraPose POSE = new CameraPose("world", 30, 80, 40, 181, 25);

   @BeforeAll static void protocolVersion() {
      previous = PacketEvents.getAPI();
      PacketEvents.setAPI(new PacketEventsAPI<Object>() {
         public boolean isLoaded() { return true; }
         public void init() { }
         public boolean isInitialized() { return true; }
         public boolean isTerminated() { return false; }
         public Object getPlugin() { return null; }
         public ServerManager getServerManager() { return () -> ServerVersion.V_26_1_2; }
         public ProtocolManager getProtocolManager() { return null; }
         public PlayerManager getPlayerManager() { return null; }
         public NettyManager getNettyManager() { return null; }
         public ChannelInjector getInjector() { return null; }
      });
   }
   @AfterAll static void restoreProtocol() { PacketEvents.setAPI(previous); }

   @Test void diggingModeUsesSurvivalWithoutAnEntityBlockingTheBlockRay() {
      List<List<PacketWrapper<?>>> sent = new ArrayList<>();
      var view = new MenuClientView(sent::add, true, 42, (byte) 0x08, null);
      view.open(POSE);
      assertFalse(sent.get(0).stream().filter(WrapperPlayServerSpawnEntity.class::isInstance)
         .map(WrapperPlayServerSpawnEntity.class::cast).anyMatch(p -> p.getEntityType() == EntityTypes.INTERACTION));
      assertEquals(0f, sent.get(0).stream().filter(WrapperPlayServerChangeGameState.class::isInstance)
         .map(WrapperPlayServerChangeGameState.class::cast).findFirst().orElseThrow().getValue());
      var hiddenPlayer = sent.get(0).stream().filter(WrapperPlayServerEntityMetadata.class::isInstance)
         .map(WrapperPlayServerEntityMetadata.class::cast).filter(packet -> packet.getEntityId() == 42).findFirst().orElseThrow();
      assertEquals((byte) 0x28, MenuClientView.readPlayerFlags(hiddenPlayer.getEntityMetadata()));
      view.close(42, 2);
      assertEquals(2f, sent.get(1).stream().filter(WrapperPlayServerChangeGameState.class::isInstance)
         .map(WrapperPlayServerChangeGameState.class::cast).findFirst().orElseThrow().getValue());
      var restoredPlayer = sent.get(1).stream().filter(WrapperPlayServerEntityMetadata.class::isInstance)
         .map(WrapperPlayServerEntityMetadata.class::cast).filter(packet -> packet.getEntityId() == 42).findFirst().orElseThrow();
      assertEquals((byte) 0x08, MenuClientView.readPlayerFlags(restoredPlayer.getEntityMetadata()));
   }

   @Test void diggingModeCanHideExperienceWithoutGrantingCreativeAbilities() {
      List<List<PacketWrapper<?>>> sent = new ArrayList<>();
      var abilities = new WrapperPlayServerPlayerAbilities(false, false, false, false, 0.05f, 0.1f);
      var view = new MenuClientView(sent::add, true, 42, (byte) 0, abilities);
      view.open(POSE);
      var packets = sent.get(0);
      var gameMode = packets.stream().filter(WrapperPlayServerChangeGameState.class::isInstance)
         .map(WrapperPlayServerChangeGameState.class::cast).findFirst().orElseThrow();
      assertEquals(1f, gameMode.getValue());
      assertSame(abilities, packets.get(packets.indexOf(gameMode) + 1));
      assertFalse(abilities.isInCreativeMode());
      assertFalse(abilities.isFlightAllowed());
   }

   @Test void spectatorClicksRemainAvailableUntilTheViewCloses() {
      List<List<PacketWrapper<?>>> sent = new ArrayList<>();
      var view = new MenuClientView(sent::add);
      view.open(POSE);
      var receiver = sent.get(0).stream().filter(WrapperPlayServerSpawnEntity.class::isInstance)
         .map(WrapperPlayServerSpawnEntity.class::cast).filter(packet -> packet.getEntityType() == EntityTypes.INTERACTION).findFirst().orElseThrow();
      assertTrue(view.acceptsEntityClick());
      assertTrue(view.acceptsClick(receiver.getUUID().orElseThrow()));
      assertFalse(view.acceptsClick(UUID.randomUUID()));
      view.close(42, 0);
      assertFalse(view.acceptsEntityClick());
      assertTrue(Math.abs(receiver.getPosition().getY() - POSE.y()) <= 1,
         "A receiver centered eight blocks below the camera is outside the client's nearby-section search");
      int count = sent.size();
      view.render(POSE, Map.of());
      view.close(42, 0);
      assertEquals(count, sent.size(), "Stale frames/duplicate cleanup must not change a closed client view");
   }

   @Test void cameraAndVisualsShareOneOutputFrameAndOneTickInterpolation() {
      List<List<PacketWrapper<?>>> sent = new ArrayList<>();
      var view = new MenuClientView(sent::add);
      view.open(POSE);
      int cameraId = sent.get(0).stream().filter(WrapperPlayServerCamera.class::isInstance)
         .map(WrapperPlayServerCamera.class::cast).findFirst().orElseThrow().getCameraId();
      view.beginFrames(List.of(101));
      var element = new CameraPose("world", 31, 82, 44, 181, 25);
      view.render(POSE, Map.of(101, element, 999, element));
      assertEquals(2, sent.size(), "An update must be delivered as one frame");
      var updates = sent.get(1);
      var moves = updates.stream().filter(WrapperPlayServerEntityTeleport.class::isInstance)
         .map(WrapperPlayServerEntityTeleport.class::cast).toList();
      assertTrue(moves.stream().anyMatch(packet -> packet.getEntityId() == cameraId && packet.getPosition().getY() == 80));
      assertTrue(moves.stream().anyMatch(packet -> packet.getEntityId() == 101 && packet.getPosition().getY() == 82));
      assertFalse(moves.stream().anyMatch(packet -> packet.getEntityId() == 999));
      assertThrows(UnsupportedOperationException.class, () -> updates.clear());
      var metadata = updates.stream().filter(WrapperPlayServerEntityMetadata.class::isInstance)
         .map(WrapperPlayServerEntityMetadata.class::cast).findFirst().orElseThrow();
      assertEquals(1, metadata.getEntityMetadata().get(0).getValue());
   }

   @Test void closeDestroysRetainedVisualsAndRestoresCameraAndActualMode() {
      List<List<PacketWrapper<?>>> sent = new ArrayList<>();
      var view = new MenuClientView(sent::add);
      view.open(POSE);
      view.beginFrames(List.of(101, 102));
      view.forget(101);
      view.close(42, 2);
      var close = sent.get(sent.size()-1);
      assertEquals(42, ((WrapperPlayServerCamera)close.get(0)).getCameraId());
      assertEquals(2, ((WrapperPlayServerChangeGameState)close.get(1)).getValue());
      int[] destroyed = ((WrapperPlayServerDestroyEntities)close.get(2)).getEntityIds();
      assertEquals(3, destroyed.length, "Two virtual entities and the remaining retained visual");
      assertTrue(java.util.Arrays.stream(destroyed).anyMatch(id -> id == 102));
      assertFalse(java.util.Arrays.stream(destroyed).anyMatch(id -> id == 101));
   }
}
