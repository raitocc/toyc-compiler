    .text

    .globl main
main:
    addi sp, sp, -128
    sw ra, 124(sp)
    sw s0, 120(sp)
    addi s0, sp, 128


entry_0:
    li t0, 0
    sw t0, -104(s0)
    li t0, 1
    sw t0, -108(s0)
    li t0, 2
    sw t0, -112(s0)
    li t0, 3
    sw t0, -116(s0)
    li t0, 4
    sw t0, -12(s0)
while_cond_1:
    lw t0, -104(s0)
    li t1, 10000000
    slt t2, t0, t1
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -108(s0)
    lw t1, -108(s0)
    mul t2, t0, t1
    sw t2, -24(s0)
    lw t0, -112(s0)
    lw t1, -116(s0)
    mul t2, t0, t1
    sw t2, -28(s0)
    lw t0, -24(s0)
    lw t1, -28(s0)
    add t2, t0, t1
    sw t2, -32(s0)
    lw t0, -32(s0)
    li t1, 1000
    rem t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -20(s0)
    lw t0, -108(s0)
    lw t1, -112(s0)
    mul t2, t0, t1
    sw t2, -40(s0)
    lw t0, -112(s0)
    lw t1, -12(s0)
    mul t2, t0, t1
    sw t2, -52(s0)
    lw t0, -40(s0)
    lw t1, -52(s0)
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    li t1, 1000
    rem t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    sw t0, -44(s0)
    lw t0, -116(s0)
    lw t1, -108(s0)
    mul t2, t0, t1
    sw t2, -68(s0)
    lw t0, -12(s0)
    lw t1, -116(s0)
    mul t2, t0, t1
    sw t2, -64(s0)
    lw t0, -68(s0)
    lw t1, -64(s0)
    add t2, t0, t1
    sw t2, -72(s0)
    lw t0, -72(s0)
    li t1, 1000
    rem t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -56(s0)
    lw t0, -116(s0)
    lw t1, -112(s0)
    mul t2, t0, t1
    sw t2, -88(s0)
    lw t0, -12(s0)
    lw t1, -12(s0)
    mul t2, t0, t1
    sw t2, -84(s0)
    lw t0, -88(s0)
    lw t1, -84(s0)
    add t2, t0, t1
    sw t2, -96(s0)
    lw t0, -96(s0)
    li t1, 1000
    rem t2, t0, t1
    sw t2, -92(s0)
    lw t0, -92(s0)
    sw t0, -76(s0)
    lw t0, -20(s0)
    sw t0, -108(s0)
    lw t0, -44(s0)
    sw t0, -112(s0)
    lw t0, -56(s0)
    sw t0, -116(s0)
    lw t0, -76(s0)
    sw t0, -12(s0)
    lw t0, -104(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -100(s0)
    lw t0, -100(s0)
    sw t0, -104(s0)
    j while_cond_1
while_end_3:
    lw a0, -108(s0)
    j main_epilogue
main_epilogue:
    lw s0, 120(sp)
    lw ra, 124(sp)
    addi sp, sp, 128
    ret

