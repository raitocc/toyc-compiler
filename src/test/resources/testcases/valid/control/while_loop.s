    .text

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
    addi s0, sp, 32


entry_0:
    li t0, 0
    mv s2, t0
    li t0, 0
    mv s1, t0
while_cond_1:
    li t1, 5
    slt s4, s1, t1
    beq s4, zero, while_end_3
while_body_2:
    add s5, s2, s1
    mv s2, s5
    li t1, 1
    add s3, s1, t1
    mv s1, s3
    j while_cond_1
while_end_3:
    mv a0, s2
    j main_epilogue
main_epilogue:
    lw s1, 20(sp)
    lw s2, 16(sp)
    lw s3, 12(sp)
    lw s4, 8(sp)
    lw s5, 4(sp)
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

