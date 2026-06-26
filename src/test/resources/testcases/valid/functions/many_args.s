    .text

    .globl sum8
sum8:
    addi sp, sp, -80
    sw ra, 76(sp)
    sw s0, 72(sp)
    addi s0, sp, 80

    sw a0, -12(s0)
    sw a1, -16(s0)
    sw a2, -20(s0)
    sw a3, -24(s0)
    sw a4, -28(s0)
    sw a5, -32(s0)
    sw a6, -36(s0)
    sw a7, -40(s0)

entry_0:
    lw t0, -12(s0)
    lw t1, -16(s0)
    add t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    lw t1, -20(s0)
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    lw t1, -24(s0)
    add t2, t0, t1
    sw t2, -52(s0)
    lw t0, -52(s0)
    lw t1, -28(s0)
    add t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -56(s0)
    lw t0, -56(s0)
    lw t1, -36(s0)
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    lw t1, -40(s0)
    add t2, t0, t1
    sw t2, -64(s0)
    lw a0, -64(s0)
    j sum8_epilogue
sum8_epilogue:
    lw s0, 72(sp)
    lw ra, 76(sp)
    addi sp, sp, 80
    ret

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    addi s0, sp, 16


entry_1:
    li t0, 1
    mv a0, t0
    li t0, 2
    mv a1, t0
    li t0, 3
    mv a2, t0
    li t0, 4
    mv a3, t0
    li t0, 5
    mv a4, t0
    li t0, 6
    mv a5, t0
    li t0, 7
    mv a6, t0
    li t0, 8
    mv a7, t0
    call sum8
    sw a0, -12(s0)
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

