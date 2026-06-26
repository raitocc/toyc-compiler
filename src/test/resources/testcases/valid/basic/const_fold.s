    .text

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    addi s0, sp, 16


entry_0:
    li t0, 3
    li t1, 6
    add t2, t0, t1
    sw t2, -12(s0)
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

