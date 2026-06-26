    .data
    .globl LIMIT
LIMIT:
    .word 10000000

    .text

    .globl main
main:
    addi sp, sp, -96
    sw ra, 92(sp)
    sw s0, 88(sp)
    addi s0, sp, 96


entry_0:
    li t0, 0
    sw t0, -76(s0)
    li t0, 0
    sw t0, -80(s0)
while_cond_1:
    lw t0, -76(s0)
    la t1, LIMIT
    lw t1, 0(t1)
    slt t2, t0, t1
    sw t2, -88(s0)
    lw t0, -88(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -76(s0)
    li t1, 2
    mul t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    sw t0, -92(s0)
    lw t0, -92(s0)
    li t1, 3
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -16(s0)
    lw t0, -16(s0)
    li t1, 4
    mul t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -24(s0)
    lw t0, -24(s0)
    li t1, 5
    sub t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -32(s0)
    lw t0, -32(s0)
    li t1, 2
    div t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    sw t0, -48(s0)
    lw t0, -48(s0)
    li t1, 3
    rem t2, t0, t1
    sw t2, -52(s0)
    lw t0, -52(s0)
    sw t0, -56(s0)
    lw t0, -76(s0)
    li t1, 5
    rem t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -72(s0)
    lw t0, -72(s0)
    sw t0, -64(s0)
    lw t0, -80(s0)
    lw t1, -64(s0)
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    li t1, 251
    rem t2, t0, t1
    sw t2, -84(s0)
    lw t0, -84(s0)
    sw t0, -80(s0)
    lw t0, -76(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    sw t0, -76(s0)
    j while_cond_1
while_end_3:
    lw a0, -80(s0)
    j main_epilogue
main_epilogue:
    lw s0, 88(sp)
    lw ra, 92(sp)
    addi sp, sp, 96
    ret

