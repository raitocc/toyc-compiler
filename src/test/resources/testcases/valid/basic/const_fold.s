    .text

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
    addi s0, sp, 16


entry_0:
    li t0, 3
    li t1, 6
    add s1, t0, t1
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

