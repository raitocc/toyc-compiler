    .text

    .globl main
main:
    addi sp, sp, -64
    sw ra, 60(sp)
    sw s0, 56(sp)
    addi s0, sp, 64


entry_0:
    li t0, 0
    sw t0, -52(s0)
    li t0, 1
    sw t0, -56(s0)
    li t0, 1
    sw t0, -60(s0)
    li t0, 0
    sw t0, -64(s0)
    li t0, 100000
    sw t0, -12(s0)
while_cond_1:
    lw t0, -64(s0)
    li t1, 100000
    slt t2, t0, t1
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -12(s0)
    li t1, 4
    mul t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    lw t1, -60(s0)
    div t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -20(s0)
    lw t0, -56(s0)
    li t1, 1
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -32(s0)
    lw t0, -32(s0)
    beq t0, zero, if_else_6
if_then_4:
    lw t0, -52(s0)
    lw t1, -20(s0)
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -52(s0)
    li t0, 0
    sw t0, -56(s0)
    j if_end_5
if_else_6:
    lw t0, -52(s0)
    lw t1, -20(s0)
    sub t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    sw t0, -52(s0)
    li t0, 1
    sw t0, -56(s0)
    j if_end_5
if_end_5:
    lw t0, -60(s0)
    li t1, 2
    add t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    sw t0, -60(s0)
    lw t0, -64(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    sw t0, -64(s0)
    j while_cond_1
while_end_3:
    lw a0, -52(s0)
    j main_epilogue
main_epilogue:
    lw s0, 56(sp)
    lw ra, 60(sp)
    addi sp, sp, 64
    ret

