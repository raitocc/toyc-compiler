    .data
    .globl a
a:
    .word 1

    .globl b
b:
    .word 2

    .text

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    addi s0, sp, 16

entry_0:
    la t0, a
    lw t0, 0(t0)
    la t1, b
    lw t1, 0(t1)
    add t2, t0, t1
    sw t2, -4(s0)
    lw t0, -4(s0)
    sw t0, -8(s0)
    j main_epilogue
main_epilogue:
    lw ra, 12(sp)
    lw s0, 8(sp)
    addi sp, sp, 16
    ret

