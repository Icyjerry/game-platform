# Lab 5：游戏大厅多模式集成与国际象棋实现

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
- Chess（国际象棋）✨ 新增

## 核心功能
- ✅ 多对局并行运行，可实时切换
- ✅ 运行时动态新增游戏
- ✅ Demo 自动演示模式
- ✅ 两种 UI 支持（JavaFX + Lanterna）
- ✅ 插件化架构

## 项目结构
```
src/main/java/reversi/
├── core/              # 核心游戏逻辑
│   ├── ChessGame      # ✨ 新增：国际象棋
│   ├── PeaceGame
│   ├── ReversiGame
│   └── MinesweeperGame
├── games/             # 游戏插件
│   ├── chess/
│   ├── peace/
│   ├── reversi/
│   └── minesweeper/
├── gamehall/          # 游戏大厅
│   ├── GameRegistry   # 插件注册表
│   └── MultiGameManager # 多对局管理
├── ui/                # 用户界面
│   ├── JavaFxUi       # ✨ 新增
│   └── LanternaUi
└── command/           # 命令解析
```

## 详见实验报告
参考 `实验报告.md`
