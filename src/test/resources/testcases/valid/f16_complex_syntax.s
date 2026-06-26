    .text

    .globl main
main:
    addi sp, sp, -80
    sw ra, 76(sp)
    sw s0, 72(sp)
    addi s0, sp, 80


entry_0:
    li t0, 10
    sw t0, -68(s0)
    li t0, 5
    sw t0, -72(s0)
    lw t0, -68(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -76(s0)
    li t0, 2
    neg t1, t0
    sw t1, -16(s0)
    lw t0, -72(s0)
    lw t1, -16(s0)
    mul t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -12(s0)
    lw t0, -68(s0)
    neg t1, t0
    sw t1, -28(s0)
    lw t0, -72(s0)
    neg t1, t0
    sw t1, -32(s0)
    lw t0, -28(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -24(s0)
    li t0, 0
    sw t0, -44(s0)
    li t0, 0
    sw t0, -40(s0)
    lw t0, -76(s0)
    li t1, 9
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -52(s0)
    lw t0, -52(s0)
    beq t0, zero, and_end_4
and_right_3:
    li t0, 10
    neg t1, t0
    sw t1, -48(s0)
    lw t0, -12(s0)
    lw t1, -48(s0)
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -60(s0)
    lw t0, -60(s0)
    beq t0, zero, and_end_4
    li t0, 1
    sw t0, -40(s0)
    j and_end_4
and_end_4:
    lw t0, -40(s0)
    beq t0, zero, and_end_2
and_right_1:
    li t0, 15
    neg t1, t0
    sw t1, -56(s0)
    lw t0, -24(s0)
    lw t1, -56(s0)
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -64(s0)
    lw t0, -64(s0)
    beq t0, zero, and_end_2
    li t0, 1
    sw t0, -44(s0)
    j and_end_2
and_end_2:
    lw t0, -44(s0)
    beq t0, zero, if_end_6
if_then_5:
    li a0, 0
    j main_epilogue
    j if_end_6
if_end_6:
    li a0, 1
    j main_epilogue
main_epilogue:
    lw s0, 72(sp)
    lw ra, 76(sp)
    addi sp, sp, 80
    ret

