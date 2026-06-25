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
    li t0, 1
    mv s6, t0
    li t0, 0
    mv s7, t0
    li t0, 1
    mv s8, t0
    li t0, 0
    mv s1, t0
    li t0, 0
    mv s2, t0
    li t1, 0
    slt s9, t1, s6
    beq s9, zero, and_end_2
and_right_1:
    li t0, 1
    mv s3, t0
    li t1, 0
    slt s10, s7, t1
    bne s10, zero, or_end_4
or_right_3:
    li t1, 1
    sub s11, s8, t1
    seqz s11, s11
    bne s11, zero, or_end_4
    li t0, 0
    mv s3, t0
    j or_end_4
or_end_4:
    beq s3, zero, and_end_2
    li t0, 1
    mv s2, t0
    j and_end_2
and_end_2:
    beq s2, zero, if_end_6
if_then_5:
    li t0, 1
    mv s1, t0
    j if_end_6
if_end_6:
    li t0, 0
    mv s4, t0
    li t0, 1
    mv s5, t0
    li t1, 0
    sub t2, s6, t1
    seqz t2, t2
    sw t2, -60(s0)
    lw t0, -60(s0)
    bne t0, zero, or_end_10
or_right_9:
    li t1, 0
    sub t2, s7, t1
    seqz t2, t2
    sw t2, -56(s0)
    lw t0, -56(s0)
    bne t0, zero, or_end_10
    li t0, 0
    mv s5, t0
    j or_end_10
or_end_10:
    beq s5, zero, and_end_8
and_right_7:
    li t1, 1
    sub t2, s8, t1
    seqz t2, t2
    sw t2, -68(s0)
    lw t0, -68(s0)
    beq t0, zero, and_end_8
    li t0, 1
    mv s4, t0
    j and_end_8
and_end_8:
    beq s4, zero, if_end_12
if_then_11:
    li t1, 1
    add t2, s1, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    mv s1, t0
    j if_end_12
if_end_12:
    mv a0, s1
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

