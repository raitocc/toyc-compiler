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
    addi s0, sp, 16

entry_0:
    la t0, counter
    lw t0, 0(t0)
    li t1, 1
    add t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    li a0, 1
    j increment_epilogue
increment_epilogue:
    lw ra, 12(sp)
    lw s0, 8(sp)
    addi sp, sp, 16
    ret

    .globl main
main:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32

entry_1:
    li t0, 0
    sw t0, -12(s0)
    li t0, 0
    sw t0, -16(s0)
    lw t0, -12(s0)
    beq t0, zero, and_end_3
and_right_2:
    call increment
    sw a0, -20(s0)
    lw t0, -20(s0)
    beq t0, zero, and_end_3
    li t0, 1
    sw t0, -16(s0)
    j and_end_3
and_end_3:
    lw t0, -16(s0)
    beq t0, zero, if_end_5
if_then_4:
    li t0, 1
    j if_end_5
if_end_5:
    li t0, 1
    sw t0, -24(s0)
    lw t0, -12(s0)
    bne t0, zero, or_end_7
or_right_6:
    call increment
    sw a0, -28(s0)
    lw t0, -28(s0)
    bne t0, zero, or_end_7
    li t0, 0
    sw t0, -24(s0)
    j or_end_7
or_end_7:
    lw t0, -24(s0)
    beq t0, zero, if_end_9
if_then_8:
    li t0, 2
    j if_end_9
if_end_9:
    la a0, counter
    lw a0, 0(a0)
    j main_epilogue
main_epilogue:
    lw ra, 28(sp)
    lw s0, 24(sp)
    addi sp, sp, 32
    ret

