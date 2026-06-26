    .text

    .globl main
main:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48


entry_0:
    li t0, 0
    sw t0, -28(s0)
    li t0, 0
    sw t0, -32(s0)
while_cond_1:
    lw t0, -32(s0)
    li t1, 10
    slt t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -32(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    sw t0, -32(s0)
    lw t0, -32(s0)
    li t1, 2
    rem t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, if_end_5
if_then_4:
    j while_cond_1
    j if_end_5
if_end_5:
    lw t0, -28(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -28(s0)
    lw t0, -28(s0)
    li t1, 20
    slt t2, t1, t0
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, if_end_7
if_then_6:
    j while_end_3
    j if_end_7
if_end_7:
    j while_cond_1
while_end_3:
    lw a0, -28(s0)
    j main_epilogue
main_epilogue:
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

