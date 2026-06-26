    .text

    .globl main
main:
    addi sp, sp, -176
    sw ra, 172(sp)
    sw s0, 168(sp)
    addi s0, sp, 176


entry_0:
    li t0, 0
    sw t0, -156(s0)
    li t0, 1
    sw t0, -160(s0)
    li t0, 2
    sw t0, -164(s0)
    li t0, 3
    sw t0, -168(s0)
    li t0, 4
    sw t0, -12(s0)
    li t0, 5
    sw t0, -16(s0)
    li t0, 6
    sw t0, -20(s0)
    li t0, 7
    sw t0, -24(s0)
    li t0, 8
    sw t0, -28(s0)
    li t0, 9
    sw t0, -32(s0)
    li t0, 10
    sw t0, -40(s0)
    li t0, 11
    sw t0, -60(s0)
    li t0, 0
    sw t0, -52(s0)
while_cond_1:
    lw t0, -52(s0)
    li t1, 3000000
    slt t2, t0, t1
    sw t2, -76(s0)
    lw t0, -76(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -156(s0)
    lw t1, -160(s0)
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -92(s0)
    lw t0, -92(s0)
    sw t0, -156(s0)
    lw t0, -160(s0)
    lw t1, -164(s0)
    add t2, t0, t1
    sw t2, -84(s0)
    lw t0, -84(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -104(s0)
    lw t0, -104(s0)
    sw t0, -160(s0)
    lw t0, -164(s0)
    lw t1, -168(s0)
    add t2, t0, t1
    sw t2, -100(s0)
    lw t0, -100(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -112(s0)
    lw t0, -112(s0)
    sw t0, -164(s0)
    lw t0, -168(s0)
    lw t1, -12(s0)
    add t2, t0, t1
    sw t2, -120(s0)
    lw t0, -120(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -116(s0)
    lw t0, -116(s0)
    sw t0, -168(s0)
    lw t0, -12(s0)
    lw t1, -16(s0)
    add t2, t0, t1
    sw t2, -128(s0)
    lw t0, -128(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -124(s0)
    lw t0, -124(s0)
    sw t0, -12(s0)
    lw t0, -16(s0)
    lw t1, -20(s0)
    add t2, t0, t1
    sw t2, -136(s0)
    lw t0, -136(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -132(s0)
    lw t0, -132(s0)
    sw t0, -16(s0)
    lw t0, -20(s0)
    lw t1, -24(s0)
    add t2, t0, t1
    sw t2, -144(s0)
    lw t0, -144(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -140(s0)
    lw t0, -140(s0)
    sw t0, -20(s0)
    lw t0, -24(s0)
    lw t1, -28(s0)
    add t2, t0, t1
    sw t2, -152(s0)
    lw t0, -152(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -148(s0)
    lw t0, -148(s0)
    sw t0, -24(s0)
    lw t0, -28(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    sw t0, -28(s0)
    lw t0, -32(s0)
    lw t1, -40(s0)
    add t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    sw t0, -32(s0)
    lw t0, -40(s0)
    lw t1, -60(s0)
    add t2, t0, t1
    sw t2, -56(s0)
    lw t0, -56(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    sw t0, -40(s0)
    lw t0, -60(s0)
    lw t1, -156(s0)
    add t2, t0, t1
    sw t2, -72(s0)
    lw t0, -72(s0)
    li t1, 100
    rem t2, t0, t1
    sw t2, -96(s0)
    lw t0, -96(s0)
    sw t0, -60(s0)
    lw t0, -52(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -88(s0)
    lw t0, -88(s0)
    sw t0, -52(s0)
    j while_cond_1
while_end_3:
    lw t0, -156(s0)
    lw t1, -60(s0)
    add t2, t0, t1
    sw t2, -108(s0)
    lw a0, -108(s0)
    j main_epilogue
main_epilogue:
    lw s0, 168(sp)
    lw ra, 172(sp)
    addi sp, sp, 176
    ret

