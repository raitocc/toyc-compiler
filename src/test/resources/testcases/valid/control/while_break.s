    .text

    .globl main
main:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    sw s1, 36(sp)
    sw s2, 32(sp)
    sw s3, 28(sp)
    sw s4, 24(sp)
    sw s5, 20(sp)
    sw s6, 16(sp)
    sw s7, 12(sp)
    sw s8, 8(sp)
    addi s0, sp, 48


entry_0:
    li t0, 0
    mv s2, t0
    li t0, 0
    mv s1, t0
while_cond_1:
    li t1, 10
    slt s7, s1, t1
    beq s7, zero, while_end_3
while_body_2:
    li t1, 1
    add s8, s1, t1
    mv s1, s8
    li t1, 2
    rem s3, s1, t1
    li t1, 0
    sub s4, s3, t1
    seqz s4, s4
    beq s4, zero, if_end_5
if_then_4:
    j while_cond_1
    j if_end_5
if_end_5:
    add s5, s2, s1
    mv s2, s5
    li t1, 20
    slt s6, t1, s2
    beq s6, zero, if_end_7
if_then_6:
    j while_end_3
    j if_end_7
if_end_7:
    j while_cond_1
while_end_3:
    mv a0, s2
    j main_epilogue
main_epilogue:
    lw s1, 36(sp)
    lw s2, 32(sp)
    lw s3, 28(sp)
    lw s4, 24(sp)
    lw s5, 20(sp)
    lw s6, 16(sp)
    lw s7, 12(sp)
    lw s8, 8(sp)
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

