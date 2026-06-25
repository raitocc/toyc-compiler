    .text

    .globl fib
fib:
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

    mv s1, a0

entry_0:
    li t1, 1
    slt s5, t1, s1
    xori s5, s5, 1
    beq s5, zero, if_end_2
if_then_1:
    mv a0, s1
    j fib_epilogue
    j if_end_2
if_end_2:
    li t1, 1
    sub s6, s1, t1
    mv a0, s6
    call fib
    mv s7, a0
    li t1, 2
    sub s2, s1, t1
    mv a0, s2
    call fib
    mv s3, a0
    add s4, s7, s3
    mv a0, s4
    j fib_epilogue
fib_epilogue:
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
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
    addi s0, sp, 16


entry_3:
    li t0, 10
    mv a0, t0
    call fib
    mv s1, a0
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

