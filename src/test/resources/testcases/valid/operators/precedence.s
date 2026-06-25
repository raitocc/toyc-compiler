    .text

    .globl main
main:
    addi sp, sp, -96
    sw ra, 92(sp)
    sw s0, 88(sp)
    sw s1, 84(sp)
    sw s2, 80(sp)
    sw s3, 76(sp)
    sw s4, 72(sp)
    sw s5, 68(sp)
    sw s6, 64(sp)
    sw s7, 60(sp)
    sw s8, 56(sp)
    sw s9, 52(sp)
    sw s10, 48(sp)
    sw s11, 44(sp)
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
    add s4, t0, t1
    li t1, 4
    mul s5, s4, t1
    sw s5, -84(s0)
    li t0, 10
    li t1, 4
    sub s7, t0, t1
    li t1, 2
    sub s8, s7, t1
    mv s6, s8
    li t0, 10
    li t1, 2
    div s10, t0, t1
    li t1, 5
    mul s11, s10, t1
    mv s9, s11
    li t0, 0
    mv s1, t0
    li t0, 0
    mv s3, t0
    li t0, 0
    mv s2, t0
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
    mv s2, t0
    j and_end_6
and_end_6:
    beq s2, zero, and_end_4
and_right_3:
    li t1, 4
    sub t2, s6, t1
    seqz t2, t2
    sw t2, -68(s0)
    lw t0, -68(s0)
    beq t0, zero, and_end_4
    li t0, 1
    mv s3, t0
    j and_end_4
and_end_4:
    beq s3, zero, and_end_2
and_right_1:
    li t1, 25
    sub t2, s9, t1
    seqz t2, t2
    sw t2, -64(s0)
    lw t0, -64(s0)
    beq t0, zero, and_end_2
    li t0, 1
    mv s1, t0
    j and_end_2
and_end_2:
    beq s1, zero, if_end_8
if_then_7:
    li a0, 1
    j main_epilogue
    j if_end_8
if_end_8:
    li a0, 0
    j main_epilogue
main_epilogue:
    lw s1, 84(sp)
    lw s2, 80(sp)
    lw s3, 76(sp)
    lw s4, 72(sp)
    lw s5, 68(sp)
    lw s6, 64(sp)
    lw s7, 60(sp)
    lw s8, 56(sp)
    lw s9, 52(sp)
    lw s10, 48(sp)
    lw s11, 44(sp)
    lw s0, 88(sp)
    lw ra, 92(sp)
    addi sp, sp, 96
    ret

