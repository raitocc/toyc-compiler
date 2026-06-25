    .text

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
    sw s2, 0(sp)
    addi s0, sp, 16


entry_0:
    li t0, 10
    mv s1, t0
    li t1, 5
    slt s2, t1, s1
    beq s2, zero, if_else_3
if_then_1:
    li t0, 2
    mv s1, t0
    j if_end_2
if_else_3:
    li t0, 1
    mv s1, t0
    j if_end_2
if_end_2:
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s2, 0(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

