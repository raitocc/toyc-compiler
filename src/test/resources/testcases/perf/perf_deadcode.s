    .text

    .globl main
main:
    addi sp, sp, -96
    sw ra, 92(sp)
    sw s0, 88(sp)
    addi s0, sp, 96


entry_0:
    li t0, 0
    sw t0, -72(s0)
    li t0, 0
    sw t0, -76(s0)
while_cond_1:
    lw t0, -72(s0)
    li t1, 1000000
    slt t2, t0, t1
    sw t2, -84(s0)
    lw t0, -84(s0)
    beq t0, zero, while_end_3
while_body_2:
    li t0, 20
    li t1, 30
    mul t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    li t1, 40
    div t2, t0, t1
    sw t2, -16(s0)
    li t0, 10
    lw t1, -16(s0)
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -88(s0)
    lw t0, -88(s0)
    li t1, 5
    add t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -24(s0)
    lw t0, -24(s0)
    li t1, 0
    mul t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -32(s0)
    lw t0, -32(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    sw t0, -44(s0)
    li t0, 100
    li t1, 200
    mul t2, t0, t1
    sw t2, -52(s0)
    lw t0, -52(s0)
    li t1, 50
    div t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    li t1, 400
    add t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    li t1, 300
    sub t2, t0, t1
    sw t2, -56(s0)
    lw t0, -56(s0)
    li t1, 0
    mul t2, t0, t1
    sw t2, -68(s0)
    lw t0, -76(s0)
    lw t1, -68(s0)
    add t2, t0, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    sw t0, -76(s0)
    lw t0, -72(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -72(s0)
    j while_cond_1
while_end_3:
    lw a0, -76(s0)
    j main_epilogue
main_epilogue:
    lw s0, 88(sp)
    lw ra, 92(sp)
    addi sp, sp, 96
    ret

