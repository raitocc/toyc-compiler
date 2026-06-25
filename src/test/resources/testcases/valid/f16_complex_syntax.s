    .text

    .globl main
main:
    addi sp, sp, -80
    sw ra, 76(sp)
    sw s0, 72(sp)
    sw s1, 68(sp)
    sw s2, 64(sp)
    sw s3, 60(sp)
    sw s4, 56(sp)
    sw s5, 52(sp)
    sw s6, 48(sp)
    sw s7, 44(sp)
    sw s8, 40(sp)
    sw s9, 36(sp)
    sw s10, 32(sp)
    sw s11, 28(sp)
    addi s0, sp, 80


entry_0:
    li t0, 10
    mv s3, t0
    li t0, 5
    mv s4, t0
    li t1, 1
    sub t2, s3, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -76(s0)
    li t0, 2
    neg s6, t0
    mul s7, s4, s6
    mv s5, s7
    neg s9, s3
    neg s10, s4
    add s11, s9, s10
    mv s8, s11
    li t0, 0
    mv s2, t0
    li t0, 0
    mv s1, t0
    lw t0, -76(s0)
    li t1, 9
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -60(s0)
    lw t0, -60(s0)
    beq t0, zero, and_end_4
and_right_3:
    li t0, 10
    neg t1, t0
    sw t1, -56(s0)
    lw t1, -56(s0)
    sub t2, s5, t1
    seqz t2, t2
    sw t2, -68(s0)
    lw t0, -68(s0)
    beq t0, zero, and_end_4
    li t0, 1
    mv s1, t0
    j and_end_4
and_end_4:
    beq s1, zero, and_end_2
and_right_1:
    li t0, 15
    neg t1, t0
    sw t1, -64(s0)
    lw t1, -64(s0)
    sub t2, s8, t1
    seqz t2, t2
    sw t2, -72(s0)
    lw t0, -72(s0)
    beq t0, zero, and_end_2
    li t0, 1
    mv s2, t0
    j and_end_2
and_end_2:
    beq s2, zero, if_end_6
if_then_5:
    li a0, 0
    j main_epilogue
    j if_end_6
if_end_6:
    li a0, 1
    j main_epilogue
main_epilogue:
    lw s1, 68(sp)
    lw s2, 64(sp)
    lw s3, 60(sp)
    lw s4, 56(sp)
    lw s5, 52(sp)
    lw s6, 48(sp)
    lw s7, 44(sp)
    lw s8, 40(sp)
    lw s9, 36(sp)
    lw s10, 32(sp)
    lw s11, 28(sp)
    lw s0, 72(sp)
    lw ra, 76(sp)
    addi sp, sp, 80
    ret

