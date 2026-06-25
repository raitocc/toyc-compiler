# ToyC 编译器开发交接文档 (HANDOFF)

本文档总结了当前 ToyC 编译器项目前端（词法分析、语法分析、AST 构建、语义检查）的开发进度与后续中端（IR 生成）的规化与设计，以供后续接手的 AI 助手或开发人员无缝衔接。

---

## 1. 已完成的工作（做了什么）

目前我们已经在本地及远程的 `ast-only` 分支上完成了编译器**前端的全部核心功能**，并进行了重构优化。所有单元测试（共 34 个）已全部通过。

* **测试用例整理**：
  * 对 `src/test/resources/testcases/` 下的用例进行了归类整理：
    * **合法用例 (valid)** 细分为 `basic/`, `control/`, `functions/`, `logical/`；
    * **非法用例 (invalid)** 细分为 `control/`, `functions/`, `structure/`, `syntax/`, `types/`, `variables/`（包括新增的 `func_missing_block.tc` 等语法错用例）。
* **位置信息跟踪**：
  * 在 [AST.java](file:///D:/08Projects/2026/toyc-compiler/src/main/java/com/toyc/compiler/ast/AST.java) 的 `AST.Node` 基类中引入了 `line`（行号，1-based）和 `column`（列号，0-based）字段。
  * 在 [AstBuilder.java](file:///D:/08Projects/2026/toyc-compiler/src/main/java/com/toyc/compiler/AstBuilder.java) 中重写了 `visit(ParseTree tree)` 方法，在构造任意 AST 节点时，自动提取其在 ANTLR `ParserRuleContext` 中起始 Token 的行列位置并注入，免去了修改各个具体节点构造函数的繁琐操作。
* **优雅的语义分析器**：
  * 在 [SemanticAnalyzer.java](file:///D:/08Projects/2026/toyc-compiler/src/main/java/com/toyc/compiler/semantic/SemanticAnalyzer.java) 中实现位置敏感的语义错误报告。
  * 移除了冗余的 `try-catch` 捕获和报错信息的字符串替换逻辑，改用在符号表定义/查询前的显式逻辑判断（如 `hasInCurrentScope` 和 `lookup == null`）。
  * 实现了对 `main` 函数签名校验、常量求值与变量遮蔽、控制流上下文中 `break`/`continue` 的层级限制、类型安全（禁止 `void` 表达式右值等）、多执行路径 return 检查等功能。
* **主入口更新与命令行支持**：
  * 更新了 [Main.java](file:///D:/08Projects/2026/toyc-compiler/src/main/java/com/toyc/compiler/Main.java)，在语义检查成功时通过 [AstPrinter](file:///D:/08Projects/2026/toyc-compiler/src/main/java/com/toyc/compiler/ast/AstPrinter.java) 向 `stdout` 打印 AST 文本；在发生错误时向 `stderr` 打印行号/列号定位的错误信息并以状态码 `1` 退出，支持标准的终端重定向。
* **Git 状态**：
  * 所有改动已全部提交并推送至 GitHub 远程分支 `ast-only`（最新 Commit `eab50f0`）。

---

## 2. 正在规化与想做的工作（想做什么）

我们接下来需要从编译器前端过渡到中端，主要任务是**中间表示 (Intermediate Representation / IR)** 的设计。

* **中间表示的选择**：
  * 采用**三地址码 (Three-Address Code / TAC)** 作为编译器的内部中间表示。
  * 设计一套精简的 TAC 指令集，能够完整表达 ToyC 语言的各项特性（如临时变量赋值、一元/二元算术与逻辑运算、无条件跳转 `goto`、条件跳转 `if ... goto`、参数传递 `param`、函数调用 `call` 与返回 `return` ）。
* **控制流与短路求值翻译设计**：
  * 仔细规划 `if-else` 分支与 `while` 循环等控制流语句在三地址码下的标签生成规则。
  * 特别是对逻辑与 `&&`、逻辑或 `||` 运算符，需要映射为基于条件跳转的“短路求值” TAC 控制流，而非简单的算术运算。
* **全局变量/常量的 TAC 表达**：
  * 对本届实验新增的全局变量和常量，在 TAC 阶段需要规划其与局部符号的区分以及在代码生成时的布局表达。

---

## 3. 下一步要做的工作（下面做什么）

接下来的 Session 应按照以下步骤推进：

1. **定义 TAC 的核心数据结构**：
   * 在项目中创建相应的包（例如 `com.toyc.compiler.ir`）。
   * 设计 `Instruction` 抽象类及其具体实现子类（如 `Assign`, `BinOp`, `Branch`, `Jump`, `Label`, `Param`, `Call`, `Return` 等）。
   * 设计操作数表示 `Operand` 抽象类（包括 `Constant` 整数常量、`Variable` 变量、`Temporary` 临时变量等）。
2. **实现 IR 生成器 (IRBuilder / TACGenerator)**：
   * 实现一个继承 `AST.Visitor<Operand>` 的访问器类，深度优先遍历 AST 并生成 TAC 指令序列。
   * 为每个作用域管理临时变量计数器（用于生成 `t0`, `t1`... 临时寄存器符号）。
3. **增加集成测试**：
   * 在 `AstTest.java` 之后扩展测试类，用于对生成的 TAC 序列进行正确性分析，确保生成的控制流拓扑正确。

---

## 4. 下一次建议使用的技能/工具

* **[diagnose](file:///C:/Users/czh/.gemini/config/skills/diagnose/SKILL.md)**：中端 IR 构建时极易出现标签生成错乱、临时变量生存期计算错误等隐蔽 Bug。强烈建议在遇到复杂控制流错误时启用此调试循环。
* **[tdd](file:///C:/Users/czh/.gemini/config/skills/tdd/SKILL.md)**：采用测试驱动开发。先针对一元运算、二元运算、单层 If、嵌套 If、While 循环、短路求值分别写出简单的测试期望，然后逐步完善 `IRBuilder` 的实现。

---
交接完毕。项目目前的最新开发分支为 **`ast-only`**。可通过以下命令进行构建与验证：
```powershell
mvn clean test
```
