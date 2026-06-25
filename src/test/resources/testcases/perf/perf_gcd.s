    .text

    .globl gcd
gcd:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    sw s1, 20(sp)
    sw s2, 16(sp)
    sw s3, 12(sp)
    sw s4, 8(sp)
    sw s5, 4(sp)
    addi s0, sp, 32

    mv s2, a0
    mv s1, a1

entry_0:
while_cond_1:
    li t1, 0
    sub s4, s1, t1
    snez s4, s4
    beq s4, zero, while_end_3
while_body_2:
    mv s5, s1
    rem s3, s2, s1
    mv s1, s3
    mv s2, s5
    j while_cond_1
while_end_3:
    mv a0, s2
    j gcd_epilogue
gcd_epilogue:
    lw s1, 20(sp)
    lw s2, 16(sp)
    lw s3, 12(sp)
    lw s4, 8(sp)
    lw s5, 4(sp)
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
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


entry_4:
    li t0, 0
    mv s1, t0
    li t0, 0
    mv s2, t0
while_cond_5:
    li t1, 1000000
    slt s4, s1, t1
    beq s4, zero, while_end_7
while_body_6:
    li t0, 48
    mv a0, t0
    li t0, 18
    mv a1, t0
    call gcd
    mv s5, a0
    add s6, s2, s5
    mv s2, s6
    li t1, 1
    add s3, s1, t1
    mv s1, s3
    j while_cond_5
while_end_7:
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

