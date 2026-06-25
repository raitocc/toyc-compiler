    .text

    .globl sum8
sum8:
    addi sp, sp, -80
    sw ra, 76(sp)
    sw s0, 72(sp)
    sw s1, 68(sp)
    sw s2, 64(sp)
    sw s3, 60(sp)
    sw s4, 56(sp)
    sw s5, 52(sp)
    sw s6, 48(sp)
    sw s7, 44(sp)
    sw s8, 40(sp)
    sw s9, 36(sp)
    sw s10, 32(sp)
    sw s11, 28(sp)
    addi s0, sp, 80

    sw a0, -56(s0)
    sw a1, -60(s0)
    sw a2, -64(s0)
    sw a3, -68(s0)
    mv s1, a4
    mv s2, a5
    mv s3, a6
    mv s4, a7

entry_0:
    lw t0, -56(s0)
    lw t1, -60(s0)
    add s5, t0, t1
    lw t1, -64(s0)
    add s6, s5, t1
    lw t1, -68(s0)
    add s7, s6, t1
    add s9, s7, s1
    add s8, s9, s2
    add s11, s8, s3
    add s10, s11, s4
    mv a0, s10
    j sum8_epilogue
sum8_epilogue:
    lw s1, 68(sp)
    lw s2, 64(sp)
    lw s3, 60(sp)
    lw s4, 56(sp)
    lw s5, 52(sp)
    lw s6, 48(sp)
    lw s7, 44(sp)
    lw s8, 40(sp)
    lw s9, 36(sp)
    lw s10, 32(sp)
    lw s11, 28(sp)
    lw s0, 72(sp)
    lw ra, 76(sp)
    addi sp, sp, 80
    ret

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
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
    mv s1, a0
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

