    .text

    .globl fib
fib:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48

entry_0:
    lw t0, -12(s0)
    li t1, 1
    slt t2, t1, t0
    xori t2, t2, 1
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, if_end_2
if_then_1:
    lw a0, -12(s0)
    j fib_epilogue
    j if_end_2
if_end_2:
    lw t0, -12(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    mv a0, t0
    call fib
    sw a0, -24(s0)
    lw t0, -12(s0)
    li t1, 2
    sub t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    mv a0, t0
    call fib
    sw a0, -32(s0)
    lw t0, -24(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -36(s0)
    lw a0, -36(s0)
    j fib_epilogue
fib_epilogue:
    lw ra, 44(sp)
    lw s0, 40(sp)
    addi sp, sp, 48
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
    sw a0, -12(s0)
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw ra, 12(sp)
    lw s0, 8(sp)
    addi sp, sp, 16
    ret

