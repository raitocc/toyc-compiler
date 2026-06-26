    .text

    .globl add_12
add_12:
    addi sp, sp, -96
    sw ra, 92(sp)
    sw s0, 88(sp)
    addi s0, sp, 96

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
    sw t2, -52(s0)
    lw t0, -52(s0)
    lw t1, -20(s0)
    add t2, t0, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    lw t1, -24(s0)
    add t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    lw t1, -28(s0)
    add t2, t0, t1
    sw t2, -72(s0)
    lw t0, -72(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    lw t1, -36(s0)
    add t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    lw t1, -40(s0)
    add t2, t0, t1
    sw t2, -76(s0)
    lw t0, -76(s0)
    lw t1, 0(s0)
    add t2, t0, t1
    sw t2, -84(s0)
    lw t0, -84(s0)
    lw t1, 4(s0)
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    lw t1, 8(s0)
    add t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    lw t1, 12(s0)
    add t2, t0, t1
    sw t2, -56(s0)
    lw a0, -56(s0)
    j add_12_epilogue
add_12_epilogue:
    lw s0, 88(sp)
    lw ra, 92(sp)
    addi sp, sp, 96
    ret

    .globl main
main:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
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
    sw a0, -20(s0)
    lw t0, -20(s0)
    sw t0, -12(s0)
    lw t0, -12(s0)
    li t1, 78
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, if_end_3
if_then_2:
    li a0, 0
    j main_epilogue
    j if_end_3
if_end_3:
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

