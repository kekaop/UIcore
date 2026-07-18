# NPC镜像系统集成文档

## 概述

NPC镜像系统是从 CustomMenu 插件移植到 CustomScreenMenu (CMP) 的模块化功能。该系统允许在玩家进入3D菜单时创建一个与玩家外观相同的 NPC 镜像。

## 模块架构

```
com.cmenu.ui.npc/
├── NPCModule.java           # 模块入口类
├── NPCMirrorManager.java    # NPC镜像管理器
├── NPCMirrorHook.java       # 菜单系统钩子
├── NPCConfig.java           # 配置管理
└── NPCCommandHandler.java   # 命令处理器
```

## 依赖要求

### 必需依赖
- **FancyNpcs** (v2.8.0+) - NPC创建和管理核心

### 可选依赖
- **SkinsRestorer** (v15.8.2+) - 皮肤同步支持

## 安装说明

1. 确保服务器已安装 FancyNpcs 插件
2. 可选安装 SkinsRestorer 以支持皮肤同步
3. 将更新后的 CustomScreenMenu.jar 放入 plugins 目录
4. 重启服务器

## 配置文件

首次运行会在 `plugins/CustomScreenMenu/` 目录下生成 `npc_config.yml`：

```yaml
# =========================================== 
# NPC镜像系统配置文件 
# CustomScreenMenu NPC Mirror Module 
# =========================================== 

# 是否启用NPC镜像功能
enabled: true

# 调试模式
debug-mode: false

# NPC相对于玩家位置的偏移量
offset:
  x: 0.0
  y: 0.0
  z: 0.0

# NPC默认朝向
default-yaw: 180.0
default-pitch: 0.0

# 自动旋转设置
auto-rotate:
  enabled: false
  speed: 1.0

# 同步设置
sync:
  equipment: true    # 同步玩家装备
  skin: true         # 同步玩家皮肤

# 禁用NPC的世界列表
disabled-worlds: []

# 禁用NPC的玩家UUID列表
disabled-players: []

# 菜单关联设置
menu-settings:
  # 是否对所有菜单启用NPC（true=全部启用）
  enable-for-all-menus: true
  # 启用NPC的菜单列表
  enabled-menus: []
  # 是否使用白名单模式（true=仅列表中的菜单启用，false=列表中的菜单禁用）
  use-whitelist-mode: false
```

### 菜单关联配置说明

#### 模式一：全部启用（默认）
```yaml
menu-settings:
  enable-for-all-menus: true
  use-whitelist-mode: false
  enabled-menus: []
```
所有菜单都会创建NPC。

#### 模式二：全部启用，但排除某些菜单
```yaml
menu-settings:
  enable-for-all-menus: true
  use-whitelist-mode: false
  enabled-menus: ["login", "register"]  # 这些菜单不会创建NPC
```
除了 login 和 register 菜单外，其他菜单都会创建NPC。

#### 模式三：仅对指定菜单启用（白名单模式）
```yaml
menu-settings:
  enable-for-all-menus: false
  use-whitelist-mode: true
  enabled-menus: ["main_menu", "lobby", "settings"]  # 只有这些菜单会创建NPC
```
只有在列表中的菜单才会创建NPC。

#### 模式四：全部禁用，但排除某些菜单
```yaml
menu-settings:
  enable-for-all-menus: false
  use-whitelist-mode: false
  enabled-menus: ["main_menu"]  # 这些菜单会创建NPC
```
默认禁用，但列表中的菜单会创建NPC。

## 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/cursormenu npc toggle` | 切换NPC创建状态 | cursormenu.npc.toggle |
| `/cursormenu npc enable` | 启用NPC创建 | cursormenu.npc.toggle |
| `/cursormenu npc disable` | 禁用NPC创建 | cursormenu.npc.toggle |
| `/cursormenu npc status` | 查看NPC状态 | cursormenu.npc.status |
| `/cursormenu npc reload` | 重载NPC配置 | cursormenu.npc.reload |
| `/cursormenu npc rotate <角度>` | 旋转NPC | cursormenu.npc.rotate |
| `/cursormenu npc help` | 显示帮助 | - |

## 权限

```yaml
cursormenu.npc.toggle    # 切换NPC创建状态
cursormenu.npc.status    # 查看NPC状态
cursormenu.npc.reload    # 重载配置
cursormenu.npc.rotate    # 旋转NPC
```

## API 使用

### 在菜单打开时创建NPC

```java
import com.cmenu.ui.npc.NPCModule;

// 在菜单打开时
NPCModule.onMenuOpen(player, menuLocation, yaw, pitch);
```

### 在菜单关闭时移除NPC

```java
// 在菜单关闭时
NPCModule.onMenuClose(player);
```

### 在菜单切换时更新NPC

```java
// 在菜单切换时
NPCModule.onMenuSwitch(player, newLocation, newYaw, newPitch);
```

### 检查模块状态

```java
if (NPCModule.isModuleEnabled()) {
    // NPC模块可用
}
```

### 获取管理器实例

```java
NPCMirrorManager manager = NPCModule.getInstance().getMirrorManager();
NPCMirrorHook hook = NPCModule.getInstance().getMirrorHook();
```

## 集成点说明

### 1. 主类集成 (CursorMenuPlugin.java)

```java
// onEnable() 中初始化
NPCModule.initialize(this);

// onDisable() 中关闭
NPCModule.shutdown();

// reloadPluginConfig() 中重载
NPCModule.reload();
```

### 2. 菜单生命周期集成

- `setupCursor()` - 调用 `NPCModule.onMenuOpen()`
- `stopCursor()` - 调用 `NPCModule.onMenuClose()`
- 菜单切换 - 调用 `NPCModule.onMenuSwitch()`

## 功能特性

### 玩家镜像NPC
- 创建与玩家外观完全相同的NPC
- 自动同步玩家装备（头盔、胸甲、护腿、靴子、主手、副手）
- 通过 SkinsRestorer 同步玩家皮肤

### 位置控制
- 可配置NPC相对于菜单的位置偏移
- 可配置NPC默认朝向
- 支持动态旋转

### 玩家控制
- 玩家可单独禁用自己的NPC创建
- 状态持久化保存

### 兼容性
- 不影响原有菜单功能
- FancyNpcs 未安装时自动禁用
- SkinsRestorer 未安装时使用默认皮肤

## 故障排除

### NPC不显示
1. 检查 FancyNpcs 是否正确安装
2. 检查 `npc_config.yml` 中 `enabled` 是否为 `true`
3. 检查玩家是否禁用了NPC创建

### 皮肤不同步
1. 确保 SkinsRestorer 已安装
2. 检查 `sync.skin` 配置是否为 `true`
3. 检查 SkinsRestorer 是否正常工作

### NPC位置不正确
1. 调整 `offset` 配置
2. 检查菜单的相机位置设置

## 技术细节

### 实体命名规则
NPC名称格式：`CMP_MIRROR_<玩家名>`

### 实体属性
- `saveToFile: false` - 不保存到文件
- 自动清理 - 玩家退出菜单时自动移除

### 线程安全
- 使用 `ConcurrentHashMap` 存储玩家NPC映射
- 所有NPC操作在主线程执行

## 更新日志

### v1.0.0
- 初始版本
- 从 CustomMenu 移植 NPC 镜像功能
- 模块化设计，独立于核心代码
