    .data
    .globl LIMIT
LIMIT:
    .word 10000000

    .text

    .globl main
main:
    addi sp, sp, -112
    sw ra, 108(sp)
    sw s0, 104(sp)
    addi s0, sp, 112


entry_0:
    li t0, 0
    sw t0, -88(s0)
    li t0, 0
    sw t0, -92(s0)
while_cond_1:
    lw t0, -88(s0)
    la t1, LIMIT
    lw t1, 0(t1)
    slt t2, t0, t1
    sw t2, -100(s0)
    lw t0, -100(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -88(s0)
    li t1, 101
    rem t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    li t1, 2
    mul t2, t0, t1
    sw t2, -16(s0)
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -104(s0)
    lw t0, -104(s0)
    li t1, 0
    add t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -24(s0)
    lw t0, -24(s0)
    li t1, 1
    mul t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -32(s0)
    lw t0, -104(s0)
    li t1, 2
    mul t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    sw t0, -56(s0)
    lw t0, -32(s0)
    lw t1, -56(s0)
    add t2, t0, t1
    sw t2, -76(s0)
    lw t0, -76(s0)
    sw t0, -64(s0)
    lw t0, -64(s0)
    sw t0, -72(s0)
    lw t0, -72(s0)
    sw t0, -84(s0)
    lw t0, -84(s0)
    li t1, 0
    mul t2, t0, t1
    sw t2, -96(s0)
    lw t0, -96(s0)
    sw t0, -80(s0)
    lw t0, -92(s0)
    lw t1, -72(s0)
    add t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    lw t1, -80(s0)
    add t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    li t1, 251
    rem t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    sw t0, -92(s0)
    lw t0, -88(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -52(s0)
    lw t0, -52(s0)
    sw t0, -88(s0)
    j while_cond_1
while_end_3:
    lw a0, -92(s0)
    j main_epilogue
main_epilogue:
    lw s0, 104(sp)
    lw ra, 108(sp)
    addi sp, sp, 112
    ret

