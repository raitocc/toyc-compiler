    .text

    .globl main
main:
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


entry_0:
    li t0, 0
    mv s1, t0
    li t0, 1
    mv s2, t0
    li t0, 1
    mv s3, t0
    li t0, 0
    mv s4, t0
    li t0, 100000
    mv s6, t0
while_cond_1:
    li t1, 100000
    slt s7, s4, t1
    beq s7, zero, while_end_3
while_body_2:
    li t1, 4
    mul s8, s6, t1
    div s9, s8, s3
    mv s5, s9
    li t1, 1
    sub s10, s2, t1
    seqz s10, s10
    beq s10, zero, if_else_6
if_then_4:
    add s11, s1, s5
    mv s1, s11
    li t0, 0
    mv s2, t0
    j if_end_5
if_else_6:
    sub t2, s1, s5
    sw t2, -60(s0)
    lw t0, -60(s0)
    mv s1, t0
    li t0, 1
    mv s2, t0
    j if_end_5
if_end_5:
    li t1, 2
    add t2, s3, t1
    sw t2, -56(s0)
    lw t0, -56(s0)
    mv s3, t0
    li t1, 1
    add t2, s4, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    mv s4, t0
    j while_cond_1
while_end_3:
    mv a0, s1
    j main_epilogue
main_epilogue:
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

