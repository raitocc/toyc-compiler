    .text

    .globl main
main:
    addi sp, sp, -80
    sw ra, 76(sp)
    sw s0, 72(sp)
    addi s0, sp, 80


entry_0:
    li t0, 1
    sw t0, -56(s0)
    li t0, 0
    sw t0, -60(s0)
    li t0, 1
    sw t0, -64(s0)
    li t0, 0
    sw t0, -68(s0)
    li t0, 0
    sw t0, -12(s0)
    lw t0, -56(s0)
    li t1, 0
    slt t2, t1, t0
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, and_end_2
and_right_1:
    li t0, 1
    sw t0, -20(s0)
    lw t0, -60(s0)
    li t1, 0
    slt t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    bne t0, zero, or_end_4
or_right_3:
    lw t0, -64(s0)
    li t1, 1
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -28(s0)
    lw t0, -28(s0)
    bne t0, zero, or_end_4
    li t0, 0
    sw t0, -20(s0)
    j or_end_4
or_end_4:
    lw t0, -20(s0)
    beq t0, zero, and_end_2
    li t0, 1
    sw t0, -12(s0)
    j and_end_2
and_end_2:
    lw t0, -12(s0)
    beq t0, zero, if_end_6
if_then_5:
    li t0, 1
    sw t0, -68(s0)
    j if_end_6
if_end_6:
    li t0, 0
    sw t0, -32(s0)
    li t0, 1
    sw t0, -36(s0)
    lw t0, -56(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -44(s0)
    lw t0, -44(s0)
    bne t0, zero, or_end_10
or_right_9:
    lw t0, -60(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -40(s0)
    lw t0, -40(s0)
    bne t0, zero, or_end_10
    li t0, 0
    sw t0, -36(s0)
    j or_end_10
or_end_10:
    lw t0, -36(s0)
    beq t0, zero, and_end_8
and_right_7:
    lw t0, -64(s0)
    li t1, 1
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -52(s0)
    lw t0, -52(s0)
    beq t0, zero, and_end_8
    li t0, 1
    sw t0, -32(s0)
    j and_end_8
and_end_8:
    lw t0, -32(s0)
    beq t0, zero, if_end_12
if_then_11:
    lw t0, -68(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    sw t0, -68(s0)
    j if_end_12
if_end_12:
    lw a0, -68(s0)
    j main_epilogue
main_epilogue:
    lw s0, 72(sp)
    lw ra, 76(sp)
    addi sp, sp, 80
    ret

