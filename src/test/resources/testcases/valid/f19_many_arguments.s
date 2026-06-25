    .text

    .globl add_12
add_12:
    addi sp, sp, -112
    sw ra, 108(sp)
    sw s0, 104(sp)
    sw s1, 100(sp)
    sw s2, 96(sp)
    sw s3, 92(sp)
    sw s4, 88(sp)
    sw s5, 84(sp)
    sw s6, 80(sp)
    sw s7, 76(sp)
    sw s8, 72(sp)
    sw s9, 68(sp)
    sw s10, 64(sp)
    sw s11, 60(sp)
    addi s0, sp, 112

    sw a0, -56(s0)
    sw a1, -60(s0)
    sw a2, -64(s0)
    sw a3, -68(s0)
    mv s1, a4
    mv s2, a5
    mv s3, a6
    mv s4, a7
    lw s5, 0(s0)
    lw s6, 4(s0)
    lw s7, 8(s0)
    lw s11, 12(s0)

entry_0:
    lw t0, -56(s0)
    lw t1, -60(s0)
    add s10, t0, t1
    lw t1, -64(s0)
    add t2, s10, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    lw t1, -68(s0)
    add t2, t0, t1
    sw t2, -76(s0)
    lw t0, -76(s0)
    add t2, t0, s1
    sw t2, -88(s0)
    lw t0, -88(s0)
    add t2, t0, s2
    sw t2, -84(s0)
    lw t0, -84(s0)
    add t2, t0, s3
    sw t2, -96(s0)
    lw t0, -96(s0)
    add t2, t0, s4
    sw t2, -92(s0)
    lw t0, -92(s0)
    add t2, t0, s5
    sw t2, -100(s0)
    lw t0, -100(s0)
    add s9, t0, s6
    add s8, s9, s7
    add t2, s8, s11
    sw t2, -72(s0)
    lw a0, -72(s0)
    j add_12_epilogue
add_12_epilogue:
    lw s1, 100(sp)
    lw s2, 96(sp)
    lw s3, 92(sp)
    lw s4, 88(sp)
    lw s5, 84(sp)
    lw s6, 80(sp)
    lw s7, 76(sp)
    lw s8, 72(sp)
    lw s9, 68(sp)
    lw s10, 64(sp)
    lw s11, 60(sp)
    lw s0, 104(sp)
    lw ra, 108(sp)
    addi sp, sp, 112
    ret

    .globl main
main:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    sw s1, 36(sp)
    sw s2, 32(sp)
    sw s3, 28(sp)
    addi s0, sp, 48


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
    li t0, 9
    sw t0, 0(sp)
    li t0, 10
    sw t0, 4(sp)
    li t0, 11
    sw t0, 8(sp)
    li t0, 12
    sw t0, 12(sp)
    call add_12
    mv s3, a0
    mv s1, s3
    li t1, 78
    sub s2, s1, t1
    seqz s2, s2
    beq s2, zero, if_end_3
if_then_2:
    li a0, 0
    j main_epilogue
    j if_end_3
if_end_3:
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 36(sp)
    lw s2, 32(sp)
    lw s3, 28(sp)
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

