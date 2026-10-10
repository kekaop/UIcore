package org.orvencraft.uicore.layout;

import org.orvencraft.uicore.ColorParser;
import org.orvencraft.uicore.UIcorePlugin;
import org.orvencraft.uicore.HoverEnlargeManager;
import org.orvencraft.uicore.api.ConfigurationCopy;
import org.orvencraft.uicore.api.model.UiElementModel;
import org.orvencraft.uicore.position.DynamicPositionSpec;
import org.orvencraft.uicore.style.TextStyleConfig;
import org.orvencraft.uicore.util.CommandUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MenuLayout {
   public final String key;
   public String name;
   private final List<String> command;
   private final boolean stop;
   public final double x;
   public final double y;
   public final double z;
   private final Set<UUID> pendingClickPlayers = ConcurrentHashMap.newKeySet();
   private final boolean teleportBool;
   private final boolean teleportOriginal;
   private final Location teleportLoc;
   private final boolean stopCommandBool;
   private final List<String> stopCommands;
   public final float tiltX;
   public final float tiltY;
   public final float tiltZ;
   private final String permission;
   private final boolean nextMenuEnabled;
   private final String nextMenuKey;
   private long commandDelay = 20L;
   private boolean useCommandDelay = false;
   private final String linkCommand;
   private String conditionVariable;
   private String conditionOperator;
   private String conditionValue;
   private List<String> randomCommands;
   private List<Integer> randomChances;
   private boolean useRandomCommands;
   private boolean hoverEnlargeEnabled;
   private double hoverEnlargeScale;
   private double textSize = 1.0;
   private boolean typewriterEnabled = false;
   private int typewriterSpeed = 2;
   private int typewriterStartDelay = 0;
   private String typewriterCursorChar = "|";
   private String hoverSound = null;
   private float hoverSoundVolume = 1.0F;
   private float hoverSoundPitch = 1.0F;
   private long hoverSoundCooldown = 20L;
   private List<String> hoverCommands;
   private List<String> leaveCommands;
   private boolean floatingAnimationEnabled = false;
   private String floatingAnimationType = "none";
   private double floatingAnimationSpeed = 1.0;
   private double floatingAnimationAmplitude = 0.25;
   private boolean entranceAnimationEnabled = false;
   private String entranceAnimationFrom = "top";
   private long entranceAnimationDelay = 0L;
   private int entranceAnimationDuration = 10;
   private double entranceAnimationDistance = 1.0;
   private String entranceAnimationEasing = "linear";
   private TextStyleConfig baseStyle = TextStyleConfig.empty();
   private TextStyleConfig hoverStyle = TextStyleConfig.empty();
   private TextStyleConfig pressedStyle = TextStyleConfig.empty();
   private TextStyleConfig permissionAllowedStyle = TextStyleConfig.empty();
   private TextStyleConfig permissionDeniedStyle = TextStyleConfig.empty();
   private TextStyleConfig toggleOffStyle = TextStyleConfig.empty();
   private TextStyleConfig toggleOnStyle = TextStyleConfig.empty();
   private int pressedStyleDuration = 3;
   private boolean hoverCursorEnabled = false;
   private String hoverCursorMaterial = "";
   private int hoverCursorCustomModelData = 0;
   private double hoverCursorScale = -1.0;
   private String hoverTooltipType = "actionbar";
   private String hoverTooltipText = "";
   private String permissionDeniedMessage = "";
   private String permissionDeniedActionBar = "";
   private String permissionDeniedSound = "";
   private float permissionDeniedSoundVolume = 1.0F;
   private float permissionDeniedSoundPitch = 1.0F;
   private String permissionDeniedClickBehavior = "deny";
   private List<String> permissionDeniedCommands = Collections.emptyList();
   private String type = "text";
   private String itemMaterial = "DIAMOND";
   private int itemCustomModelData = 0;
   private double itemScale = 0.5;
   private boolean itemGlow = false;
   private boolean itemRotate = false;
   private double itemRotateSpeed = 1.0;
   private String itemBillboard = "center";
   private String itemDisplayTransform = "none";
   private String avatarPlayer = "self";
   private boolean avatarFaceOnly;
   private String hitboxText = "      ";
   private double hitboxWidth = 0.7;
   private double hitboxHeight = 0.7;
   private double hitboxDepth = 0.25;
   private boolean customHitboxConfigured;
   private String dragGroup = "";
   private String dragMode = "";
   private String dragTarget = "";
   private double dragSensitivity = 1.0;
   private List<String> clickRequiredPermissions = Collections.emptyList();
   private List<PlaceholderRequirement> clickPlaceholderRequirements = Collections.emptyList();
   private VisibilityCondition visibleIf = VisibilityCondition.always();
   private String clickRequirementDeniedMessage = "";
   private String clickRequirementDeniedActionBar = "";
   private String clickRequirementDeniedSound = "";
   private float clickRequirementDeniedSoundVolume = 1.0F;
   private float clickRequirementDeniedSoundPitch = 1.0F;
   private List<String> clickRequirementDeniedCommands = Collections.emptyList();
   private boolean toggleContentEnabled = false;
   private String toggleContentOff = "";
   private String toggleContentOn = "";
   private List<String> toggleContentOnCommands = Collections.emptyList();
   private List<String> toggleContentOffCommands = Collections.emptyList();
   private boolean cooldownEnabled = false;
   private String cooldownKey = "";
   private long cooldownMillis = 0L;
   private String cooldownDeniedMessage = "";
   private String cooldownDeniedActionBar = "";
   private List<String> cooldownDeniedCommands = Collections.emptyList();
   private String barValue = "0";
   private String barMax = "100";
   private int barSegments = 10;
   private String barFilled = "|";
   private String barEmpty = "|";
   private String barPrefix = "";
   private String barSuffix = "";
   private boolean refreshMenuEnabled = false;
   private long refreshMenuDelay = 2L;
   private String ownerMenuKey = "";
   private UiElementModel sourceModel = UiElementModel.empty();
   private String inputFieldName = "";
   private String inputFieldPrompt = "";
   private DynamicPositionSpec dynamicPositionSpec;
   private String initialElementState = "";
   private Map<String, ElementStateVariant> elementStateVariants = Collections.emptyMap();
   private Map<String, String> clickElementStates = Collections.emptyMap();
   private String extensionActionId = "";
   private Map<String, Object> extensionActionArgs = Map.of();

   private record ElementStateVariant(
      boolean visible,
      String text,
      TextStyleConfig style,
      String material,
      Integer customModelData,
      Boolean glow
   ) {}

   public MenuLayout(
      String var1,
      String var2,
      List<String> var3,
      boolean var4,
      double var5,
      double var7,
      double var9,
      boolean var11,
      boolean var12,
      Location var13,
      boolean var14,
      List<String> var15,
      float var16,
      float var17,
      float var18,
      String var19,
      boolean var20,
      String var21
   ) {
      this(var1, var2, var3, var4, var5, var7, var9, var11, var12, var13, var14, var15, var16, var17, var18, var19, var20, var21, "");
   }

   public MenuLayout(
      String var1,
      String var2,
      List<String> var3,
      boolean var4,
      double var5,
      double var7,
      double var9,
      boolean var11,
      boolean var12,
      Location var13,
      boolean var14,
      List<String> var15,
      float var16,
      float var17,
      float var18,
      String var19,
      boolean var20,
      String var21,
      String var22
   ) {
      this.key = var1;
      this.name = var2;
      this.command = var3;
      this.stop = var4;
      this.x = var5;
      this.y = var7;
      this.z = var9;
      this.teleportBool = var11;
      this.teleportLoc = var13;
      this.stopCommandBool = var14;
      this.stopCommands = var15;
      this.teleportOriginal = var12;
      this.tiltX = var16;
      this.tiltY = var17;
      this.tiltZ = var18;
      this.permission = var19;
      this.nextMenuEnabled = var20;
      this.nextMenuKey = var21;
      this.commandDelay = 20L;
      this.linkCommand = var22;
   }

   public void setCommandDelay(long var1) {
      this.commandDelay = var1;
      this.useCommandDelay = var1 > 0L;
   }

   public boolean isHoverEnlargeEnabled() {
      return this.hoverEnlargeEnabled;
   }

   public double getHoverEnlargeScale() {
      return this.hoverEnlargeScale;
   }

   public double getTextSize() {
      return this.textSize;
   }

   public boolean isItemLayout() {
      return "item".equalsIgnoreCase(this.type) || this.isPlayerAvatarLayout();
   }

   public boolean isPlayerAvatarLayout() {
      return "player-avatar".equalsIgnoreCase(this.type)
         || "player-head".equalsIgnoreCase(this.type)
         || "avatar".equalsIgnoreCase(this.type);
   }

   public boolean isBarTextLayout() {
      return "bar-text".equalsIgnoreCase(this.type);
   }

   public String getLayoutType() {
      return this.type;
   }

   public String getItemMaterial() {
      return this.itemMaterial;
   }

   public String resolveItemMaterial(Player player) {
      ElementStateVariant variant = this.resolveElementStateVariant(player);
      String configured = variant != null && variant.material() != null ? variant.material() : this.itemMaterial;
      return this.resolveDataBinding(player, configured);
   }

   public int getItemCustomModelData() {
      return this.itemCustomModelData;
   }

   public int resolveItemCustomModelData(Player player) {
      ElementStateVariant variant = this.resolveElementStateVariant(player);
      if (variant != null && variant.customModelData() != null) return variant.customModelData();
      int fallback = this.itemCustomModelData;
      Object raw = this.sourceModel.get(this.sourceModel.contains("custom-model-data") ? "custom-model-data" : "item.custom-model-data");
      if (!(raw instanceof String reference) || !this.isDataBinding(reference)) return fallback;
      try {
         double resolved = Double.parseDouble(this.resolveDataBinding(player, reference));
         return Double.isFinite(resolved) && resolved >= 0 && resolved <= Integer.MAX_VALUE && resolved == Math.rint(resolved)
            ? (int)resolved : fallback;
      } catch (NumberFormatException exception) {
         return fallback;
      }
   }

   public double getItemScale() {
      return this.itemScale;
   }

   public boolean isItemGlow() {
      return this.itemGlow;
   }

   public boolean resolveItemGlow(Player player) {
      ElementStateVariant variant = this.resolveElementStateVariant(player);
      if (variant != null && variant.glow() != null) return variant.glow();
      boolean fallback = this.itemGlow;
      Object raw = this.sourceModel.get(this.sourceModel.contains("glow") ? "glow" : "item.glow");
      if (!(raw instanceof String reference) || !this.isDataBinding(reference)) return fallback;
      String resolved = this.resolveDataBinding(player, reference);
      return "true".equalsIgnoreCase(resolved) || "false".equalsIgnoreCase(resolved)
         ? Boolean.parseBoolean(resolved) : fallback;
   }

   private boolean isDataBinding(String value) {
      return value != null && value.matches("\\{[a-z][a-z0-9_.-]*:[a-z][a-z0-9_.-]*\\}");
   }

   private String resolveDataBinding(Player player, String value) {
      return this.isDataBinding(value)
         ? UIcorePlugin.resolveDataValue(player, this.ownerMenuKey, this.key, value) : value;
   }

   public boolean isItemRotate() {
      return this.itemRotate;
   }

   public double getItemRotateSpeed() {
      return this.itemRotateSpeed;
   }

   public String getItemBillboard() {
      return this.itemBillboard;
   }

   public String getItemDisplayTransform() {
      return this.itemDisplayTransform;
   }

   public String getAvatarPlayer() {
      return this.avatarPlayer;
   }

   public boolean isAvatarFaceOnly() {
      return this.isPlayerAvatarLayout() && this.avatarFaceOnly;
   }

   public String getHitboxText() {
      return this.hitboxText;
   }

   public double getHitboxWidth() {
      return this.hitboxWidth;
   }

   public double getHitboxHeight() {
      return this.hitboxHeight;
   }

   public double getHitboxDepth() {
      return this.hitboxDepth;
   }

   public String getDragGroup() { return this.dragGroup; }
   public String getDragMode() { return this.dragMode; }
   public String getDragTarget() { return this.dragTarget; }
   public double getDragSensitivity() { return this.dragSensitivity; }

   public boolean hasCustomHitbox() {
      return this.customHitboxConfigured;
   }

   public boolean hasToggleContent() {
      return this.toggleContentEnabled;
   }

   public boolean hasInputField() {
      return this.inputFieldName != null && !this.inputFieldName.isBlank();
   }

   public String getInputFieldName() {
      return this.inputFieldName;
   }

   public String getInputFieldPrompt() {
      return this.inputFieldPrompt;
   }

   public boolean hasDynamicPosition() {
      return this.dynamicPositionSpec != null && this.dynamicPositionSpec.isDynamic();
   }

   public DynamicPositionSpec getDynamicPositionSpec() {
      if (this.dynamicPositionSpec == null) {
         this.dynamicPositionSpec = DynamicPositionSpec.from(null, this.x, this.y, this.z);
      }
      return this.dynamicPositionSpec;
   }

   public long getDynamicPositionStartTick() {
      if (!this.entranceAnimationEnabled) {
         return 0L;
      }
      return this.entranceAnimationDelay + Math.max(1, this.entranceAnimationDuration) + 1L;
   }

   public void setOwnerMenuKey(String ownerMenuKey) {
      this.ownerMenuKey = ownerMenuKey == null ? "" : ownerMenuKey;
   }

   public String getOwnerMenuKey() {
      return this.ownerMenuKey;
   }

   public YamlConfiguration copySourceConfig() {
      return ConfigurationCopy.configuration(this.sourceModel.properties());
   }

   public UiElementModel sourceModel() { return this.sourceModel; }

   public String getToggleContentOff() {
      return this.toggleContentOff;
   }

   public String getToggleContentOn() {
      return this.toggleContentOn;
   }

   public boolean isTypewriterEnabled() {
      return this.typewriterEnabled;
   }

   public int getTypewriterSpeed() {
      return this.typewriterSpeed;
   }

   public int getTypewriterStartDelay() {
      return this.typewriterStartDelay;
   }

   public String getTypewriterCursorChar() {
      return this.typewriterCursorChar;
   }

   public String getHoverSound() {
      return this.hoverSound;
   }

   public float getHoverSoundVolume() {
      return this.hoverSoundVolume;
   }

   public float getHoverSoundPitch() {
      return this.hoverSoundPitch;
   }

   public long getHoverSoundCooldown() {
      return this.hoverSoundCooldown;
   }

   public List<String> getHoverCommands() {
      return this.hoverCommands;
   }

   public List<String> getLeaveCommands() {
      return this.leaveCommands;
   }

   public boolean hasHoverCommands() {
      return this.hoverCommands != null && !this.hoverCommands.isEmpty();
   }

   public boolean hasLeaveCommands() {
      return this.leaveCommands != null && !this.leaveCommands.isEmpty();
   }

   public boolean isFloatingAnimationEnabled() {
      return this.floatingAnimationEnabled;
   }

   public String getFloatingAnimationType() {
      return this.floatingAnimationType;
   }

   public double getFloatingAnimationSpeed() {
      return this.floatingAnimationSpeed;
   }

   public double getFloatingAnimationAmplitude() {
      return this.floatingAnimationAmplitude;
   }

   public boolean isEntranceAnimationEnabled() {
      return this.entranceAnimationEnabled;
   }

   public String getEntranceAnimationFrom() {
      return this.entranceAnimationFrom;
   }

   public long getEntranceAnimationDelay() {
      return this.entranceAnimationDelay;
   }

   public int getEntranceAnimationDuration() {
      return this.entranceAnimationDuration;
   }

   public double getEntranceAnimationDistance() {
      return this.entranceAnimationDistance;
   }

   public String getEntranceAnimationEasing() {
      return this.entranceAnimationEasing;
   }

   public List<String> getCommands() {
      return this.command;
   }

   public String getExtensionActionId() { return this.extensionActionId; }

   public Map<String, Object> getExtensionActionArguments() { return this.extensionActionArgs; }

   public boolean hasPermission(Player player) {
      return this.permission == null || this.permission.isEmpty() || player.hasPermission(this.permission) || player.isOp();
   }

   public boolean isVisible(Player player) {
      ElementStateVariant variant = this.resolveElementStateVariant(player);
      return (variant == null || variant.visible()) && (this.visibleIf == null || this.visibleIf.matches(player, this));
   }

   public boolean hasElementState(String state) {
      return state != null && this.elementStateVariants.containsKey(state);
   }

   public String getInitialElementState() {
      return this.initialElementState;
   }

   public Map<String, String> getClickElementStates() {
      return this.clickElementStates;
   }

   private ElementStateVariant resolveElementStateVariant(Player player) {
      if (this.elementStateVariants.isEmpty()) return null;
      String state = player == null || UIcorePlugin.plugin == null
         ? ""
         : UIcorePlugin.plugin.getPlayerElementStateOverride(player, this.ownerMenuKey, this.key);
      return this.elementStateVariants.get(this.elementStateVariants.containsKey(state) ? state : this.initialElementState);
   }

   public boolean hasHoverStyle() {
      return !this.hoverStyle.isEmpty();
   }

   public boolean hasPressedStyle() {
      return !this.pressedStyle.isEmpty();
   }

   public boolean hasPermissionStyle() {
      return !this.permissionAllowedStyle.isEmpty() || !this.permissionDeniedStyle.isEmpty();
   }

   public boolean hasToggleStyle() {
      return !this.toggleOffStyle.isEmpty() || !this.toggleOnStyle.isEmpty();
   }

   public boolean hasHoverCursor() {
      return this.hoverCursorEnabled && this.hoverCursorMaterial != null && !this.hoverCursorMaterial.isBlank();
   }

   public String getHoverCursorMaterial() {
      return this.hoverCursorMaterial;
   }

   public int getHoverCursorCustomModelData() {
      return this.hoverCursorCustomModelData;
   }

   public double getHoverCursorScale() {
      return this.hoverCursorScale;
   }

   public boolean hasHoverTooltip() {
      return this.hoverTooltipText != null && !this.hoverTooltipText.isBlank();
   }

   public String getHoverTooltipType() {
      return this.hoverTooltipType;
   }

   public String getHoverTooltipText() {
      return this.hoverTooltipText;
   }

   public int getPressedStyleDuration() {
      return this.pressedStyleDuration;
   }

   public boolean hasHoverVisualEffect() {
      return this.hasHoverStyle() || this.hasHoverCursor() || this.hasHoverTooltip();
   }

   public boolean hasPermissionDeniedCommands() {
      return this.permissionDeniedCommands != null && !this.permissionDeniedCommands.isEmpty();
   }

   public boolean hasAnyTextStyle() {
      return !this.baseStyle.isEmpty() || this.hasPermissionStyle() || this.hasToggleStyle() || this.hasHoverStyle() || this.hasPressedStyle()
         || this.elementStateVariants.values().stream().anyMatch(variant -> !variant.style().isEmpty());
   }

   public TextStyleConfig resolveTextStyle(Player player, boolean hovered, boolean pressed) {
      TextStyleConfig style = this.baseStyle;
      boolean allowed = this.hasPermission(player);
      if (allowed) {
         style = style.merge(this.permissionAllowedStyle);
      }

      if (this.toggleContentEnabled && player != null) {
         boolean toggled = UIcorePlugin.plugin.getLayoutToggleState(player, this.ownerMenuKey, this.key);
         style = style.merge(toggled ? this.toggleOnStyle : this.toggleOffStyle);
      }

      ElementStateVariant variant = this.resolveElementStateVariant(player);
      if (variant != null) {
         style = style.merge(variant.style());
      }

      if (hovered) {
         style = style.merge(this.hoverStyle);
      }

      if (pressed) {
         style = style.merge(this.pressedStyle);
      }

      if (!allowed) {
         style = style.merge(this.permissionDeniedStyle);
      }

      return style;
   }

   public String resolveTextColor(Player player, boolean hovered, boolean pressed) {
      return this.resolveTextStyle(player, hovered, pressed).color();
   }

   public boolean hasSmoothHoverTransition() {
      return this.hoverStyle.smoothTransition();
   }

   public int getHoverTransitionDuration() {
      return this.hoverStyle.transitionDuration();
   }

   public String renderStyledName(Player player, boolean hovered, boolean pressed) {
      return this.renderStyledText(player, this.resolveDisplayName(player), hovered, pressed);
   }

   public String renderStyledName(Player player, boolean hovered, boolean pressed, String colorOverride) {
      return this.renderStyledText(player, this.resolveDisplayName(player), hovered, pressed, colorOverride);
   }

   private String resolveDisplayName(Player player) {
      ElementStateVariant variant = this.resolveElementStateVariant(player);
      if (variant != null && variant.text() != null) {
         return variant.text();
      }
      if (this.isBarTextLayout()) {
         return this.renderBarText(player);
      }

      if (this.toggleContentEnabled && player != null) {
         return UIcorePlugin.plugin.getLayoutToggleState(player, this.ownerMenuKey, this.key) ? this.toggleContentOn : this.toggleContentOff;
      }

      return this.name;
   }

   public String renderStyledText(Player player, String text, boolean hovered, boolean pressed) {
      return this.renderStyledText(player, text, hovered, pressed, null);
   }

   public String renderStyledText(Player player, String text, boolean hovered, boolean pressed, String colorOverride) {
      String parsedText = UIcorePlugin.parsePlaceholders(player, text == null ? "" : text);
      return this.resolveTextStyle(player, hovered, pressed).render(parsedText, colorOverride);
   }

   public net.kyori.adventure.text.Component renderStyledComponent(Player player, boolean hovered, boolean pressed) {
      return this.renderStyledComponent(player, this.resolveDisplayName(player), hovered, pressed, null);
   }

   public net.kyori.adventure.text.Component renderStyledComponent(Player player, boolean hovered, boolean pressed, String colorOverride) {
      return this.renderStyledComponent(player, this.resolveDisplayName(player), hovered, pressed, colorOverride);
   }

   public net.kyori.adventure.text.Component renderStyledComponent(Player player, String text, boolean hovered, boolean pressed, String colorOverride) {
      return this.resolveTextStyle(player, hovered, pressed).renderComponent(text == null ? "" : text, colorOverride,
         source -> UIcorePlugin.renderDisplayComponent(player, this.ownerMenuKey, this.key, source));
   }

   public void loadConfig(ConfigurationSection var1) {
      this.sourceModel = ConfigurationCopy.model(var1);
      this.dynamicPositionSpec = DynamicPositionSpec.from(var1, this.x, this.y, this.z);
      for (String error : this.dynamicPositionSpec.configurationErrors()) {
         String location = (this.ownerMenuKey == null || this.ownerMenuKey.isBlank() ? "<unknown>" : this.ownerMenuKey) + ":" + this.key;
         if (UIcorePlugin.plugin != null) {
            UIcorePlugin.plugin.getLogger().warning("Invalid dynamic position for " + location + " (" + error + "). Static fallback will be used.");
         }
      }
      this.type = var1.getString("type", "text");
      ConfigurationSection states = var1.getConfigurationSection("states");
      if (states != null) {
         Map<String, ElementStateVariant> variants = new LinkedHashMap<>();
         for (String state : states.getKeys(false)) {
            ConfigurationSection variant = states.getConfigurationSection(state);
            if (state.isBlank() || variant == null) {
               throw new IllegalArgumentException(this.ownerMenuKey + ":" + this.key + " has an invalid element state: " + state);
            }
            variants.put(state, new ElementStateVariant(
               variant.getBoolean("visible", true),
               variant.contains("text") ? variant.getString("text", "") : null,
               TextStyleConfig.from(variant.getConfigurationSection("style")),
               variant.contains("material") ? variant.getString("material", "") : null,
               variant.contains("custom-model-data") ? Math.max(0, variant.getInt("custom-model-data")) : null,
               variant.contains("glow") ? variant.getBoolean("glow") : null
            ));
         }
         if (variants.isEmpty()) {
            throw new IllegalArgumentException(this.ownerMenuKey + ":" + this.key + " has no element states");
         }
         this.elementStateVariants = Collections.unmodifiableMap(variants);
         String configuredInitial = var1.getString("initial-state", "");
         if (!configuredInitial.isBlank() && !variants.containsKey(configuredInitial)) {
            throw new IllegalArgumentException(this.ownerMenuKey + ":" + this.key + " has unknown initial state: " + configuredInitial);
         }
         this.initialElementState = variants.containsKey(configuredInitial) ? configuredInitial
            : variants.containsKey("default") ? "default"
            : variants.keySet().iterator().next();
      }

      Map<String, String> stateChanges = new LinkedHashMap<>();
      if (var1.isString("set-state")) {
         stateChanges.put(this.key, var1.getString("set-state", ""));
      } else {
         ConfigurationSection setState = var1.getConfigurationSection("set-state");
         if (setState != null) {
            stateChanges.put(setState.getString("element", this.key), setState.getString("state", ""));
         }
      }
      ConfigurationSection setStates = var1.getConfigurationSection("set-states");
      if (setStates != null) {
         for (Map.Entry<String, Object> change : setStates.getValues(false).entrySet()) {
            stateChanges.put(change.getKey(), String.valueOf(change.getValue()));
         }
      }
      stateChanges.entrySet().removeIf(change -> change.getKey().isBlank() || change.getValue().isBlank());
      this.clickElementStates = Collections.unmodifiableMap(stateChanges);
      if (var1.contains("command-delay")) {
         this.setCommandDelay(var1.getLong("command-delay", 20L));
      }

      this.baseStyle = TextStyleConfig.from(var1.getConfigurationSection("style"));
      this.hoverStyle = TextStyleConfig.from(var1.getConfigurationSection("hover-style"));
      this.pressedStyle = TextStyleConfig.from(var1.getConfigurationSection("pressed-style"));
      if (var1.contains("pressed-style.duration")) {
         this.pressedStyleDuration = Math.max(1, var1.getInt("pressed-style.duration", 3));
      }

      ConfigurationSection permissionStyle = var1.getConfigurationSection("permission-style");
      if (permissionStyle != null) {
         this.permissionAllowedStyle = TextStyleConfig.from(permissionStyle.getConfigurationSection("allowed"));
         this.permissionDeniedStyle = TextStyleConfig.from(permissionStyle.getConfigurationSection("denied"));
      }

      ConfigurationSection toggleStyle = var1.getConfigurationSection("toggle-style");
      if (toggleStyle != null) {
         this.toggleOffStyle = TextStyleConfig.from(getSectionWithBooleanKeyFallback(toggleStyle, "off", "false"));
         this.toggleOnStyle = TextStyleConfig.from(getSectionWithBooleanKeyFallback(toggleStyle, "on", "true"));
      }

      ConfigurationSection hoverCursor = var1.getConfigurationSection("hover-cursor");
      if (hoverCursor != null) {
         this.hoverCursorEnabled = hoverCursor.getBoolean("enabled", true);
         this.hoverCursorMaterial = hoverCursor.getString("material", "");
         this.hoverCursorCustomModelData = Math.max(0, hoverCursor.getInt("custom-model-data", 0));
         this.hoverCursorScale = hoverCursor.contains("scale") ? hoverCursor.getDouble("scale") : -1.0;
      }

      ConfigurationSection hoverTooltip = var1.getConfigurationSection("hover-tooltip");
      if (hoverTooltip != null) {
         this.hoverTooltipType = hoverTooltip.getString("type", "actionbar");
         this.hoverTooltipText = hoverTooltip.getString("text", "");
      }

      ConfigurationSection permissionDenied = var1.getConfigurationSection("permission-denied");
      if (permissionDenied != null) {
         this.permissionDeniedMessage = permissionDenied.getString("message", "");
         this.permissionDeniedActionBar = permissionDenied.getString("actionbar", "");
         this.permissionDeniedSound = permissionDenied.getString("sound", "");
         this.permissionDeniedSoundVolume = (float)permissionDenied.getDouble("volume", permissionDenied.getDouble("sound-volume", 1.0));
         this.permissionDeniedSoundPitch = (float)permissionDenied.getDouble("pitch", permissionDenied.getDouble("sound-pitch", 1.0));
         this.permissionDeniedClickBehavior = permissionDenied.getString("click-behavior", "deny");
         this.permissionDeniedCommands = permissionDenied.getStringList("commands");
      }

      ConfigurationSection clickRequirements = var1.getConfigurationSection("click-requirements");
      if (clickRequirements != null) {
         this.loadClickRequirements(clickRequirements);

         ConfigurationSection denied = clickRequirements.getConfigurationSection("denied");
         if (denied != null) {
            this.clickRequirementDeniedMessage = denied.getString("message", "");
            this.clickRequirementDeniedActionBar = denied.getString("actionbar", "");
            this.clickRequirementDeniedSound = denied.getString("sound", "");
            this.clickRequirementDeniedSoundVolume = (float)denied.getDouble("volume", denied.getDouble("sound-volume", 1.0));
            this.clickRequirementDeniedSoundPitch = (float)denied.getDouble("pitch", denied.getDouble("sound-pitch", 1.0));
            this.clickRequirementDeniedCommands = denied.getStringList("commands");
         }
      }

      if (var1.contains("visible-if")) {
         Object visibleIfValue = var1.get("visible-if");
         this.visibleIf = VisibilityCondition.fromValue(visibleIfValue);
      }

      ConfigurationSection action = var1.getConfigurationSection("action");
      if (action != null) {
         this.extensionActionId = action.getString("type", "");
         if (this.extensionActionId.isBlank()) throw new IllegalArgumentException(this.ownerMenuKey + ":" + this.key + " action.type is required");
         Map<String, Object> arguments = new LinkedHashMap<>(action.getValues(false));
         arguments.remove("type");
         if (arguments.values().stream().anyMatch(value -> !(value instanceof String || value instanceof Number || value instanceof Boolean)))
            throw new IllegalArgumentException(this.ownerMenuKey + ":" + this.key + " action arguments must be scalar values");
         this.extensionActionArgs = Map.copyOf(arguments);
      }

      ConfigurationSection toggleContent = var1.getConfigurationSection("toggle-content");
      if (toggleContent != null) {
         this.toggleContentEnabled = toggleContent.getBoolean("enabled", true);
         this.toggleContentOff = getStringWithBooleanKeyFallback(toggleContent, "off", "false", toggleContent.getString("before", this.name));
         this.toggleContentOn = getStringWithBooleanKeyFallback(toggleContent, "on", "true", toggleContent.getString("after", this.name));
         this.toggleContentOnCommands = toggleContent.getStringList("on-commands");
         if (this.toggleContentOnCommands.isEmpty()) {
            this.toggleContentOnCommands = toggleContent.getStringList("commands-on");
         }

         this.toggleContentOffCommands = toggleContent.getStringList("off-commands");
         if (this.toggleContentOffCommands.isEmpty()) {
            this.toggleContentOffCommands = toggleContent.getStringList("commands-off");
         }
      }

      ConfigurationSection cooldown = var1.getConfigurationSection("cooldown");
      if (cooldown != null) {
         this.cooldownEnabled = cooldown.getBoolean("enabled", true);
         this.cooldownKey = cooldown.getString("key", "");
         this.cooldownMillis = parseDurationMillis(cooldown);
         ConfigurationSection denied = cooldown.getConfigurationSection("denied");
         if (denied != null) {
            this.cooldownDeniedMessage = denied.getString("message", "");
            this.cooldownDeniedActionBar = denied.getString("actionbar", "");
            this.cooldownDeniedCommands = denied.getStringList("commands");
         }
      }

      ConfigurationSection refreshMenu = var1.getConfigurationSection("refresh-menu");
      if (refreshMenu != null) {
         this.refreshMenuEnabled = refreshMenu.getBoolean("enabled", true);
         long defaultDelay = this.useCommandDelay ? this.commandDelay + 1L : 1L;
         this.refreshMenuDelay = Math.max(0L, refreshMenu.getLong("delay", refreshMenu.getLong("delay-ticks", defaultDelay)));
      } else if (var1.isBoolean("refresh-menu")) {
         this.refreshMenuEnabled = var1.getBoolean("refresh-menu", false);
         this.refreshMenuDelay = this.useCommandDelay ? this.commandDelay + 1L : 1L;
      }

      if ("input-field".equalsIgnoreCase(this.type)) {
         this.inputFieldName = var1.getString("field", this.key);
      }

      if (var1.isString("input-field")) {
         this.inputFieldName = var1.getString("input-field", this.inputFieldName);
      } else {
         ConfigurationSection inputField = var1.getConfigurationSection("input-field");
         if (inputField != null) {
            this.inputFieldName = inputField.getString("field", inputField.getString("name", this.inputFieldName.isBlank() ? this.key : this.inputFieldName));
            this.inputFieldPrompt = inputField.getString("prompt", inputField.getString("message", ""));
         }
      }

      if (this.isBarTextLayout()) {
         ConfigurationSection bar = var1.getConfigurationSection("bar");
         this.barValue = var1.getString("value", bar != null ? bar.getString("value", this.barValue) : this.barValue);
         this.barMax = var1.getString("max", bar != null ? bar.getString("max", this.barMax) : this.barMax);
         this.barSegments = Math.max(1, Math.min(256, var1.getInt("segments", bar != null ? bar.getInt("segments", this.barSegments) : this.barSegments)));
         this.barFilled = var1.getString("filled", bar != null ? bar.getString("filled", this.barFilled) : this.barFilled);
         this.barEmpty = var1.getString("empty", bar != null ? bar.getString("empty", this.barEmpty) : this.barEmpty);
         this.barPrefix = var1.getString("prefix", bar != null ? bar.getString("prefix", this.barPrefix) : this.barPrefix);
         this.barSuffix = var1.getString("suffix", bar != null ? bar.getString("suffix", this.barSuffix) : this.barSuffix);
      }

      if (var1.contains("condition.variable") && var1.contains("condition.operator") && var1.contains("condition.value")) {
         this.conditionVariable = var1.getString("condition.variable");
         this.conditionOperator = var1.getString("condition.operator");
         this.conditionValue = var1.getString("condition.value");
      }

      if (var1.contains("random-commands") && var1.contains("random-chances")) {
         this.randomCommands = var1.getStringList("random-commands");
         this.randomChances = var1.getIntegerList("random-chances");
         this.useRandomCommands = this.randomCommands.size() == this.randomChances.size() && !this.randomCommands.isEmpty();
      }

      if (var1.contains("hover-enlarge.enabled")) {
         this.hoverEnlargeEnabled = var1.getBoolean("hover-enlarge.enabled");
         this.hoverEnlargeScale = var1.getDouble("hover-enlarge.scale", 1.2);
      }

      if (var1.contains("text-size")) {
         this.textSize = var1.getDouble("text-size", 1.0);
      }

      ConfigurationSection configuredHitbox = var1.getConfigurationSection("hitbox");
      this.dragGroup = var1.getString("drag-group", "").trim();
      ConfigurationSection drag = var1.getConfigurationSection("drag");
      if (drag != null) {
         this.dragMode = drag.getString("mode", "").trim().toLowerCase(Locale.ROOT);
         this.dragTarget = drag.getString("target", drag.getString("group", "")).trim();
         double sensitivity = drag.getDouble("sensitivity", 1.0);
         this.dragSensitivity = Double.isFinite(sensitivity) ? Math.max(0.01, Math.min(10.0, sensitivity)) : 1.0;
      }
      if (configuredHitbox != null) {
         this.customHitboxConfigured = true;
         this.hitboxText = configuredHitbox.getString("text", this.hitboxText);
         this.hitboxWidth = Math.max(0.05, configuredHitbox.getDouble("width", this.hitboxWidth));
         this.hitboxHeight = Math.max(0.05, configuredHitbox.getDouble("height", this.hitboxHeight));
         this.hitboxDepth = Math.max(0.05, configuredHitbox.getDouble("depth", this.hitboxDepth));
      }

      if (this.isItemLayout()) {
         ConfigurationSection itemSection = var1.getConfigurationSection("item");
         ConfigurationSection avatarSection = var1.getConfigurationSection("avatar");
         this.itemMaterial = this.isPlayerAvatarLayout()
            ? "PLAYER_HEAD"
            : var1.getString("material", itemSection != null ? itemSection.getString("material", "DIAMOND") : "DIAMOND");
         this.itemCustomModelData = Math.max(0, var1.getInt("custom-model-data", itemSection != null ? itemSection.getInt("custom-model-data", 0) : 0));
         this.itemScale = Math.max(0.01, var1.getDouble("scale", itemSection != null ? itemSection.getDouble("scale", 0.5) : 0.5));
         this.itemGlow = var1.getBoolean("glow", itemSection != null && itemSection.getBoolean("glow", false));
         this.itemRotate = var1.getBoolean("rotate", itemSection != null && itemSection.getBoolean("rotate", false));
         this.itemRotateSpeed = var1.getDouble("rotate-speed", itemSection != null ? itemSection.getDouble("rotate-speed", 1.0) : 1.0);
         this.itemBillboard = var1.getString("billboard", itemSection != null ? itemSection.getString("billboard", "center") : "center");
         this.itemDisplayTransform = var1.getString(
            "transform",
            itemSection != null ? itemSection.getString("transform", this.isPlayerAvatarLayout() ? "head" : "none") : this.isPlayerAvatarLayout() ? "head" : "none"
         );
         if (this.isPlayerAvatarLayout()) {
            this.avatarFaceOnly = var1.getBoolean("face-only", avatarSection != null && avatarSection.getBoolean("face-only", false));
            this.avatarPlayer = var1.getString(
               "player",
               avatarSection != null ? avatarSection.getString("player", avatarSection.getString("owner", "self")) : "self"
            );
         }
         ConfigurationSection hitboxSection = configuredHitbox;
         if (hitboxSection != null) {
            this.hitboxText = hitboxSection.getString("text", this.hitboxText);
            this.hitboxWidth = Math.max(0.05, hitboxSection.getDouble("width", this.itemScale));
            this.hitboxHeight = Math.max(0.05, hitboxSection.getDouble("height", this.itemScale));
            this.hitboxDepth = Math.max(0.05, hitboxSection.getDouble("depth", 0.25));
         } else {
            this.hitboxWidth = Math.max(0.05, this.itemScale);
            this.hitboxHeight = Math.max(0.05, this.itemScale);
         }
      }

      if (var1.contains("typewriter")) {
         ConfigurationSection var2 = var1.getConfigurationSection("typewriter");
         if (var2 != null) {
            this.typewriterEnabled = var2.getBoolean("enabled", false);
            this.typewriterSpeed = Math.max(1, var2.getInt("speed", 2));
            this.typewriterStartDelay = Math.max(0, var2.getInt("start-delay", 0));
            this.typewriterCursorChar = var2.getString("cursor-char", "|");
         }
      }

      if (var1.contains("hover-sound")) {
         ConfigurationSection var4 = var1.getConfigurationSection("hover-sound");
         if (var4 != null) {
            String var3 = var4.getString("name");
            if (var3 != null && !var3.isEmpty()) {
               this.hoverSound = var3;
               this.hoverSoundVolume = (float)var4.getDouble("volume", 1.0);
               this.hoverSoundPitch = (float)var4.getDouble("pitch", 1.0);
               this.hoverSoundCooldown = Math.max(5L, var4.getLong("cooldown", 20L));
            }
         }
      }

      if (var1.contains("hover-commands")) {
         this.hoverCommands = var1.getStringList("hover-commands");
      }

      if (var1.contains("leave-commands")) {
         this.leaveCommands = var1.getStringList("leave-commands");
      }

      if (var1.contains("floating-animation")) {
         ConfigurationSection var5 = var1.getConfigurationSection("floating-animation");
         if (var5 != null) {
            this.floatingAnimationEnabled = var5.getBoolean("enabled", false);
            this.floatingAnimationType = var5.getString("type", "up-down");
            this.floatingAnimationSpeed = Math.max(0.01, var5.getDouble("speed", 1.0));
            this.floatingAnimationAmplitude = Math.max(0.0, var5.getDouble("amplitude", 0.25));
         }
      }

      ConfigurationSection entrance = var1.getConfigurationSection("entrance-animation");
      if (entrance == null) {
         entrance = var1.getConfigurationSection("open-animation");
      }
      if (entrance != null) {
         this.entranceAnimationEnabled = entrance.getBoolean("enabled", true);
         String entranceFrom = entrance.getString("from", "top");
         this.entranceAnimationFrom = entranceFrom == null ? "top" : entranceFrom.trim().toLowerCase(java.util.Locale.ROOT);
         this.entranceAnimationDelay = Math.max(0L, entrance.getLong("delay", 0L));
         this.entranceAnimationDuration = Math.max(0, Math.min(59, entrance.getInt("duration", 10)));
         this.entranceAnimationDistance = Math.max(0.0, entrance.getDouble("distance", 1.0));
         String entranceEasing = entrance.getString("easing", "linear");
         this.entranceAnimationEasing = entranceEasing == null ? "linear" : entranceEasing.trim().toLowerCase(java.util.Locale.ROOT);
      }

      if (var1.contains("animation")) {
         ConfigurationSection animationSection = var1.getConfigurationSection("animation");
         Object animationValue = var1.get("animation");
         if (animationSection != null) {
            this.applyAnimationConfig(animationSection);
         } else if (animationValue != null) {
            this.applyAnimationType(String.valueOf(animationValue), true, 1.0, 0.25, "y");
         }
      }
   }

   private static String getStringWithBooleanKeyFallback(ConfigurationSection section, String key, String booleanKey, String fallback) {
      if (section == null) {
         return fallback;
      }

      String value = section.getString(key, null);
      if (value != null) {
         return value;
      }

      value = section.getString(booleanKey, null);
      return value == null ? fallback : value;
   }

   private static ConfigurationSection getSectionWithBooleanKeyFallback(ConfigurationSection section, String key, String booleanKey) {
      if (section == null) {
         return null;
      }

      ConfigurationSection value = section.getConfigurationSection(key);
      return value == null ? section.getConfigurationSection(booleanKey) : value;
   }

   private static long parseDurationMillis(ConfigurationSection section) {
      if (section == null) {
         return 0L;
      }

      if (section.contains("ticks")) {
         return Math.max(0L, section.getLong("ticks", 0L) * 50L);
      }

      if (section.contains("seconds")) {
         return Math.max(0L, Math.round(section.getDouble("seconds", 0.0) * 1000.0));
      }

      String raw = section.getString("time", section.getString("duration", "0"));
      return parseDurationMillis(raw);
   }

   private static long parseDurationMillis(String raw) {
      if (raw == null || raw.isBlank()) {
         return 0L;
      }

      String value = raw.trim().toLowerCase();
      try {
         if (value.endsWith("ms")) {
            return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 2).trim())));
         }
         if (value.endsWith("tick") || value.endsWith("ticks")) {
            String number = value.replace("ticks", "").replace("tick", "").trim();
            return Math.max(0L, Math.round(Double.parseDouble(number) * 50.0));
         }
         if (value.endsWith("s")) {
            return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 1).trim()) * 1000.0));
         }
         if (value.endsWith("m")) {
            return Math.max(0L, Math.round(Double.parseDouble(value.substring(0, value.length() - 1).trim()) * 60000.0));
         }
         return Math.max(0L, Math.round(Double.parseDouble(value) * 1000.0));
      } catch (NumberFormatException ignored) {
         return 0L;
      }
   }

   private String renderBarText(Player player) {
      double value = parseNumber(UIcorePlugin.resolveDataValue(player, this.ownerMenuKey, this.key, this.barValue));
      double max = parseNumber(UIcorePlugin.resolveDataValue(player, this.ownerMenuKey, this.key, this.barMax));
      if (max <= 0.0) {
         max = 1.0;
      }

      double clampedValue = Math.max(0.0, Math.min(value, max));
      int filledCount = (int)Math.round(clampedValue / max * this.barSegments);
      filledCount = Math.max(0, Math.min(this.barSegments, filledCount));
      int emptyCount = Math.max(0, this.barSegments - filledCount);
      double percent = clampedValue / max * 100.0;
      String bar = repeat(this.barFilled, filledCount) + repeat(this.barEmpty, emptyCount);
      return applyBarPlaceholders(this.barPrefix + bar + this.barSuffix, value, max, percent, filledCount, emptyCount);
   }

   private static double parseNumber(String raw) {
      if (raw == null || raw.isBlank()) {
         return 0.0;
      }

      String normalized = raw.trim().replace(',', '.').replaceAll("[^0-9+\\-.]", "");
      if (normalized.isBlank() || "-".equals(normalized) || "+".equals(normalized) || ".".equals(normalized)) {
         return 0.0;
      }

      try {
         return Double.parseDouble(normalized);
      } catch (NumberFormatException ignored) {
         return 0.0;
      }
   }

   private static String repeat(String value, int count) {
      if (count <= 0) {
         return "";
      }

      return (value == null ? "" : value).repeat(count);
   }

   private static String applyBarPlaceholders(String text, double value, double max, double percent, int filled, int empty) {
      String valueText = formatNumber(value);
      String maxText = formatNumber(max);
      String percentText = formatNumber(percent);
      return (text == null ? "" : text)
         .replace("{value}", valueText)
         .replace("{max}", maxText)
         .replace("{percent}", percentText)
         .replace("{filled}", String.valueOf(filled))
         .replace("{empty}", String.valueOf(empty));
   }

   private static String formatNumber(double value) {
      if (Math.abs(value - Math.rint(value)) < 0.000001) {
         return String.valueOf((long)Math.rint(value));
      }

      return String.format(java.util.Locale.ROOT, "%.1f", value);
   }

   private void applyAnimationConfig(ConfigurationSection section) {
      boolean enabled = section.getBoolean("enabled", true);
      String type = section.getString("type", "");
      ConfigurationSection source = section;
      if (type == null || type.isBlank()) {
         for (String candidate : new String[]{"fade", "move", "rotate", "scale", "bounce"}) {
            ConfigurationSection nested = section.getConfigurationSection(candidate);
            if (nested != null) {
               type = candidate;
               source = nested;
               enabled = nested.getBoolean("enabled", enabled);
               break;
            } else if (section.isBoolean(candidate)) {
               type = candidate;
               enabled = section.getBoolean(candidate, enabled);
               break;
            }
         }
      }

      if (type == null || type.isBlank()) {
         type = "move";
      }

      this.applyAnimationType(
         type,
         enabled,
         source.getDouble("speed", section.getDouble("speed", 1.0)),
         source.getDouble("amplitude", section.getDouble("amplitude", 0.25)),
         source.getString("axis", section.getString("axis", "y"))
      );
   }

   private void applyAnimationType(String type, boolean enabled, double speed, double amplitude, String axis) {
      String normalized = type == null ? "none" : type.trim().toLowerCase();
      if ("move".equals(normalized)) {
         normalized = "x".equalsIgnoreCase(axis) ? "left-right" : "up-down";
      }

      this.floatingAnimationEnabled = enabled && !"none".equalsIgnoreCase(normalized);
      this.floatingAnimationType = normalized;
      this.floatingAnimationSpeed = Math.max(0.01, speed);
      this.floatingAnimationAmplitude = Math.max(0.0, amplitude);
   }

   public void runCommand(Player var1) {
      if (UIcorePlugin.plugin == null) {
         return;
      }
      long sessionGeneration = UIcorePlugin.plugin.getMenuSessionGeneration(var1);
      if (!UIcorePlugin.plugin.isMenuSessionCurrent(var1, sessionGeneration)) {
         return;
      }
      if (this.prepareInteraction(var1)) {
          UIcorePlugin.plugin.selectedLayouts.put(var1, this);
          if (this.pendingClickPlayers.add(var1.getUniqueId())) {
            if (!this.extensionActionId.isBlank() && !UIcorePlugin.plugin.getExtensionManager().executeAction(
               this.extensionActionId, var1, this.ownerMenuKey, this.key, this.extensionActionArgs)) {
               this.pendingClickPlayers.remove(var1.getUniqueId());
               return;
            }
            this.startCooldown(var1);
            if (this.hasInputField()) {
               UIcorePlugin.plugin.setCurrentPlayerInputField(var1, this.inputFieldName);
               String prompt = this.inputFieldPrompt == null || this.inputFieldPrompt.isBlank()
                  ? UIcorePlugin.plugin.getLangMessage("input.field_set", "&a[UIcore] Current input field: %field%").replace("%field%", this.inputFieldName)
                  : UIcorePlugin.parsePlaceholders(var1, this.inputFieldPrompt).replace("%field%", this.inputFieldName);
               CommandUtils.sendActionBar(var1, prompt);
            }

            if (this.toggleContentEnabled) {
               boolean newState = UIcorePlugin.plugin.toggleLayoutContent(var1, this);
               this.executeToggleContentCommands(var1, newState);
            }

            if (!this.clickElementStates.isEmpty()) {
               UIcorePlugin.plugin.applyElementStateChanges(var1, this.ownerMenuKey, this.clickElementStates);
            }

            HoverEnlargeManager hoverManager = UIcorePlugin.plugin.getHoverEnlargeManager();
            if (hoverManager != null) {
               hoverManager.applyPressedStyle(var1, this);
            }

            UIcorePlugin.foliaLib
               .scheduling()
               .entitySpecificScheduler(var1)
               .runDelayed(var1x -> this.pendingClickPlayers.remove(var1.getUniqueId()), null, 1L);
            if (this.linkCommand != null && !this.linkCommand.isEmpty()) {
               this.sendClickableLink(var1, this.linkCommand);
            } else if (this.useRandomCommands) {
               this.executeRandomCommands(var1);
            } else {
               for (String var8 : this.command) {
                  this.dispatchCommand(var1, var8);
               }
            }

             this.scheduleMenuRefresh(var1, sessionGeneration);

            if (this.nextMenuEnabled && !this.nextMenuKey.isEmpty()) {
               UIcorePlugin.foliaLib
                  .scheduling()
                  .entitySpecificScheduler(var1)
                   .runDelayed(var2x -> {
                      if (UIcorePlugin.plugin.isMenuSessionCurrent(var1, sessionGeneration)) {
                         UIcorePlugin.plugin.setupCursor(var1, this.nextMenuKey);
                      }
                   }, null, 5L);
            } else {
               if (this.stop) {
                  UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(var1).runDelayed(var2x -> {
                      boolean var3x = this.teleportBool && this.teleportOriginal;
                   if (UIcorePlugin.plugin.isMenuSessionCurrent(var1, sessionGeneration)) {
                      UIcorePlugin.plugin.stopCursorAsync(var1, var3x).whenComplete((restored, failure) -> {
                         if (failure != null || !Boolean.TRUE.equals(restored)) return;
                         if (this.teleportBool) this.teleport(var1, () -> this.runStopCommand(var1));
                         else var1.getScheduler().execute(UIcorePlugin.plugin, () -> this.runStopCommand(var1), null, 0L);
                      });
                   }
                  }, null, 2L);
               }

            }
         }
      }
   }

   /** Applies the same permission, requirement, condition and cooldown gates as a click. */
   public boolean prepareInteraction(Player player) {
      return this.isInteractionAllowed(player, true);
   }

   /** Rechecks interaction state after an event listener has run, without duplicate feedback. */
   public boolean isInteractionStillAllowed(Player player) {
      return this.isInteractionAllowed(player, false);
   }

   /** Records a non-command interaction, such as a drag, against this layout's cooldown. */
   public void recordInteraction(Player player) {
      this.startCooldown(player);
   }

   private boolean isInteractionAllowed(Player player, boolean notifyPlayer) {
      if (!this.hasPermission(player)) {
         if (notifyPlayer) this.handlePermissionDeniedClick(player);
         return false;
      }
      if (!this.isVisible(player)) {
         if (notifyPlayer) player.sendMessage(UIcorePlugin.plugin.getLangMessage("menu.condition_not_met", "&c[UIcore] The condition was not met."));
         return false;
      }
      if (!this.passesClickRequirements(player)) {
         if (notifyPlayer) this.handleClickRequirementDenied(player);
         return false;
      }
      if (this.conditionVariable != null && this.conditionOperator != null && this.conditionValue != null && !this.checkCondition(player)) {
         if (notifyPlayer) player.sendMessage(UIcorePlugin.plugin.getLangMessage("menu.condition_not_met", "&c[UIcore] The condition was not met."));
         return false;
      }
      if (!this.passesCooldown(player)) {
         if (notifyPlayer) this.handleCooldownDenied(player);
         return false;
      }
      return true;
   }

   private void scheduleMenuRefresh(Player player, long sessionGeneration) {
      if (this.refreshMenuEnabled && !this.stop && (!this.nextMenuEnabled || this.nextMenuKey.isEmpty())) {
         UIcorePlugin.foliaLib.scheduling().entitySpecificScheduler(player).runDelayed(var1 -> {
            if (UIcorePlugin.plugin.isMenuSessionCurrent(player, sessionGeneration) && UIcorePlugin.playerCursors.containsKey(player)) {
               UIcorePlugin.plugin.refreshCurrentMenu(player);
            }
         }, null, this.refreshMenuDelay);
      }
   }

   private boolean passesCooldown(Player player) {
      return !this.cooldownEnabled || this.cooldownMillis <= 0L || UIcorePlugin.plugin.getLayoutCooldownLeftMillis(player, this.resolveCooldownKey(player)) <= 0L;
   }

   private void startCooldown(Player player) {
      if (this.cooldownEnabled && this.cooldownMillis > 0L) {
         UIcorePlugin.plugin.startLayoutCooldown(player, this.resolveCooldownKey(player), this.cooldownMillis);
      }
   }

   private String resolveCooldownKey(Player player) {
      if (this.cooldownKey != null && !this.cooldownKey.isBlank()) {
         return this.cooldownKey;
      }

      String menuKey = this.ownerMenuKey;
      if ((menuKey == null || menuKey.isBlank()) && player != null) {
         menuKey = UIcorePlugin.plugin.getCurrentPlayerMenu(player);
      }

      return (menuKey == null || menuKey.isBlank() ? "menu" : menuKey) + ":" + this.key;
   }

   private void handleCooldownDenied(Player player) {
      long leftMillis = UIcorePlugin.plugin.getLayoutCooldownLeftMillis(player, this.resolveCooldownKey(player));
      if (this.cooldownDeniedMessage != null && !this.cooldownDeniedMessage.isBlank()) {
         CommandUtils.sendChatMessage(player, this.applyCooldownPlaceholders(this.cooldownDeniedMessage, leftMillis));
      }

      if (this.cooldownDeniedActionBar != null && !this.cooldownDeniedActionBar.isBlank()) {
         CommandUtils.sendActionBar(player, this.applyCooldownPlaceholders(this.cooldownDeniedActionBar, leftMillis));
      }

      if (this.cooldownDeniedCommands != null) {
         for (String command : this.cooldownDeniedCommands) {
            CommandUtils.executeCommand(player, this.applyCooldownPlaceholders(command, leftMillis));
         }
      }
   }

   private String applyCooldownPlaceholders(String text, long leftMillis) {
      long leftSeconds = Math.max(1L, (leftMillis + 999L) / 1000L);
      long leftTicks = Math.max(1L, (leftMillis + 49L) / 50L);
      return (text == null ? "" : text)
         .replace("{cooldown.left}", String.valueOf(leftSeconds))
         .replace("%cooldown_left%", String.valueOf(leftSeconds))
         .replace("{cooldown.left_ticks}", String.valueOf(leftTicks))
         .replace("%cooldown_left_ticks%", String.valueOf(leftTicks));
   }

   private boolean passesClickRequirements(Player player) {
      for (String permission : this.clickRequiredPermissions) {
         if (permission != null && !permission.isBlank() && !player.hasPermission(permission) && !player.isOp()) {
            return false;
         }
      }

      for (PlaceholderRequirement requirement : this.clickPlaceholderRequirements) {
         if (!requirement.matches(player)) {
            return false;
         }
      }

      return true;
   }

   private void loadClickRequirements(ConfigurationSection clickRequirements) {
      String location = (this.ownerMenuKey == null || this.ownerMenuKey.isBlank() ? "<unknown>" : this.ownerMenuKey)
         + ":" + this.key + ".click-requirements";
      Object configuredPermissions = clickRequirements.get("permissions");
      if (configuredPermissions != null && !(configuredPermissions instanceof List<?>)) {
         throw new IllegalArgumentException(location + ".permissions must be a list of permission strings");
      }
      List<String> permissions = new ArrayList<>();
      if (configuredPermissions instanceof List<?> configuredList) {
         for (Object value : configuredList) {
            if (!(value instanceof String permission) || permission.isBlank()) {
               throw new IllegalArgumentException(location + ".permissions must contain only non-blank strings");
            }
            permissions.add(permission);
         }
      }
      this.clickRequiredPermissions = List.copyOf(permissions);

      Object configuredPlaceholders = clickRequirements.get("placeholders");
      if (configuredPlaceholders != null && !(configuredPlaceholders instanceof List<?>)) {
         throw new IllegalArgumentException(location + ".placeholders must be a list");
      }
      List<PlaceholderRequirement> placeholders = new ArrayList<>();
      if (configuredPlaceholders instanceof List<?> configuredList) {
         for (Object rawRequirement : configuredList) {
            if (!(rawRequirement instanceof Map<?, ?> requirement)) {
               throw new IllegalArgumentException(location + ".placeholders entries must be maps");
            }
            Object placeholder = requirement.get("placeholder");
            Object operator = requirement.get("operator");
            Object value = requirement.get("value");
            if (!(placeholder instanceof String placeholderText) || placeholderText.isBlank()
               || !(operator instanceof String operatorText) || !isSupportedPlaceholderOperator(operatorText)
               || value == null || value instanceof Map<?, ?> || value instanceof List<?>) {
               throw new IllegalArgumentException(location + ".placeholders entries require placeholder, operator and scalar value");
            }
            placeholders.add(new PlaceholderRequirement(placeholderText, operatorText, String.valueOf(value)));
         }
      }
      this.clickPlaceholderRequirements = List.copyOf(placeholders);
   }

   private static boolean isSupportedPlaceholderOperator(String operator) {
      return operator != null && switch (operator.toLowerCase(Locale.ROOT)) {
         case ">", ">=", "<", "<=", "==", "!=", "contains", "!contains", "not_contains" -> true;
         default -> false;
      };
   }

   private void handleClickRequirementDenied(Player player) {
      if (this.clickRequirementDeniedMessage != null && !this.clickRequirementDeniedMessage.isBlank()) {
         CommandUtils.sendChatMessage(player, this.clickRequirementDeniedMessage);
      } else if (this.permissionDeniedMessage != null && !this.permissionDeniedMessage.isBlank()) {
         CommandUtils.sendChatMessage(player, this.permissionDeniedMessage);
      } else {
         player.sendMessage(UIcorePlugin.plugin.getLangMessage("permission.no_permission_button", "&c[UIcore] You cannot use this element."));
      }

      if (this.clickRequirementDeniedActionBar != null && !this.clickRequirementDeniedActionBar.isBlank()) {
         CommandUtils.sendActionBar(player, this.clickRequirementDeniedActionBar);
      } else if (this.permissionDeniedActionBar != null && !this.permissionDeniedActionBar.isBlank()) {
         CommandUtils.sendActionBar(player, this.permissionDeniedActionBar);
      }

      String sound = this.clickRequirementDeniedSound != null && !this.clickRequirementDeniedSound.isBlank()
         ? this.clickRequirementDeniedSound
         : this.permissionDeniedSound;
      if (sound != null && !sound.isBlank()) {
         boolean customSound = this.clickRequirementDeniedSound != null && !this.clickRequirementDeniedSound.isBlank();
         player.playSound(
            player.getLocation(),
            sound,
            customSound ? this.clickRequirementDeniedSoundVolume : this.permissionDeniedSoundVolume,
            customSound ? this.clickRequirementDeniedSoundPitch : this.permissionDeniedSoundPitch
         );
      }

      List<String> commands = this.clickRequirementDeniedCommands != null && !this.clickRequirementDeniedCommands.isEmpty()
         ? this.clickRequirementDeniedCommands
         : this.permissionDeniedCommands;
      if (commands != null) {
         for (String command : commands) {
            CommandUtils.executeCommand(player, command);
         }
      }
   }

   private void handlePermissionDeniedClick(Player player) {
      if ("ignore".equalsIgnoreCase(this.permissionDeniedClickBehavior)) {
         return;
      }

      if (this.permissionDeniedMessage != null && !this.permissionDeniedMessage.isBlank()) {
         CommandUtils.sendChatMessage(player, this.permissionDeniedMessage);
      } else {
         player.sendMessage(UIcorePlugin.plugin.getLangMessage("permission.no_permission_button", "&c[UIcore] You cannot use this element."));
      }

      if (this.permissionDeniedActionBar != null && !this.permissionDeniedActionBar.isBlank()) {
         CommandUtils.sendActionBar(player, this.permissionDeniedActionBar);
      }

      if (this.permissionDeniedSound != null && !this.permissionDeniedSound.isBlank()) {
         this.playPermissionDeniedSound(player);
      }

      if (this.hasPermissionDeniedCommands()) {
         for (String command : this.permissionDeniedCommands) {
            CommandUtils.executeCommand(player, command);
         }
      }
   }

   private void playPermissionDeniedSound(Player player) {
      player.playSound(player.getLocation(), this.permissionDeniedSound, this.permissionDeniedSoundVolume, this.permissionDeniedSoundPitch);
   }

   private void sendClickableLink(Player var1, String var2) {
      try {
         if (var2.startsWith("[link]")) {
            String var3 = var2.substring(6);
            String[] var4 = var3.split("\\|", 2);
            String var5 = var4[0];
            String var6 = var4.length > 1 ? var4[1] : var5;
            var1.sendMessage(ColorParser.parse(var6).clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(var5)));
         } else {
            var1.sendMessage(ColorParser.parse(var2));
         }
      } catch (RuntimeException var7) {
         var1.sendMessage(ChatColor.translateAlternateColorCodes('&', var2));
      }
   }

   private void executeRandomCommands(Player var1) {
      Random var2 = new Random();
      int var3 = 0;

      for (int var5 : this.randomChances) {
         var3 += var5;
      }

      if (var3 > 0) {
         int var10 = var2.nextInt(var3) + 1;
         int var11 = 0;

         for (int var6 = 0; var6 < this.randomCommands.size(); var6++) {
            var11 += this.randomChances.get(var6);
            if (var10 <= var11) {
               String var7 = this.randomCommands.get(var6);
               this.dispatchCommand(var1, var7);
               break;
            }
         }
      }
   }

   private void executeToggleContentCommands(Player player, boolean newState) {
      List<String> commands = newState ? this.toggleContentOnCommands : this.toggleContentOffCommands;
      if (commands != null && !commands.isEmpty()) {
         for (String command : commands) {
            this.dispatchCommand(player, command);
         }
      }
   }

   private void dispatchCommand(Player var1, String var2) {
      CommandUtils.executeCommand(var1, var2, this.useCommandDelay ? this.commandDelay : 0L);
   }

   private void connectToServer(Player var1, String var2) {
      CommandUtils.connectToServer(var1, var2);
   }

   private void teleport(Player player, Runnable afterTeleport) {
      if (!this.teleportBool) {
         afterTeleport.run();
         return;
      }

      Location target = this.teleportOriginal ? UIcorePlugin.playerLocations.get(player) : this.teleportLoc;
      if (target == null || target.getWorld() == null) {
         target = player.getLocation();
      } else {
         target = target.clone();
      }
      target.setYaw(UIcorePlugin.exitYaw);
      target.setPitch(UIcorePlugin.exitPitch);

      /*
       * A menu can return the player to a different region or world. teleportAsync
       * transfers ownership without blocking the entity region; its completion can
       * run on an arbitrary thread, so continue on the player's scheduler.
       */
      player.teleportAsync(target).whenComplete((ignored, failure) -> {
         if (UIcorePlugin.plugin != null) {
            player.getScheduler().execute(UIcorePlugin.plugin, afterTeleport, null, 0L);
         }
      });
   }

   private void runStopCommand(Player var1) {
      if (this.stopCommandBool) {
         for (String var3 : this.stopCommands) {
            CommandUtils.executeCommand(var1, var3);
         }
      }
   }

   private boolean checkCondition(Player var1) {
      if (!UIcorePlugin.hasPAPI) {
         return false;
      } else {
         String var2 = PlaceholderAPI.setPlaceholders(var1, this.conditionVariable);
         if (var2 == null || var2.matches("(?s).*%[^%\\s]+%.*")) return false;

         try {
            double var3 = Double.parseDouble(var2);
            double var5 = Double.parseDouble(this.conditionValue);
            if (!Double.isFinite(var3) || !Double.isFinite(var5)) return false;
            String var7 = this.conditionOperator;
            switch (var7) {
               case ">":
                  return var3 > var5;
               case ">=":
                  return var3 >= var5;
               case "<":
                  return var3 < var5;
               case "<=":
                  return var3 <= var5;
               case "==":
                  return var3 == var5;
               case "!=":
                  return var3 != var5;
               default:
                  return false;
            }
         } catch (NumberFormatException var10) {
            String var4 = this.conditionOperator;
            switch (var4) {
               case "==":
                  return var2.equals(this.conditionValue);
               case "!=":
                  return !var2.equals(this.conditionValue);
               default:
                  return false;
            }
         }
      }
   }

   private static class VisibilityCondition {
      private static final int MAX_REGEX_LENGTH = 256;
      private static final int MAX_REGEX_INPUT_LENGTH = 1024;
      private final String kind;
      private final String key;
      private final String operator;
      private final String expected;
      private final List<VisibilityCondition> children;
      private final com.google.re2j.Pattern regex;
      private final Map<String, Object> arguments;

      private VisibilityCondition(String kind, String key, String operator, String expected, List<VisibilityCondition> children) {
         this(kind, key, operator, expected, children, Map.of());
      }

      private VisibilityCondition(String kind, String key, String operator, String expected, List<VisibilityCondition> children, Map<String, Object> arguments) {
         this.kind = kind;
         this.key = key == null ? "" : key;
         this.operator = operator == null ? "==" : operator;
         this.expected = expected == null ? "" : expected;
         this.children = children == null ? Collections.emptyList() : children;
         this.regex = "matches".equals(this.operator) ? compileRegex(this.expected) : null;
         this.arguments = Map.copyOf(arguments);
      }

      static VisibilityCondition always() {
         return new VisibilityCondition("always", "", "==", "", Collections.emptyList());
      }

      static VisibilityCondition never() {
         return new VisibilityCondition("never", "", "==", "", Collections.emptyList());
      }

      static VisibilityCondition invalid() {
         return new VisibilityCondition("invalid", "", "==", "", Collections.emptyList());
      }

      static VisibilityCondition from(ConfigurationSection section) {
         if (section == null) {
            return always();
         }

          for (String structural : List.of("all", "any", "not")) if (section.contains(structural) && section.getKeys(false).size() != 1) return invalid();
          Object allValue = section.get("all");
          if (allValue instanceof List<?>) {
             List<VisibilityCondition> children = fromList((List<?>)allValue);
             return children.isEmpty() || children.stream().anyMatch(VisibilityCondition::isInvalid)
                ? invalid()
                : new VisibilityCondition("all", "", "==", "", children);
          }
          if (section.contains("all")) {
             return invalid();
          }

          Object anyValue = section.get("any");
          if (anyValue instanceof List<?>) {
             List<VisibilityCondition> children = fromList((List<?>)anyValue);
             return children.isEmpty() || children.stream().anyMatch(VisibilityCondition::isInvalid)
                ? invalid()
                : new VisibilityCondition("any", "", "==", "", children);
          }
          if (section.contains("any")) {
             return invalid();
          }

          if (section.contains("not")) {
             VisibilityCondition child = fromValue(section.get("not"));
             return child.isInvalid() ? invalid() : new VisibilityCondition("not", "", "==", "", List.of(child));
          }

         if (section.contains("custom")) {
            ConfigurationSection custom = section.getConfigurationSection("custom");
            if (custom == null || section.getKeys(false).size() != 1) return invalid();
            String id = custom.getString("type", "");
            if (id.isBlank()) return invalid();
            Map<String, Object> arguments = new LinkedHashMap<>(custom.getValues(false));
            arguments.remove("type");
            if (arguments.values().stream().anyMatch(value -> !(value instanceof String || value instanceof Number || value instanceof Boolean))) return invalid();
            return new VisibilityCondition("custom", id, "==", "", Collections.emptyList(), arguments);
         }

         java.util.Set<String> accepted = java.util.Set.of("permission", "placeholder", "state", "operator", "value", "equals", "not-equals", "not_equals", "contains", "not-contains", "not_contains", "matches", "greater-than", "greater_than", "greater-or-equal", "greater_or_equal", "less-than", "less_than", "less-or-equal", "less_or_equal");
         for (String key : section.getKeys(false)) if (!accepted.contains(key) && !key.startsWith("state.")) return invalid();
         List<VisibilityCondition> conditions = new ArrayList<>();
         String permission = section.getString("permission", "");
         if (!permission.isBlank()) {
            conditions.add(new VisibilityCondition("permission", permission, "==", "true", Collections.emptyList()));
         }

         String placeholder = section.getString("placeholder", "");
         if (!placeholder.isBlank()) {
            conditions.add(new VisibilityCondition("placeholder", placeholder, operatorFrom(section), expectedFrom(section), Collections.emptyList()));
         }

         ConfigurationSection stateSection = section.getConfigurationSection("state");
         if (stateSection != null) {
            for (Map.Entry<String, Object> entry : stateSection.getValues(false).entrySet()) {
               conditions.add(new VisibilityCondition("state", entry.getKey(), "==", String.valueOf(entry.getValue()), Collections.emptyList()));
            }
         }

         for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
            String directKey = entry.getKey();
            if (directKey.startsWith("state.")) {
               conditions.add(new VisibilityCondition("state", directKey.substring("state.".length()), "==", String.valueOf(entry.getValue()), Collections.emptyList()));
            }
         }

          if (conditions.isEmpty()) {
             return invalid();
         }

         return conditions.size() == 1 ? conditions.get(0) : new VisibilityCondition("all", "", "==", "", conditions);
      }

      static VisibilityCondition fromValue(Object value) {
         if (value instanceof ConfigurationSection section) {
            return from(section);
         }
         if (value instanceof Map<?, ?> values) {
            return from(sectionFromMap(values));
         }
         if (value instanceof List<?> values) {
            List<VisibilityCondition> children = fromList(values);
            return children.isEmpty() || children.stream().anyMatch(VisibilityCondition::isInvalid)
               ? invalid()
               : new VisibilityCondition("all", "", "==", "", children);
         }
         if (value instanceof Boolean booleanValue) {
            return booleanValue ? always() : never();
         }
         if (value instanceof String permission && !permission.isBlank()) {
            return new VisibilityCondition("permission", permission.trim(), "==", "true", Collections.emptyList());
         }
         return invalid();
      }

      private static List<VisibilityCondition> fromList(List<?> values) {
         List<VisibilityCondition> conditions = new ArrayList<>();
         for (Object value : values) {
            if (value instanceof Map<?, ?>) {
               conditions.add(from(sectionFromMap((Map<?, ?>)value)));
            } else if (value instanceof Boolean booleanValue) {
               conditions.add(booleanValue ? always() : never());
            } else if (value instanceof String) {
               String raw = ((String)value).trim();
               if (!raw.isBlank()) {
                  conditions.add(new VisibilityCondition("permission", raw, "==", "true", Collections.emptyList()));
               }
            } else {
               conditions.add(invalid());
            }
         }

         return conditions;
      }

      private static ConfigurationSection sectionFromMap(Map<?, ?> values) {
         YamlConfiguration config = new YamlConfiguration();
         for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() != null) {
               putMapValue(config, String.valueOf(entry.getKey()), entry.getValue());
            }
         }

         return config;
      }

      private static void putMapValue(YamlConfiguration config, String path, Object value) {
         if (value instanceof Map<?, ?> nested) {
            for (Map.Entry<?, ?> entry : nested.entrySet()) {
               if (entry.getKey() != null) {
                  putMapValue(config, path + "." + entry.getKey(), entry.getValue());
               }
            }
         } else {
            config.set(path, value);
         }
      }

      private static String operatorFrom(ConfigurationSection section) {
         String explicit = section.getString("operator", "");
         if (!explicit.isBlank()) {
            return normalizeOperator(explicit);
         } else if (section.contains("equals")) {
            return "==";
         } else if (section.contains("not-equals") || section.contains("not_equals")) {
            return "!=";
         } else if (section.contains("contains")) {
            return "contains";
         } else if (section.contains("not-contains") || section.contains("not_contains")) {
            return "!contains";
         } else if (section.contains("matches")) {
            return "matches";
         } else if (section.contains("greater-than") || section.contains("greater_than")) {
            return ">";
         } else if (section.contains("greater-or-equal") || section.contains("greater_or_equal")) {
            return ">=";
         } else if (section.contains("less-than") || section.contains("less_than")) {
            return "<";
         } else if (section.contains("less-or-equal") || section.contains("less_or_equal")) {
            return "<=";
         } else {
            return "==";
         }
      }

      private static String expectedFrom(ConfigurationSection section) {
         String[] keys = new String[]{
            "value",
            "equals",
            "not-equals",
            "not_equals",
            "contains",
            "not-contains",
            "not_contains",
            "matches",
            "greater-than",
            "greater_than",
            "greater-or-equal",
            "greater_or_equal",
            "less-than",
            "less_than",
            "less-or-equal",
            "less_or_equal"
         };

         for (String key : keys) {
            if (section.contains(key)) {
               Object value = section.get(key);
               return value == null ? "" : String.valueOf(value);
            }
         }

         return "";
      }

      private static String normalizeOperator(String operator) {
         String lower = operator.trim().toLowerCase();
         switch (lower) {
            case "equals":
            case "eq":
               return "==";
            case "not-equals":
            case "not_equals":
            case "neq":
               return "!=";
            case "not-contains":
            case "not_contains":
               return "!contains";
            case "greater-than":
            case "greater_than":
               return ">";
            case "greater-or-equal":
            case "greater_or_equal":
               return ">=";
            case "less-than":
            case "less_than":
               return "<";
            case "less-or-equal":
            case "less_or_equal":
               return "<=";
            default:
               return operator;
         }
      }

      private boolean canEvaluate(Player player, MenuLayout layout) {
         if (this.isInvalid() || !List.of("==", "!=", ">", ">=", "<", "<=", "contains", "!contains", "matches").contains(normalizeOperator(this.operator))) return false;
         if ("permission".equals(this.kind) && player == null) return false;
         if ("placeholder".equals(this.kind)) {
            if (!UIcorePlugin.hasPAPI || player == null) return false;
            String resolved = UIcorePlugin.parsePlaceholders(player, this.key);
            if (resolved != null && resolved.matches("(?s).*%[^%\\s]+%.*")) return false;
            if (List.of(">", ">=", "<", "<=").contains(normalizeOperator(this.operator))) {
               try { if (!Double.isFinite(Double.parseDouble(resolved)) || !Double.isFinite(Double.parseDouble(this.expected))) return false; }
               catch (RuntimeException ignored) { return false; }
            }
         }
         if ("state".equals(this.kind) && (player == null || layout == null || UIcorePlugin.plugin == null)) return false;
         if ("custom".equals(this.kind) && (player == null || layout == null || UIcorePlugin.plugin == null)) return false;
         for (VisibilityCondition child : this.children) if (!child.canEvaluate(player, layout)) return false;
         return true;
      }

      boolean matches(Player player, MenuLayout layout) {
         return Boolean.TRUE.equals(this.evaluate(player, layout));
      }

      private Boolean evaluate(Player player, MenuLayout layout) {
         if (!this.canEvaluate(player, layout)) return null;
         switch (this.kind) {
            case "always":
               return true;
            case "never":
               return false;
            case "permission":
               return player != null && (player.hasPermission(this.key) || player.isOp());
            case "placeholder":
                return UIcorePlugin.hasPAPI
                   && player != null
                   && this.compare(UIcorePlugin.parsePlaceholders(player, this.key), this.expected, this.operator);
            case "state":
                return player != null
                   && layout != null
                   && UIcorePlugin.plugin != null
                   && this.compare(UIcorePlugin.plugin.getPlayerMenuState(player, layout.getOwnerMenuKey(), this.key), UIcorePlugin.parsePlaceholders(player, this.expected), this.operator);
            case "custom":
               return UIcorePlugin.plugin.getExtensionManager()
                  .evaluateCondition(this.key, player, layout.getOwnerMenuKey(), layout.key, this.arguments).orElse(null);
            case "all":
               for (VisibilityCondition child : this.children) {
                  Boolean value = child.evaluate(player, layout);
                  if (value == null) return null;
                  if (!value) return false;
               }

               return true;
            case "any":
               boolean any = false;
               for (VisibilityCondition child : this.children) {
                  Boolean value = child.evaluate(player, layout);
                  if (value == null) return null;
                  any |= value;
               }

               return any;
            case "not":
               if (this.children.size() != 1) return null;
               Boolean childValue = this.children.get(0).evaluate(player, layout);
               return childValue == null ? null : !childValue;
            default:
               return null;
         }
      }

      private boolean isInvalid() {
         return "invalid".equals(this.kind);
      }

      private boolean compare(String actual, String expected, String operator) {
         String left = actual == null ? "" : actual;
         String right = expected == null ? "" : expected;
         String op = normalizeOperator(operator == null ? "==" : operator);
         try {
            double leftNumber = Double.parseDouble(left);
            double rightNumber = Double.parseDouble(right);
            if (!Double.isFinite(leftNumber) || !Double.isFinite(rightNumber)) return false;
            switch (op) {
               case ">":
                  return leftNumber > rightNumber;
               case ">=":
                  return leftNumber >= rightNumber;
               case "<":
                  return leftNumber < rightNumber;
               case "<=":
                  return leftNumber <= rightNumber;
               case "==":
                  return leftNumber == rightNumber;
               case "!=":
                  return leftNumber != rightNumber;
               default:
                  break;
            }
         } catch (NumberFormatException var8) {
         }

         switch (op.toLowerCase()) {
            case "==":
               return left.equalsIgnoreCase(right);
            case "!=":
               return !left.equalsIgnoreCase(right);
            case "contains":
               return left.contains(right);
            case "!contains":
               return !left.contains(right);
            case "matches":
               return this.regex != null && left.length() <= MAX_REGEX_INPUT_LENGTH && this.regex.matcher(left).matches();
            default:
               return false;
         }
      }

      private static com.google.re2j.Pattern compileRegex(String expression) {
         if (expression == null || expression.length() > MAX_REGEX_LENGTH || expression.indexOf('%') >= 0 || expression.contains("{input.") || expression.contains("{menu.input."))
            throw new IllegalArgumentException("matches requires a fixed regex of at most " + MAX_REGEX_LENGTH + " characters");
         try { return com.google.re2j.Pattern.compile(expression); }
         catch (com.google.re2j.PatternSyntaxException exception) { throw new IllegalArgumentException("Invalid matches regex: " + exception.getDescription(), exception); }
      }

   }

   private static class PlaceholderRequirement {
      private final String placeholder;
      private final String operator;
      private final String expected;

      PlaceholderRequirement(String placeholder, String operator, String expected) {
         this.placeholder = placeholder;
         this.operator = operator;
         this.expected = expected;
      }

      boolean matches(Player player) {
         String actual = UIcorePlugin.parsePlaceholders(player, this.placeholder);
         String expectedValue = UIcorePlugin.parsePlaceholders(player, this.expected);
         if (actual == null || expectedValue == null || actual.matches("(?s).*%[^%\\s]+%.*") || expectedValue.matches("(?s).*%[^%\\s]+%.*")) return false;
         try {
            double actualNumber = Double.parseDouble(actual);
            double expectedNumber = Double.parseDouble(expectedValue);
            switch (this.operator) {
               case ">":
                  return actualNumber > expectedNumber;
               case ">=":
                  return actualNumber >= expectedNumber;
               case "<":
                  return actualNumber < expectedNumber;
               case "<=":
                  return actualNumber <= expectedNumber;
               case "==":
                  return actualNumber == expectedNumber;
               case "!=":
                  return actualNumber != expectedNumber;
               default:
                  return false;
            }
         } catch (NumberFormatException var7) {
            switch (this.operator.toLowerCase()) {
               case "==":
                  return actual.equals(expectedValue);
               case "!=":
                  return !actual.equals(expectedValue);
               case "contains":
                  return actual.contains(expectedValue);
               case "!contains":
               case "not_contains":
                  return !actual.contains(expectedValue);
               default:
                  return false;
            }
         }
      }
   }
}
