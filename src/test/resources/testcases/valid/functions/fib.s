    .text

    .globl fib
fib:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32

entry_0:
    lw t0, -4(s0)
    li t1, 1
    slt t2, t1, t0
    xori t2, t2, 1
    sw t2, -8(s0)
    lw t0, -8(s0)
    beq t0, zero, if_end_2
if_then_1:
    j fib_epilogue
    j if_end_2
if_end_2:
    lw t0, -4(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    mv a0, t0
    call fib
    sw a0, -16(s0)
    lw t0, -4(s0)
    li t1, 2
    sub t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    mv a0, t0
    call fib
    sw a0, -24(s0)
    lw t0, -16(s0)
    lw t1, -24(s0)
    add t2, t0, t1
    sw t2, -28(s0)
    j fib_epilogue
fib_epilogue:
    lw ra, 28(sp)
    lw s0, 24(sp)
    addi sp, sp, 32
    ret

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    addi s0, sp, 16

entry_3:
    li t0, 10
    mv a0, t0
    call fib
    sw a0, -4(s0)
    j main_epilogue
main_epilogue:
    lw ra, 12(sp)
    lw s0, 8(sp)
    addi sp, sp, 16
    ret

