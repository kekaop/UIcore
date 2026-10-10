package org.orvencraft.uicore;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.junit.jupiter.api.Test;

class MenuDamageAttributionTest {
   private static <T> T entity(Class<T> type) {
      java.util.UUID id = java.util.UUID.randomUUID();
      return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
         (proxy, method, args) -> method.getName().equals("getUniqueId") ? id : null));
   }

   private EntityDamageByEntityEvent event(Entity damager, Entity cause) {
      DamageSource source = (DamageSource)Proxy.newProxyInstance(DamageSource.class.getClassLoader(), new Class<?>[]{DamageSource.class},
         (proxy, method, args) -> method.getName().equals("getCausingEntity") ? cause : null);
      return new EntityDamageByEntityEvent(damager, entity(Player.class), DamageCause.ENTITY_ATTACK, source, 2);
   }

   @Test void attributesDirectAttacksAndPlayerCausedExplosions() {
      Player player = entity(Player.class);
      assertTrue(MenuListener.isPlayerDamage(event(player, null)));
      assertTrue(MenuListener.isPlayerDamage(event(entity(Entity.class), player)));
      assertFalse(MenuListener.isPlayerDamage(event(entity(Entity.class), null)));
   }

   @Test void includesPlayerProjectilesButNotMobProjectiles() {
      Player shooter = entity(Player.class);
      Projectile projectile = (Projectile)Proxy.newProxyInstance(Projectile.class.getClassLoader(), new Class<?>[]{Projectile.class},
         (proxy, method, args) -> method.getName().equals("getShooter") ? shooter : null);
      assertTrue(MenuListener.isPlayerDamage(event(projectile, null)));
      assertFalse(MenuListener.isPlayerDamage(event(entity(Projectile.class), null)));
   }

   @Test void doesNotBlockDamageThePlayerCausesToThemselves() {
      Player player = entity(Player.class);
      DamageSource source = (DamageSource)Proxy.newProxyInstance(DamageSource.class.getClassLoader(), new Class<?>[]{DamageSource.class},
         (proxy, method, args) -> method.getName().equals("getCausingEntity") ? player : null);
      assertFalse(MenuListener.isPlayerDamage(new EntityDamageByEntityEvent(player, player, DamageCause.ENTITY_ATTACK, source, 2)));
   }
}
