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
    sw s1, 4(sp)
    sw s2, 0(sp)
    addi s0, sp, 16


entry_0:
    la t0, a
    lw t0, 0(t0)
    la t1, b
    lw t1, 0(t1)
    add s2, t0, t1
    mv s1, s2
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s2, 0(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

