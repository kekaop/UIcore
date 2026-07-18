# WASD导航系统集成文档

## 概述

WASD导航系统是从 CustomMenu 插件移植到 CustomScreenMenu (CMP) 的模块化功能。该系统允许玩家使用 WASD 键盘导航和空格确认来操作菜单，与原有光标控制系统并存。

## 模块架构

```
com.cmenu.ui.wasd/
├── WASDModule.java           # 模块入口类
├── WASDNavigationManager.java # 导航管理器
├── WASDNavigationHook.java   # 菜单系统钩子
├── WASDConfig.java           # 配置管理
├── WASDSession.java          # 玩家会话
└── WASDExpansion.java        # PAPI变量扩展
```

## 依赖要求

### 必需依赖
- **PacketEvents** - 数据包监听
- **PlaceholderAPI** - 变量支持

## 安装说明

1. 确保服务器已安装 PlaceholderAPI 插件
2. 将更新后的 CustomScreenMenu.jar 放入 plugins 目录
3. 重启服务器

## 配置文件

首次运行会在 `plugins/CustomScreenMenu/` 目录下生成 `wasd_config.yml`：

```yaml
# ===========================================
# WASD导航系统配置文件
# CustomScreenMenu WASD Navigation Module
# ===========================================

enabled: true
debug-mode: false

navigation:
  horizontal-threshold: 0.5
  dot-product-threshold: 0.5
  selection-cooldown: 500

sound:
  enabled: true
  selection-sound: "minecraft:entity.experience_orb.pickup"
  volume: 1.0
  pitch: 1.0

menu-settings:
  enable-for-all-menus: true
  enabled-menus: []
  use-whitelist-mode: false

disabled-players: []
```

## 菜单配置

在菜单配置文件中添加 `wasd-enabled` 选项：

```yaml
example_menu:
  world: "world"
  x: 100
  y: 64
  z: 200
  yaw: 0
  pitch: 0
  wasd-enabled: true  # 是否启用WASD导航
  layouts:
    button1:
      name: "&a按钮1"
      command: "[console] say 点击了按钮1"
      x: 0
      y: 1
      z: 2
```

## 操作方式

| 按键 | 功能 |
|------|------|
| **W** | 向上选择 |
| **S** | 向下选择 |
| **A** | 向左选择 |
| **D** | 向右选择 |
| **空格** | 确认执行 |

## PAPI 变量

WASD导航系统提供以下 PlaceholderAPI 变量：

### 基础变量

| 变量 | 描述 | 示例输出 |
|------|------|----------|
| `%cmp_wasd_menu%` | 当前玩家所在的WASD菜单名称 | `test_menu` |
| `%cmp_wasd_enabled%` | 当前玩家是否启用了WASD导航 | `true` / `false` |
| `%cmp_wasd_index%` | 当前玩家选中的索引 | `0` |
| `%cmp_wasd_x%` | 当前选中位置的X坐标 | `100.50` |
| `%cmp_wasd_y%` | 当前选中位置的Y坐标 | `64.25` |
| `%cmp_wasd_z%` | 当前选中位置的Z坐标 | `200.75` |
| `%cmp_wasd_location%` | 当前位置的完整信息 | `test_menu,100.50,64.25,200.75` |

### 菜单特定变量

| 变量格式 | 描述 |
|----------|------|
| `%cmp_<菜单名>_xyz%` | 指定菜单的XYZ坐标（仅当玩家在该菜单时有效） |
| `%cmp_<菜单名>_x%` | 指定菜单的X坐标 |
| `%cmp_<菜单名>_y%` | 指定菜单的Y坐标 |
| `%cmp_<菜单名>_z%` | 指定菜单的Z坐标 |

### 使用示例

**在菜单文本中显示当前坐标：**
```yaml
layouts:
  info_button:
    name: "&e当前位置: %cmp_wasd_location%"
    x: 0
    y: 0
    z: 2
```

**检测玩家是否在特定菜单：**
```
%cmp_test_menu_xyz%
```
如果玩家在 `test` 菜单中，返回坐标；否则返回空字符串。

## 菜单关联配置

### 模式一：全部启用（默认）
```yaml
menu-settings:
  enable-for-all-menus: true
  use-whitelist-mode: false
  enabled-menus: []
```
所有启用 `wasd-enabled: true` 的菜单都会启用WASD导航。

### 模式二：仅指定菜单启用
```yaml
menu-settings:
  enable-for-all-menus: false
  use-whitelist-mode: true
  enabled-menus: ["main_menu", "lobby"]
```
只有在列表中的菜单才会启用WASD导航。

### 模式三：排除特定菜单
```yaml
menu-settings:
  enable-for-all-menus: true
  use-whitelist-mode: false
  enabled-menus: ["login", "register"]
```
除了 login 和 register 菜单外，其他菜单都启用WASD导航。

## 与光标系统的兼容

WASD导航系统与原有光标控制系统完全兼容：

1. **并存运行**：两种操作方式可以同时使用
2. **无冲突**：WASD导航不会干扰光标控制
3. **平滑切换**：玩家可以随时选择使用哪种方式

## API 使用

### 检查模块状态

```java
if (WASDModule.isModuleEnabled()) {
    // WASD模块可用
}
```

### 获取玩家当前菜单

```java
String menu = WASDModule.getPlayerCurrentMenu(player);
```

### 获取玩家当前位置

```java
Location loc = WASDModule.getPlayerCurrentLocation(player);
```

### 检查菜单是否启用WASD

```java
if (WASDModule.isMenuEnabled("test_menu")) {
    // 菜单启用了WASD导航
}
```

## 技术细节

### 导航算法

| 方向 | 判断方式 | 条件 |
|------|----------|------|
| **上（W）** | Y 轴差值 | Y 值更大，X/Z 偏移 < 阈值 |
| **下（S）** | Y 轴差值 | Y 值更小，X/Z 偏移 < 阈值 |
| **左（A）** | 向量点积 | 与左向量点积 > 阈值 |
| **右（D）** | 向量点积 | 与右向量点积 > 阈值 |

### 选择冷却

默认冷却时间为 500ms，防止快速连续选择。

### 选中效果

选中项会自动放大 0.5 倍，并播放选中音效。

## 故障排除

### WASD导航不工作
1. 检查 `wasd_config.yml` 中 `enabled` 是否为 `true`
2. 检查菜单配置中 `wasd-enabled` 是否为 `true`
3. 检查菜单是否在 `enabled-menus` 列表中

### PAPI变量不显示
1. 确保 PlaceholderAPI 已安装
2. 检查变量格式是否正确
3. 确认玩家当前在WASD菜单中

### 导航方向不正确
1. 调整 `horizontal-threshold` 和 `dot-product-threshold`
2. 检查菜单布局是否合理

## 更新日志

### v1.0.0
- 初始版本
- 从 CustomMenu 移植 WASD 导航功能
- 添加 PAPI 变量支持
- 模块化设计，独立于核心代码
