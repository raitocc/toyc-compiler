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
    addi s0, sp, 32


entry_0:
    li t0, 10
    sw t0, -20(s0)
    li t0, 0
    sw t0, -24(s0)
    li t0, 5
    sw t0, -28(s0)
    lw t0, -28(s0)
    sw t0, -24(s0)
    li t0, 0
    sw t0, -32(s0)
    lw t0, -20(s0)
    li t1, 10
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -12(s0)
    lw t0, -12(s0)
    beq t0, zero, and_end_2
and_right_1:
    lw t0, -24(s0)
    li t1, 5
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, and_end_2
    li t0, 1
    sw t0, -32(s0)
    j and_end_2
and_end_2:
    lw t0, -32(s0)
    beq t0, zero, if_end_4
if_then_3:
    li a0, 1
    j main_epilogue
    j if_end_4
if_end_4:
    li a0, 0
    j main_epilogue
main_epilogue:
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

