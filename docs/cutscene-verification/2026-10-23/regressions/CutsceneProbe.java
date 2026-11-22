package smoke;

import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.UIcoreProvider;
import org.orvencraft.uicore.api.event.*;

public final class CutsceneProbe extends JavaPlugin implements Listener {
   public void onEnable() {
      Bukkit.getPluginManager().registerEvents(this, this);
      com.github.retrooper.packetevents.PacketEvents.getAPI().getEventManager().registerListener(new com.github.retrooper.packetevents.event.PacketListenerAbstract(com.github.retrooper.packetevents.event.PacketListenerPriority.MONITOR) {
         @Override public void onPacketReceive(com.github.retrooper.packetevents.event.PacketReceiveEvent event) {
            if (event.getPacketType() == com.github.retrooper.packetevents.protocol.packettype.PacketType.Play.Client.SPECTATE_ENTITY)
               getLogger().info("Probe packet cancelled=" + event.isCancelled() + " id=" + new com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSpectateEntity(event).getEntityId());
         }
      });
      getCommand("cutsceneprobe").setExecutor((sender, command, label, args) -> {
         if (args.length < 2) return false;
         Player player = Bukkit.getPlayerExact(args[1]);
         if (player == null) { sender.sendMessage("Probe player absent"); return true; }
         player.getScheduler().execute(this, () -> {
            var api = UIcoreProvider.get();
            switch (args[0]) {
               case "choose" -> sender.sendMessage("Probe choose=" + api.cutscenes().choose(player, args[2]));
               case "element" -> sender.sendMessage("Probe element=" + JavaPlugin.getPlugin(UIcorePlugin.class).activateMenuElement(player, args[2]));
               case "close" -> {
                  api.closeMenu(player);
                  player.getScheduler().runDelayed(this, task -> sender.sendMessage("Probe closed=" + !api.isMenuOpen(player)
                     + " cameraAbsent=" + !UIcorePlugin.playerSit.containsKey(player)
                     + " cursorAbsent=" + !UIcorePlugin.playerCursors.containsKey(player)), null, 15);
               }
               case "report" -> {
                  var target = UIcorePlugin.playerSit.get(player);
                  int ping = player.getPing();
                  var state = api.cutscenes().state(player).orElse(null);
                  if (target == null) { sender.sendMessage("Probe no camera"); break; }
                  target.getScheduler().run(this, task -> {
                     var eye = target.getEyeLocation();
                     var cursor = UIcorePlugin.cursorExactLocations.get(player);
                     var frame = new org.orvencraft.uicore.camera.MenuCameraFrame(new org.orvencraft.uicore.cutscene.model.CameraPose(
                        eye.getWorld().getName(), eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch()));
                     var local = cursor == null ? new org.bukkit.util.Vector() : frame.worldToLocal(cursor);
                     sender.sendMessage(String.format(Locale.ROOT,
                        "Probe report ping=%d ms eye=%.8f,%.8f,%.8f yaw=%.8f pitch=%.8f cursor=%.8f,%.8f,%.8f state=%s", ping,
                        eye.getX(), eye.getY(), eye.getZ(), eye.getYaw(), eye.getPitch(), local.getX(), local.getY(), local.getZ(), state));
                  }, null);
               }
            }
         }, null, 0);
         return true;
      });
   }
   private void log(CutsceneEvent event) { getLogger().info(event.getClass().getSimpleName() + " " + event.getSnapshot()); }
   @EventHandler public void start(CutsceneStartEvent event) { log(event); }
   @EventHandler public void step(CutsceneStepEnterEvent event) { log(event); }
   @EventHandler public void pause(CutscenePauseEvent event) { log(event); }
   @EventHandler public void resume(CutsceneResumeEvent event) { log(event); }
   @EventHandler public void choice(CutsceneChoiceEvent event) { log(event); getLogger().info("Accepted option " + event.getOptionId()); }
   @EventHandler public void complete(CutsceneCompleteEvent event) { log(event); }
   @EventHandler public void abort(CutsceneAbortEvent event) { log(event); getLogger().info("Abort " + event.getReason() + ": " + event.getDiagnostic()); }
   @EventHandler public void click(MenuClickEvent event) { getLogger().info("Probe click " + event.getMenuId() + ":" + event.getElementId()); }
   @EventHandler public void hover(MenuHoverEvent event) {
      var plugin = JavaPlugin.getPlugin(UIcorePlugin.class);
      String state = plugin.getCutsceneManager().state(event.getPlayer()).map(s -> s.state().name()).orElse("NONE");
      getLogger().info("Probe hover " + event.getMenuId() + ":" + event.getElementId() + " enter=" + event.isEntering()
         + " moving=" + plugin.isCutsceneCameraMoving(event.getPlayer()) + " state=" + state);
   }
}
