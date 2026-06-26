    .data
    .globl OUTER
OUTER:
    .word 180

    .globl MID
MID:
    .word 160

    .globl INNER
INNER:
    .word 100

    .text

    .globl main
main:
    addi sp, sp, -160
    sw ra, 156(sp)
    sw s0, 152(sp)
    addi s0, sp, 160


entry_0:
    li t0, 0
    sw t0, -144(s0)
    li t0, 0
    sw t0, -148(s0)
while_cond_1:
    lw t0, -144(s0)
    la t1, OUTER
    lw t1, 0(t1)
    slt t2, t0, t1
    sw t2, -152(s0)
    lw t0, -152(s0)
    beq t0, zero, while_end_3
while_body_2:
    li t0, 0
    sw t0, -156(s0)
while_cond_4:
    lw t0, -156(s0)
    la t1, MID
    lw t1, 0(t1)
    slt t2, t0, t1
    sw t2, -12(s0)
    lw t0, -12(s0)
    beq t0, zero, while_end_6
while_body_5:
    li t0, 0
    sw t0, -16(s0)
while_cond_7:
    lw t0, -16(s0)
    la t1, INNER
    lw t1, 0(t1)
    slt t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    beq t0, zero, while_end_9
while_body_8:
    lw t0, -144(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -24(s0)
    lw t0, -156(s0)
    li t1, 2
    add t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    sw t0, -32(s0)
    lw t0, -16(s0)
    li t1, 3
    add t2, t0, t1
    sw t2, -52(s0)
    lw t0, -52(s0)
    sw t0, -60(s0)
    lw t0, -24(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    sw t0, -76(s0)
    lw t0, -32(s0)
    lw t1, -60(s0)
    add t2, t0, t1
    sw t2, -84(s0)
    lw t0, -84(s0)
    sw t0, -88(s0)
    lw t0, -60(s0)
    lw t1, -76(s0)
    add t2, t0, t1
    sw t2, -92(s0)
    lw t0, -92(s0)
    sw t0, -96(s0)
    lw t0, -76(s0)
    lw t1, -88(s0)
    add t2, t0, t1
    sw t2, -108(s0)
    lw t0, -108(s0)
    sw t0, -100(s0)
    lw t0, -88(s0)
    lw t1, -96(s0)
    add t2, t0, t1
    sw t2, -116(s0)
    lw t0, -116(s0)
    sw t0, -104(s0)
    lw t0, -96(s0)
    lw t1, -100(s0)
    add t2, t0, t1
    sw t2, -124(s0)
    lw t0, -124(s0)
    sw t0, -112(s0)
    lw t0, -100(s0)
    lw t1, -104(s0)
    add t2, t0, t1
    sw t2, -132(s0)
    lw t0, -132(s0)
    sw t0, -120(s0)
    lw t0, -104(s0)
    lw t1, -112(s0)
    add t2, t0, t1
    sw t2, -140(s0)
    lw t0, -140(s0)
    sw t0, -128(s0)
    lw t0, -112(s0)
    lw t1, -120(s0)
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -136(s0)
    lw t0, -148(s0)
    lw t1, -128(s0)
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    lw t1, -136(s0)
    add t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    li t1, 251
    rem t2, t0, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    sw t0, -148(s0)
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -56(s0)
    lw t0, -56(s0)
    sw t0, -16(s0)
    j while_cond_7
while_end_9:
    lw t0, -156(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -156(s0)
    j while_cond_4
while_end_6:
    lw t0, -144(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -72(s0)
    lw t0, -72(s0)
    sw t0, -144(s0)
    j while_cond_1
while_end_3:
    lw a0, -148(s0)
    j main_epilogue
main_epilogue:
    lw s0, 152(sp)
    lw ra, 156(sp)
    addi sp, sp, 160
    ret

