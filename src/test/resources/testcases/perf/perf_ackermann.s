    .text

    .globl ack
ack:
    addi sp, sp, -64
    sw ra, 60(sp)
    sw s0, 56(sp)
    sw s1, 52(sp)
    sw s2, 48(sp)
    sw s3, 44(sp)
    sw s4, 40(sp)
    sw s5, 36(sp)
    sw s6, 32(sp)
    sw s7, 28(sp)
    sw s8, 24(sp)
    sw s9, 20(sp)
    sw s10, 16(sp)
    sw s11, 12(sp)
    addi s0, sp, 64

    mv s1, a0
    mv s2, a1

entry_0:
    li t1, 0
    sub t2, s1, t1
    seqz t2, t2
    sw t2, -56(s0)
    lw t0, -56(s0)
    beq t0, zero, if_end_2
if_then_1:
    li t1, 1
    add t2, s2, t1
    sw t2, -60(s0)
    lw a0, -60(s0)
    j ack_epilogue
    j if_end_2
if_end_2:
    li t0, 0
    mv s3, t0
    li t1, 0
    slt s4, t1, s1
    beq s4, zero, and_end_4
and_right_3:
    li t1, 0
    sub s5, s2, t1
    seqz s5, s5
    beq s5, zero, and_end_4
    li t0, 1
    mv s3, t0
    j and_end_4
and_end_4:
    beq s3, zero, if_end_6
if_then_5:
    li t1, 1
    sub s6, s1, t1
    mv a0, s6
    li t0, 1
    mv a1, t0
    call ack
    mv s7, a0
    mv a0, s7
    j ack_epilogue
    j if_end_6
if_end_6:
    li t1, 1
    sub s8, s1, t1
    mv a0, s8
    mv a1, s1
    li t1, 1
    sub s9, s2, t1
    mv a2, s9
    call ack
    mv s11, a0
    mv a0, s11
    call ack
    mv s10, a0
    mv a0, s10
    j ack_epilogue
ack_epilogue:
    lw s1, 52(sp)
    lw s2, 48(sp)
    lw s3, 44(sp)
    lw s4, 40(sp)
    lw s5, 36(sp)
    lw s6, 32(sp)
    lw s7, 28(sp)
    lw s8, 24(sp)
    lw s9, 20(sp)
    lw s10, 16(sp)
    lw s11, 12(sp)
    lw s0, 56(sp)
    lw ra, 60(sp)
    addi sp, sp, 64
    ret

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
    addi s0, sp, 16


entry_7:
    li t0, 3
    mv a0, t0
    li t0, 6
    mv a1, t0
    call ack
    mv s1, a0
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

