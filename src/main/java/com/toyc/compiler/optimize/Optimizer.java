package com.toyc.compiler.optimize;

import com.toyc.compiler.ir.TAC;
import java.util.List;

public class Optimizer {
    public List<TAC> optimize(List<TAC> instructions) {
        // SemanticAnalyzer already does Constant Folding and Propagation for constants.
        // Future improvements:
        // 1. Peephole optimization
        // 2. Dead code elimination (e.g., instructions after an unconditional JMP or RET in the same block)
        // 3. Common Subexpression Elimination (CSE)
        return instructions;
    }
}
