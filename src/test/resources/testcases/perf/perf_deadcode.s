    .text

    .globl main
main:
    addi sp, sp, -96
    sw ra, 92(sp)
    sw s0, 88(sp)
    sw s1, 84(sp)
    sw s2, 80(sp)
    sw s3, 76(sp)
    sw s4, 72(sp)
    sw s5, 68(sp)
    sw s6, 64(sp)
    sw s7, 60(sp)
    sw s8, 56(sp)
    sw s9, 52(sp)
    sw s10, 48(sp)
    sw s11, 44(sp)
    addi s0, sp, 96


entry_0:
    li t0, 0
    mv s1, t0
    li t0, 0
    mv s2, t0
while_cond_1:
    li t1, 1000000
    slt t2, s1, t1
    sw t2, -84(s0)
    lw t0, -84(s0)
    beq t0, zero, while_end_3
while_body_2:
    li t0, 20
    li t1, 30
    mul s3, t0, t1
    li t1, 40
    div s4, s3, t1
    li t0, 10
    add s5, t0, s4
    sw s5, -88(s0)
    lw t0, -88(s0)
    li t1, 5
    add s7, t0, t1
    mv s6, s7
    li t1, 0
    mul s9, s6, t1
    mv s8, s9
    li t1, 1
    add s10, s8, t1
    sw s10, -56(s0)
    li t0, 100
    li t1, 200
    mul t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    li t1, 50
    div s11, t0, t1
    li t1, 400
    add t2, s11, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    li t1, 300
    sub t2, t0, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    li t1, 0
    mul t2, t0, t1
    sw t2, -76(s0)
    lw t1, -76(s0)
    add t2, s2, t1
    sw t2, -72(s0)
    lw t0, -72(s0)
    mv s2, t0
    li t1, 1
    add t2, s1, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    mv s1, t0
    j while_cond_1
while_end_3:
    mv a0, s2
    j main_epilogue
main_epilogue:
    lw s1, 84(sp)
    lw s2, 80(sp)
    lw s3, 76(sp)
    lw s4, 72(sp)
    lw s5, 68(sp)
    lw s6, 64(sp)
    lw s7, 60(sp)
    lw s8, 56(sp)
    lw s9, 52(sp)
    lw s10, 48(sp)
    lw s11, 44(sp)
    lw s0, 88(sp)
    lw ra, 92(sp)
    addi sp, sp, 96
    ret

