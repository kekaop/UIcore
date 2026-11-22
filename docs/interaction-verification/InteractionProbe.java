package smoke;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.api.UIcoreProvider;
import org.orvencraft.uicore.api.event.MenuClickEvent;
import org.orvencraft.uicore.api.event.MenuDragEvent;
import org.orvencraft.uicore.api.event.MenuDropEvent;
import org.orvencraft.uicore.position.DynamicPositionSpec.Position;
import org.orvencraft.uicore.layout.MenuLayout;
import java.lang.reflect.Method;
import java.util.Locale;

public final class InteractionProbe extends JavaPlugin implements Listener {
  boolean cancelDrop;
  String mutation = "none";
  public void onEnable() {
    Bukkit.getPluginManager().registerEvents(this,this);
    UIcoreProvider.get().actions().register(this,"probe:drop",context->{
      var plugin=JavaPlugin.getPlugin(UIcorePlugin.class);
      var state=plugin.getPlayerMenuStateSnapshot(context.player(),context.menuId());
      getLogger().info("PROBE action owner="+context.elementId()+" handle="+context.argument("drag-handle")
        +" moved="+context.argument("drag-target")+" receiver="+context.argument("drop-target")+" state="+state.get("dropped"));
      plugin.setPlayerMenuState(context.player(),context.menuId(),"drop_count",
        Integer.toString(Integer.parseInt(state.getOrDefault("drop_count","0"))+1));
    });
    getCommand("interactionprobe").setExecutor((sender,cmd,label,args)->{
      Player p=Bukkit.getPlayerExact(args[1]);
      if(p==null) return true;
      p.getScheduler().run(this,task->{try {
        UIcorePlugin plugin=JavaPlugin.getPlugin(UIcorePlugin.class);
        String menu=plugin.getCurrentPlayerMenu(p);
        var section=plugin.getRuntimeSection(p,menu);
        switch(args[0]) {
          case "pointer" -> {
            Location base=new Location(p.getWorld(),section.cameraX,section.cameraY,section.cameraZ,section.yaw,section.pitch);
            Vector f=base.getDirection(),r=new Vector(-f.getZ(),0,f.getX()).normalize(),u=f.clone().crossProduct(r).multiply(-1).normalize();
            UIcorePlugin.cursorExactLocations.put(p,base.add(f.multiply(3.5)).add(r.multiply(Double.parseDouble(args[2]))).add(u.multiply(Double.parseDouble(args[3]))));
            sender.sendMessage("PROBE pointer ready");
          }
          case "carrier" -> sender.sendMessage("PROBE carrier-visible="+p.canSee(UIcorePlugin.playerSit.get(p)));
          case "hotbar" -> sender.sendMessage("PROBE held slot="+p.getInventory().getHeldItemSlot());
          case "render" -> {
            var layout=plugin.getLayoutForPlayer(p,menu,args[2]);
            var display=plugin.getTextDisplayByKeys(p,menu,args[2]);
            Method position=UIcorePlugin.class.getDeclaredMethod("getCurrentLayoutPosition",Player.class,String.class,MenuLayout.class);
            position.setAccessible(true);
            Position local=(Position)position.invoke(plugin,p,menu,layout);
            Method world=UIcorePlugin.class.getDeclaredMethod("toLayoutWorldLocation",org.orvencraft.uicore.section.Section.class,MenuLayout.class,Position.class);
            world.setAccessible(true);
            Location expected=(Location)world.invoke(plugin,section,layout,local);
            Method itemLookup=UIcorePlugin.class.getDeclaredMethod("findTrackedItemDisplay",Player.class,String.class,String.class);
            itemLookup.setAccessible(true);
            var item=(org.bukkit.entity.ItemDisplay)itemLookup.invoke(plugin,p,menu,args[2]);
            sender.sendMessage(String.format(Locale.ROOT,"PROBE render %s x=%.3f y=%.3f scale=%.3f item=%.3f error=%.6f",
              args[2],local.x(),local.y(),display.getTransformation().getScale().x,
              item==null?0:item.getTransformation().getScale().x,display.getLocation().distance(expected)));
          }
          case "report" -> {
            Method m=UIcorePlugin.class.getDeclaredMethod("getCurrentLayoutPosition",Player.class,String.class,MenuLayout.class);m.setAccessible(true);
            Position root=(Position)m.invoke(plugin,p,menu,plugin.getLayoutForPlayer(p,menu,"root"));
            Position child=(Position)m.invoke(plugin,p,menu,plugin.getLayoutForPlayer(p,menu,"child"));
            var display=plugin.getTextDisplayByKeys(p,menu,"child");
            var actual=plugin.getMenuDisplayLocation(p,display);
            Method w=UIcorePlugin.class.getDeclaredMethod("toLayoutWorldLocation",org.orvencraft.uicore.section.Section.class,MenuLayout.class,Position.class);w.setAccessible(true);
            Location expected=(Location)w.invoke(plugin,section,plugin.getLayoutForPlayer(p,menu,"child"),child);
            sender.sendMessage(String.format(Locale.ROOT,"PROBE report root=%.4f,%.4f child=%.4f,%.4f error=%.6f",root.x(),root.y(),child.x(),child.y(),actual.distance(expected)));
          }
          case "inspect" -> {
            var layout=plugin.getLayoutForPlayer(p,menu,args[2]);
            Method m=UIcorePlugin.class.getDeclaredMethod("getCurrentLayoutPosition",Player.class,String.class,MenuLayout.class);m.setAccessible(true);
            Position pos=layout==null?null:(Position)m.invoke(plugin,p,menu,layout);
            var field=UIcorePlugin.class.getDeclaredField("dragGestures");field.setAccessible(true);
            boolean active=((java.util.Map<?,?>)field.get(plugin)).containsKey(p.getUniqueId());
            sender.sendMessage("PROBE inspect "+args[2]+" position="+(pos==null?"hidden":String.format(Locale.ROOT,"%.4f,%.4f",pos.x(),pos.y()))
              +" active="+active+" state="+plugin.getPlayerMenuStateSnapshot(p,menu));
          }
          case "edit" -> {
            String raw=args[4]; Object value=raw.equals("null")?null:raw.equals("true")?true:raw.equals("false")?false:raw;
            org.orvencraft.uicore.api.UIcoreProvider.get().elements().edit(p,menu,args[2],e->e.set(args[3],value));
            sender.sendMessage("PROBE edited");
          }
          case "remove" -> {
            var elements=org.orvencraft.uicore.api.UIcoreProvider.get().elements();
            elements.remove(p,menu,args[2]); elements.remove(menu,args[2]);sender.sendMessage("PROBE removed");
          }
          case "add" -> {
            org.orvencraft.uicore.api.UIcoreProvider.get().elements().add(menu,
               org.orvencraft.uicore.api.MenuElementSpec.builder("extra").type("group").build());sender.sendMessage("PROBE added");
          }
          case "mutate" -> {mutation=args[2];sender.sendMessage("PROBE mutation="+mutation);}
          case "stall-frame" -> {
            var field=UIcorePlugin.class.getDeclaredField("cutsceneViews");field.setAccessible(true);
            var view=((java.util.Map<?,?>)field.get(plugin)).get(p.getUniqueId());
            if(view==null)throw new IllegalStateException("No camera frame view");
            var moving=view.getClass().getDeclaredField("moving");moving.setAccessible(true);moving.set(view,true);
            var inFlight=view.getClass().getDeclaredField("inFlight");inFlight.setAccessible(true);
            inFlight.set(view,new java.util.concurrent.CompletableFuture<Boolean>());
            sender.sendMessage("PROBE frame stalled");
          }
          case "cancel" -> {cancelDrop=Boolean.parseBoolean(args[2]);sender.sendMessage("PROBE cancel="+cancelDrop);}
          case "close" -> {plugin.stopCursor(p,true);sender.sendMessage("PROBE closed");}
        }
      }catch(Exception e){getLogger().log(java.util.logging.Level.SEVERE,"PROBE error",e);}},null);
      return true;
    });
  }
  @EventHandler public void click(MenuClickEvent e) {
    if(!mutation.equals("CLICK"))return;
    mutation="none";
    UIcoreProvider.get().elements().edit(e.getPlayer(),e.getMenuId(),e.getElementId(),editor->editor.x(.4));
    getLogger().info("PROBE replaced clicked element");
  }
  @EventHandler public void drag(MenuDragEvent e){
    if(mutation.equals(e.getPhase().name())) {
      mutation="none";
      org.orvencraft.uicore.api.UIcoreProvider.get().elements().edit(e.getPlayer(),e.getMenuId(),e.getTargetId(),editor->editor.x(.4));
    }
    if(cancelDrop && e.getPhase()==MenuDragEvent.Phase.DROP)e.setCancelled(true);
    getLogger().info("PROBE drag "+e.getPhase()+" target="+e.getTargetId()+" x="+e.getX()+" y="+e.getY());
  }

  @EventHandler public void dropped(MenuDropEvent e) throws Exception {
    var plugin=JavaPlugin.getPlugin(UIcorePlugin.class);
    Method m=UIcorePlugin.class.getDeclaredMethod("getCurrentLayoutPosition",Player.class,String.class,MenuLayout.class);m.setAccessible(true);
    var layout=plugin.getLayoutForPlayer(e.getPlayer(),e.getMenuId(),e.getTargetId());
    Position pos=(Position)m.invoke(plugin,e.getPlayer(),e.getMenuId(),layout);
    getLogger().info(String.format(Locale.ROOT,"PROBE committed %s receiver=%s x=%.4f actual=%.4f state=%s",
      e.getTargetId(),e.getDropTargetId(),e.getX(),pos.x(),plugin.getPlayerMenuStateSnapshot(e.getPlayer(),e.getMenuId()).get("dropped")));
  }
}
