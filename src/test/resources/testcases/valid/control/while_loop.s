    .text

    .globl main
main:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32

entry_0:
    li t0, 0
    sw t0, -4(s0)
    li t0, 0
    sw t0, -8(s0)
while_cond_1:
    lw t0, -8(s0)
    li t1, 5
    slt t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -4(s0)
    lw t1, -8(s0)
    add t2, t0, t1
    sw t2, -16(s0)
    lw t0, -16(s0)
    lw t0, -8(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    j while_cond_1
while_end_3:
    j main_epilogue
main_epilogue:
    lw ra, 28(sp)
    lw s0, 24(sp)
    addi sp, sp, 32
    ret

