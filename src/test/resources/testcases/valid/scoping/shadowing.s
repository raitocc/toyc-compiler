    .data
    .globl x
x:
    .word 100

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
    sw s6, 0(sp)
    addi s0, sp, 32


entry_0:
    li t0, 10
    mv s5, t0
    li t0, 0
    mv s1, t0
    li t0, 5
    mv s6, t0
    mv s1, s6
    li t0, 0
    mv s2, t0
    li t1, 10
    sub s3, s5, t1
    seqz s3, s3
    beq s3, zero, and_end_2
and_right_1:
    li t1, 5
    sub s4, s1, t1
    seqz s4, s4
    beq s4, zero, and_end_2
    li t0, 1
    mv s2, t0
    j and_end_2
and_end_2:
    beq s2, zero, if_end_4
if_then_3:
    li a0, 1
    j main_epilogue
    j if_end_4
if_end_4:
    li a0, 0
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

