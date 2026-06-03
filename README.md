# Lab 6：游戏大厅 JavaFX GUI

## 快速开始

### 编译
```bash
./mvnw compile
```

### 运行（JavaFX GUI）
```bash
./mvnw javafx:run
```

### 运行（Lanterna TUI）
```bash
./mvnw exec:java -Dexec.mainClass=reversi.App -Dexec.args="--ui lanterna"
```

### 运行测试
```bash
./mvnw test
```

## 包含模式
- Peace（和平棋）
- Reversi（黑白棋）
- Minesweeper（扫雷）
- Chess（国际象棋）

## 核心功能
- ✅ JavaFX 左中右三栏 GUI：左侧控制与状态，中间棋盘，右侧游戏列表
- ✅ 全鼠标交互：棋盘点击、游戏列表点击切换、按钮控制
- ✅ 新建当前对局：`New Game` 重置当前选中游戏槽位
- ✅ 扫雷左键翻开、右键插旗
- ✅ 国际象棋先点击棋子，再点击目标格
- ✅ Reversi 无合法步时通过 `Pass` 按钮跳过
- ✅ `Demo` 可直接运行，`Stop Demo` 返回手动模式，`Quit` 安全退出
- ✅ 保留 Lanterna TUI 入口用于架构对比

## JavaFX 操作

- 右侧 `GAME LIST`：鼠标左键单击切换不同对局，切换后保留各局状态。
- 左侧 `New Game`：重置当前选中的对局，不改变游戏列表槽位。
- 左侧 `Add Game`：新增指定模式对局，并自动切换到新对局。
- Peace / Reversi：鼠标左键点击棋盘空格落子。
- Minesweeper：鼠标左键翻开格子，鼠标右键切换旗标。
- Chess：鼠标左键先选中己方棋子，再点击目标格完成移动；兵升变默认升后。

## 项目结构
```
src/main/java/reversi/
├── core/              # 通用会话接口与棋盘能力接口
│   └── model/         # Board / Disc / GameMode / Position 等值对象
├── games/             # 各游戏实现与插件
│   ├── chess/
│   ├── peace/
│   ├── reversi/
│   └── minesweeper/
├── gamehall/          # 游戏大厅
│   ├── GameController # 统一命令分发与状态文案
│   ├── GameRegistry   # 游戏插件注册表
│   └── MultiGameManager # 多对局管理
├── ui/                # 用户界面
│   ├── JavaFxUi
│   └── LanternaUi
└── command/           # 命令解析
```

## 详见实验报告
参考 `实验报告_lab6.md`
