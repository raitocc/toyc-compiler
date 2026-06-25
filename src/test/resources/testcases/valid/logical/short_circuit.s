    .data
    .globl counter
counter:
    .word 0

    .text

    .globl increment
increment:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
    addi s0, sp, 16


entry_0:
    la t0, counter
    lw t0, 0(t0)
    li t1, 1
    add s1, t0, t1
    la t6, counter
    sw s1, 0(t6)
    li a0, 1
    j increment_epilogue
increment_epilogue:
    lw s1, 4(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
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
    addi s0, sp, 32


entry_1:
    li t0, 0
    mv s1, t0
    li t0, 0
    mv s3, t0
    beq s1, zero, and_end_3
and_right_2:
    call increment
    mv s5, a0
    beq s5, zero, and_end_3
    li t0, 1
    mv s3, t0
    j and_end_3
and_end_3:
    beq s3, zero, if_end_5
if_then_4:
    li t0, 1
    mv s1, t0
    j if_end_5
if_end_5:
    li t0, 1
    mv s2, t0
    bne s1, zero, or_end_7
or_right_6:
    call increment
    mv s4, a0
    bne s4, zero, or_end_7
    li t0, 0
    mv s2, t0
    j or_end_7
or_end_7:
    beq s2, zero, if_end_9
if_then_8:
    li t0, 2
    mv s1, t0
    j if_end_9
if_end_9:
    la a0, counter
    lw a0, 0(a0)
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

