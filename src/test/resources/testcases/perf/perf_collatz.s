    .text

    .globl collatz_length
collatz_length:
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
    sw s9, 4(sp)
    addi s0, sp, 48

    mv s1, a0

entry_0:
    li t0, 0
    mv s2, t0
while_cond_1:
    li t1, 1
    slt s8, t1, s1
    beq s8, zero, while_end_3
while_body_2:
    li t1, 2
    rem s9, s1, t1
    li t1, 0
    sub s3, s9, t1
    seqz s3, s3
    beq s3, zero, if_else_6
if_then_4:
    li t1, 2
    div s4, s1, t1
    mv s1, s4
    j if_end_5
if_else_6:
    li t0, 3
    mul s5, t0, s1
    li t1, 1
    add s6, s5, t1
    mv s1, s6
    j if_end_5
if_end_5:
    li t1, 1
    add s7, s2, t1
    mv s2, s7
    j while_cond_1
while_end_3:
    mv a0, s2
    j collatz_length_epilogue
collatz_length_epilogue:
    lw s1, 36(sp)
    lw s2, 32(sp)
    lw s3, 28(sp)
    lw s4, 24(sp)
    lw s5, 20(sp)
    lw s6, 16(sp)
    lw s7, 12(sp)
    lw s8, 8(sp)
    lw s9, 4(sp)
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

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
    addi s0, sp, 48


entry_7:
    li t0, 0
    mv s2, t0
    li t0, 1
    mv s1, t0
while_cond_8:
    li t1, 100000
    slt s4, s1, t1
    beq s4, zero, while_end_10
while_body_9:
    mv a0, s1
    call collatz_length
    mv s6, a0
    mv s3, s6
    slt s5, s2, s3
    beq s5, zero, if_end_12
if_then_11:
    mv s2, s3
    j if_end_12
if_end_12:
    li t1, 1
    add s7, s1, t1
    mv s1, s7
    j while_cond_8
while_end_10:
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
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

