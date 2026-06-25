    .text

    .globl is_prime
is_prime:
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

    mv s2, a0

entry_0:
    li t0, 2
    mv s1, t0
while_cond_1:
    mul s6, s1, s1
    slt s7, s2, s6
    xori s7, s7, 1
    beq s7, zero, while_end_3
while_body_2:
    rem s3, s2, s1
    li t1, 0
    sub s4, s3, t1
    seqz s4, s4
    beq s4, zero, if_end_5
if_then_4:
    li a0, 0
    j is_prime_epilogue
    j if_end_5
if_end_5:
    li t1, 1
    add s5, s1, t1
    mv s1, s5
    j while_cond_1
while_end_3:
    li a0, 1
    j is_prime_epilogue
is_prime_epilogue:
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

    .globl main
main:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    sw s1, 20(sp)
    sw s2, 16(sp)
    sw s3, 12(sp)
    sw s4, 8(sp)
    sw s5, 4(sp)
    sw s6, 0(sp)
    addi s0, sp, 32


entry_6:
    li t0, 0
    mv s2, t0
    li t0, 2
    mv s1, t0
while_cond_7:
    li t1, 200000
    slt s5, s1, t1
    beq s5, zero, while_end_9
while_body_8:
    mv a0, s1
    call is_prime
    mv s3, a0
    beq s3, zero, if_end_11
if_then_10:
    li t1, 1
    add s6, s2, t1
    mv s2, s6
    j if_end_11
if_end_11:
    li t1, 1
    add s4, s1, t1
    mv s1, s4
    j while_cond_7
while_end_9:
    mv a0, s2
    j main_epilogue
main_epilogue:
    lw s1, 20(sp)
    lw s2, 16(sp)
    lw s3, 12(sp)
    lw s4, 8(sp)
    lw s5, 4(sp)
    lw s6, 0(sp)
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

