# ToyC 编译器实现说明与架构参考

这份文档是 ToyC 编译器的详细实现说明，涵盖了从输入源码到输出 RISC-V32 汇编的整个编译生命周期，包含自上而下的架构、调用链、文件关系、输入输出、接口以及使用说明。

---

## 一、 系统架构与文件关系

编译器被划分为前端（词法/语法解析）、中端（语义分析/IR 构建）和后端（寄存器分配与代码生成），每个部分都有明确的职责和对应的 Java 包：

### 1. `frontend` / 词法与语法分析
*   `src/main/antlr4/toyc/ToyC.g4`
    *   **职责**：定义完整的 ToyC 文法和词法规则，使用 ANTLR4 自动生成 Lexer 和 Parser。
*   `com.toyc.compiler.AstBuilder`
    *   **职责**：继承 `ToyCBaseVisitor`，将 ANTLR 的 `ParseTree` 转化为我们自定义的抽象语法树。
*   `com.toyc.compiler.ast.AST`
    *   **职责**：内部包含了所有的 AST 节点结构（如 `CompUnit`, `IfStmt`, `BinaryExpr`）以及用于遍历的 `Visitor` 接口。

### 2. `semantic` / 语义分析
*   `com.toyc.compiler.semantic.SymbolTable`
    *   **职责**：维护变量、常量和函数的符号信息。使用 `Stack<Map>` 实现嵌套的块级作用域。
*   `com.toyc.compiler.semantic.SemanticAnalyzer`
    *   **职责**：对 AST 进行自顶向下的遍历。完成变量重复定义检查、未声明检查。**特别地，它会在编译期计算常量表达式的值**，并将 `ConstDecl` 及其引用直接在 AST 中替换为 `NumberExpr` 以实现常数折叠。

### 3. `ir` / 中间代码生成
*   `com.toyc.compiler.ir.TAC`
    *   **职责**：定义线性三地址码（Three-Address Code）的结构，如 `ADD t0, t1, t2`、`BEQZ label, cond` 等。
*   `com.toyc.compiler.ir.IRBuilder`
    *   **职责**：将 AST 展平为 TAC 指令流，并管理控制流标签（Label）、短路求值的跳转逻辑，生成临时变量（Temp Virtual Registers）。

### 4. `optimize` / 代码优化
*   `com.toyc.compiler.optimize.Optimizer`
    *   **职责**：执行在 IR 层面的中间代码优化。目前保留架构占位符，因为基于 AST 的常量折叠已经在语义分析阶段完成。未来可拓展窥孔优化或死代码删除。

### 5. `backend` / 代码生成与寄存器分配
*   `com.toyc.compiler.backend.RiscvCodeGen`
    *   **职责**：接收优化后的 TAC 指令流，并针对 RISC-V32 ISA 输出实际的汇编文本。
    *   **特性**：内置了一个**基于栈的简化分配策略**，动态预扫描函数内需要的所有虚拟寄存器和局部变量，计算固定栈帧大小。对于指令，使用 `t0-t2` 等临时物理寄存器作为累加器，操作完立即溢出回栈，以绝对保证代码的正确性。

---

## 二、 调用链分析 (Call Chain)

当你执行 `Main.java` 时，数据流向如下：

1.  **输入接收**：`Main` 从 `System.in` 读取源代码并封装成 `CharStream`。
2.  **Lex & Parse**：
    `ToyCLexer` (词法) $\rightarrow$ `CommonTokenStream` $\rightarrow$ `ToyCParser.compUnit()` $\rightarrow$ `ParseTree`
3.  **AST 构建**：
    `AstBuilder.visit(ParseTree)` $\rightarrow$ `AST.Node` (树状结构)
4.  **语义检查与常数折叠**：
    `SemanticAnalyzer.analyze(AST)` $\rightarrow$ 重写后的 `AST.Node`
5.  **中间代码生成 (IR)**：
    `IRBuilder.build(AST)` $\rightarrow$ `List<TAC>` (平铺的指令列表)
6.  **优化器介入**：
    `Optimizer.optimize(List<TAC>)` $\rightarrow$ `List<TAC>`
7.  **机器码生成 (Backend)**：
    `RiscvCodeGen(System.out, List<TAC>).generate()` $\rightarrow$ 向控制台输出 `.data` 与 `.text` 汇编段。

---

## 三、 使用说明与接口规范

### 1. 构建项目
本项目使用 Maven 管理，请确保系统已安装 JDK 21+ 与 Maven 3.9+。
```bash
mvn clean package
```
执行完毕后，会在 `target/` 目录下生成一个包含所有依赖的 Fat Jar（`toyc-compiler-1.0-SNAPSHOT-jar-with-dependencies.jar`）。

### 2. 编译 ToyC 源文件
编译器严格按照任务要求，从标准输入读入源码，从标准输出打印汇编代码：
```bash
# Windows (PowerShell) 下运行：
Get-Content input.tc | java -jar target/toyc-compiler-1.0-SNAPSHOT-jar-with-dependencies.jar > output.s

# Linux / Git Bash 下运行：
java -jar target/toyc-compiler-1.0-SNAPSHOT-jar-with-dependencies.jar < input.tc > output.s
```

### 3. 开启性能优化
如果评测平台传入了 `-opt` 标志，在运行时追加该参数即可触发 Optimizer 流程：
```bash
java -jar target/toyc-compiler-1.0-SNAPSHOT-jar-with-dependencies.jar -opt < input.tc > output.s
```

### 4. 验证汇编正确性 (需要交叉编译工具链)
要测试汇编代码是否能跑出正确的退出码，需要依赖 RISC-V 交叉编译环境，执行以下流程：
```bash
# 1. 将汇编编译成可执行文件
riscv64-unknown-elf-gcc -march=rv32im -mabi=ilp32 output.s -o a.out

# 2. 在 QEMU 模拟器中执行
qemu-riscv32 a.out

# 3. 打印退出码 (Linux / Bash)
echo $?
```
