# ToyC Compiler

ToyC 是一款从零构建的、目标架构为 **RISC-V RV32IM** 的 C 语言子集编译器。本项目实现了从源码解析到汇编生成的完整 5 阶段编译管线，具备完善的类型检查、多级作用域控制、IR 中间代码级优化以及自动化端到端测试套件。

---

## 1. 项目概览

本项目旨在实现从源码到 RISC-V 汇编的完整编译流程。核心实现成果包括：
- 完整的 5 阶段编译管线（词法/语法分析、语义分析、IR 生成、IR 优化、目标代码生成）。
- 达到 100% 自动化测试通过率与 100% 核心逻辑分支覆盖率。
- 引入了基于定点迭代的常量折叠、复写传播、死代码消除等多级代码优化机制。
- 实现了基于调用者/被调用者保存约定的贪心寄存器分配算法。

---

## 2. 编译器架构设计与代码导读

编译流程为严格的单向流水线，入口位于 `src/main/java/com/toyc/compiler/Main.java`。核心架构分为五个模块：

### 阶段一：词法与语法分析 (Frontend)
将 C 语言源码转化为自定义的抽象语法树 (AST)。
* **`src/main/antlr4/toyc/ToyC.g4`**：ANTLR4 语法文件。定义了词法规则（Token）和语法规则（Parser）。Maven 依据此文件自动生成 Lexer 和 Parser。
* **`com.toyc.compiler.ast.AST`**：定义了抽象语法树的所有节点结构（如 `Program`, `FuncDef`, `IfStmt`, `BinaryExpr` 等），并声明了 `Visitor` 接口。后续的语义分析与 IR 生成均基于此 `Visitor` 模式实现。
* **`com.toyc.compiler.AstBuilder`**：负责将 ANTLR4 生成的 Parse Tree 转化为自定义的 `AST.Node` 结构。

### 阶段二：语义分析 (Semantic Analysis)
负责拦截不符合 C 语言语义的非法代码（如未声明先使用、变量重复定义、类型不匹配、参数数量不符等）。
* **`com.toyc.compiler.semantic.SemanticAnalyzer`**：语义分析器核心实现。基于深度优先搜索 (DFS) 遍历 AST，提前返回并抛出携带行列号的语义错误异常（`SemanticError`）。
* **`com.toyc.compiler.semantic.SymbolTable` 与 `Symbol`**：通过栈式哈希表（`List<Map<String, Symbol>>`）实现多级块级作用域（Block Scope）的符号表管理，支持局部变量对同名全局/外部变量的遮蔽（Shadowing）。

### 阶段三：中间代码生成 (IR Generation)
将树状 AST 展平，生成线性、与寄存器无关的三地址码 (Three-Address Code, IR)。
* **`com.toyc.compiler.ir.IR`**：定义 IR 指令体系（`MOV`, `ADD`, `BRANCH`, `CALL`, `RET` 等）。每条指令最多包含两个源操作数和一个目的操作数。
* **`com.toyc.compiler.ir.IrGenerator`**：遍历 AST 并发射 (Emit) IR 指令，将复合表达式拆解为采用临时变量（如 `%t1`）的线性指令流。

### 阶段四：中间代码优化 (IR Optimization)
在架构无关层面对 IR 进行指令精简。
* **`com.toyc.compiler.ir.IrOptimizer`**：基于定点迭代 (Fixed-point Iteration) 算法的优化器，实现了以下三项优化：
  1. **常量折叠 (Constant Folding)**：将算术结果在编译期求值计算。
  2. **复写传播 (Copy Propagation)**：追踪并替换临时变量，消除无用的赋值传递。
  3. **死代码消除 (Dead Code Elimination)**：移除计算后未被使用的多余指令。

### 阶段五：目标代码生成 (Backend)
将 IR 转化为能在实际设备或 QEMU 模拟器上执行的 RISC-V RV32IM 汇编。
* **`com.toyc.compiler.backend.RiscvGenerator`**：后端核心生成器。
  * **寄存器分配**：实现基于被调用者保存寄存器（`s1-s11`）的贪心映射算法。当寄存器耗尽时，触发内存溢出 (Spill) 操作。
  * **栈帧管理 (Stack Frame)**：在函数入口 (Prologue) 分配栈空间并保存 `ra` 与 `s0`，在函数出口 (Epilogue) 恢复寄存器状态并执行 `ret` 指令。

---

## 3. 支持的语法特性

本项目支持以下 C 语言核心特性集：
- **数据类型**：全局与局部有符号 32 位整数 (`int`) 及常量 (`const int`)。
- **算术与关系运算**：`+`, `-`, `*`, `/`, `%`，及 `<`, `>`, `<=`, `>=`, `==`, `!=`。
- **逻辑短路机制**：完全实现 `&&` 与 `||` 的短路跳转执行。
- **控制流**：支持深层嵌套的 `if-else` 分支与 `while` 循环（含 `break` 和 `continue`）。
- **函数调用**：支持多参数传递（借助寄存器与栈区溢出机制）及递归调用。

---

## 4. 开发与构建指引

### 环境要求
- **Java**: JDK 17 及以上版本。
- **构建工具**: Maven 3.6+。
- **测试环境**: 包含 `riscv64-unknown-elf-gcc` 工具链与 `qemu-riscv32` 模拟器的 Unix/WSL 环境。

### 构建与编译
```bash
# 触发 ANTLR4 自动代码生成并编译主项目
mvn clean compile

# 打包为可执行的 JAR 文件
mvn package
```

### 运行机制
通过命令行执行主程序：
```bash
# -opt 参数用于开启 IR 优化器
java -cp target/classes com.toyc.compiler.Main -opt < source_file.tc > output_file.s
```

---

## 5. 测试与基准数据

本项目实现了严格的单元测试与端到端回归验证，共计 175 个测试样例。

### 自动化测试套件
项目实现了无人值守回归测试机制，测试文件均位于 `src/test/resources/testcases/`。执行命令：
```bash
mvn test -Dregen=true
```
测试流程通过 Java `ProcessBuilder` 自动调用系统 `riscv64-unknown-elf-gcc` 进行汇编链接，并使用 `qemu-riscv32` 模拟执行以比对预期退出码。

### 性能基准测试 (Benchmark)
位于 `testcases/perf/` 目录下的高压力数学算法测试，包含：
1. **阿克曼函数 (`perf_ackermann.tc`)**：评估海量栈帧创建与深层递归下 Prologue/Epilogue 机制的稳定性。
2. **欧几里得算法 (`perf_gcd.tc`)**：高频次循环迭代与取模运算评估。
3. **斐波那契数列与圆周率级数**：大规模整数算术组合与分支跳转测试。

---

## 6. 开发规范与已知限制

### 提交与测试规范
- **分支覆盖要求**：所有对 `SemanticAnalyzer` 或 `RiscvGenerator` 的功能修改，必须配套相应的 `valid/invalid` 源码测试样例，并确保 `mvn test` 整体通过。
- **语法文件限制**：禁止手动修改 `src/main/generated/` 目录内的自动生成代码。所有语法级变动均需在 `ToyC.g4` 中进行定义并依靠 Maven 重新生成。

### 当前架构局限性
- 暂未实现对数组 (Array) 和指针 (Pointer) 数据类型的支持。
- 函数定义强制要求“先声明后调用”，不支持函数的互相间接递归调用 (Forward Declaration)。
- 后端寄存器分配采用简单的贪心映射策略，暂未引入基于控制流图 (CFG) 和活跃变量分析 (LVA) 的图着色算法。
