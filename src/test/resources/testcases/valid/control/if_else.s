    .text

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    addi s0, sp, 16

entry_0:
    li t0, 10
    sw t0, -4(s0)
    lw t0, -4(s0)
    li t1, 5
    slt t2, t1, t0
    sw t2, -8(s0)
    lw t0, -8(s0)
    beq t0, zero, if_else_3
if_then_1:
    li t0, 2
    j if_end_2
if_else_3:
    li t0, 1
    j if_end_2
if_end_2:
    lw a0, -4(s0)
    j main_epilogue
main_epilogue:
    lw ra, 12(sp)
    lw s0, 8(sp)
    addi sp, sp, 16
    ret

