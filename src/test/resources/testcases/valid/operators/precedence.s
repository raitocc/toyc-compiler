    .text

    .globl main
main:
    addi sp, sp, -96
    sw ra, 92(sp)
    sw s0, 88(sp)
    addi s0, sp, 96


entry_0:
    li t0, 3
    li t1, 4
    mul t2, t0, t1
    sw t2, -76(s0)
    li t0, 2
    lw t1, -76(s0)
    add t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -72(s0)
    li t0, 2
    li t1, 3
    add t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    li t1, 4
    mul t2, t0, t1
    sw t2, -16(s0)
    lw t0, -16(s0)
    sw t0, -84(s0)
    li t0, 10
    li t1, 4
    sub t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    li t1, 2
    sub t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -20(s0)
    li t0, 10
    li t1, 2
    div t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    li t1, 5
    mul t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    sw t0, -32(s0)
    li t0, 0
    sw t0, -40(s0)
    li t0, 0
    sw t0, -52(s0)
    li t0, 0
    sw t0, -48(s0)
    lw t0, -72(s0)
    li t1, 14
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -60(s0)
    lw t0, -60(s0)
    beq t0, zero, and_end_6
and_right_5:
    lw t0, -84(s0)
    li t1, 20
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -56(s0)
    lw t0, -56(s0)
    beq t0, zero, and_end_6
    li t0, 1
    sw t0, -48(s0)
    j and_end_6
and_end_6:
    lw t0, -48(s0)
    beq t0, zero, and_end_4
and_right_3:
    lw t0, -20(s0)
    li t1, 4
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -68(s0)
    lw t0, -68(s0)
    beq t0, zero, and_end_4
    li t0, 1
    sw t0, -52(s0)
    j and_end_4
and_end_4:
    lw t0, -52(s0)
    beq t0, zero, and_end_2
and_right_1:
    lw t0, -32(s0)
    li t1, 25
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -64(s0)
    lw t0, -64(s0)
    beq t0, zero, and_end_2
    li t0, 1
    sw t0, -40(s0)
    j and_end_2
and_end_2:
    lw t0, -40(s0)
    beq t0, zero, if_end_8
if_then_7:
    li a0, 1
    j main_epilogue
    j if_end_8
if_end_8:
    li a0, 0
    j main_epilogue
main_epilogue:
    lw s0, 88(sp)
    lw ra, 92(sp)
    addi sp, sp, 96
    ret

