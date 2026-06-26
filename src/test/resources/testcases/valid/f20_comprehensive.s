    .text

    .globl gcd
gcd:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32

    sw a0, -12(s0)
    sw a1, -16(s0)

entry_0:
    lw t0, -16(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, if_end_2
if_then_1:
    lw a0, -12(s0)
    j gcd_epilogue
    j if_end_2
if_end_2:
    lw t0, -12(s0)
    lw t1, -16(s0)
    rem t2, t0, t1
    sw t2, -28(s0)
    lw t0, -16(s0)
    mv a0, t0
    lw t0, -28(s0)
    mv a1, t0
    call gcd
    sw a0, -20(s0)
    lw a0, -20(s0)
    j gcd_epilogue
gcd_epilogue:
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

    .globl magic
magic:
    addi sp, sp, -112
    sw ra, 108(sp)
    sw s0, 104(sp)
    addi s0, sp, 112

    sw a0, -12(s0)
    sw a1, -16(s0)
    sw a2, -20(s0)
    sw a3, -24(s0)
    sw a4, -28(s0)
    sw a5, -32(s0)
    sw a6, -36(s0)
    sw a7, -40(s0)

entry_3:
    lw t0, -12(s0)
    lw t1, -16(s0)
    add t2, t0, t1
    sw t2, -48(s0)
    lw t0, -48(s0)
    lw t1, -20(s0)
    add t2, t0, t1
    sw t2, -60(s0)
    lw t0, -60(s0)
    lw t1, -24(s0)
    add t2, t0, t1
    sw t2, -56(s0)
    lw t0, -56(s0)
    lw t1, -28(s0)
    add t2, t0, t1
    sw t2, -64(s0)
    lw t0, -64(s0)
    sw t0, -52(s0)
    lw t0, -32(s0)
    lw t1, -36(s0)
    add t2, t0, t1
    sw t2, -68(s0)
    lw t0, -68(s0)
    lw t1, -40(s0)
    add t2, t0, t1
    sw t2, -80(s0)
    lw t0, -80(s0)
    lw t1, 0(s0)
    add t2, t0, t1
    sw t2, -76(s0)
    lw t0, -76(s0)
    lw t1, 4(s0)
    add t2, t0, t1
    sw t2, -88(s0)
    lw t0, -88(s0)
    sw t0, -72(s0)
    lw t0, -52(s0)
    lw t1, -72(s0)
    sub t2, t0, t1
    sw t2, -96(s0)
    lw t0, -96(s0)
    sw t0, -84(s0)
    li t0, 1
    neg t1, t0
    sw t1, -104(s0)
    lw t0, -84(s0)
    lw t1, -104(s0)
    mul t2, t0, t1
    sw t2, -100(s0)
    lw t0, -100(s0)
    sw t0, -92(s0)
    lw t0, -92(s0)
    mv a0, t0
    li t0, 100
    mv a1, t0
    call gcd
    sw a0, -44(s0)
    lw a0, -44(s0)
    j magic_epilogue
magic_epilogue:
    lw s0, 104(sp)
    lw ra, 108(sp)
    addi sp, sp, 112
    ret

    .globl main
main:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48


entry_4:
    li t0, 1
    sw t0, -16(s0)
    li t0, 0
    sw t0, -12(s0)
while_cond_5:
    lw t0, -16(s0)
    li t1, 10
    slt t2, t1, t0
    xori t2, t2, 1
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, while_end_7
while_body_6:
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
    call magic
    sw a0, -20(s0)
    lw t0, -12(s0)
    lw t1, -20(s0)
    add t2, t0, t1
    sw t2, -32(s0)
    lw t0, -32(s0)
    sw t0, -12(s0)
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -16(s0)
    j while_cond_5
while_end_7:
    lw t0, -12(s0)
    li t1, 250
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -36(s0)
    lw t0, -36(s0)
    beq t0, zero, if_end_9
if_then_8:
    li a0, 0
    j main_epilogue
    j if_end_9
if_end_9:
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

