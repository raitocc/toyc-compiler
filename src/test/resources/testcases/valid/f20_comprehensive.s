    .text

    .globl gcd
gcd:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    sw s1, 20(sp)
    sw s2, 16(sp)
    sw s3, 12(sp)
    sw s4, 8(sp)
    sw s5, 4(sp)
    addi s0, sp, 32

    mv s2, a0
    mv s1, a1

entry_0:
    li t1, 0
    sub s4, s1, t1
    seqz s4, s4
    beq s4, zero, if_end_2
if_then_1:
    mv a0, s2
    j gcd_epilogue
    j if_end_2
if_end_2:
    mv a0, s1
    rem s5, s2, s1
    mv a1, s5
    call gcd
    mv s3, a0
    mv a0, s3
    j gcd_epilogue
gcd_epilogue:
    lw s1, 20(sp)
    lw s2, 16(sp)
    lw s3, 12(sp)
    lw s4, 8(sp)
    lw s5, 4(sp)
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

    .globl magic
magic:
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

    mv s1, a0
    mv s2, a1
    mv s3, a2
    mv s4, a3
    mv s5, a4
    mv s7, a5
    mv s9, a6
    mv s8, a7
    lw s11, 0(s0)
    lw s10, 4(s0)

entry_3:
    add t2, s1, s2
    sw t2, -56(s0)
    lw t0, -56(s0)
    add t2, t0, s3
    sw t2, -68(s0)
    lw t0, -68(s0)
    add t2, t0, s4
    sw t2, -64(s0)
    lw t0, -64(s0)
    add t2, t0, s5
    sw t2, -72(s0)
    lw t0, -72(s0)
    sw t0, -60(s0)
    add t2, s7, s9
    sw t2, -76(s0)
    lw t0, -76(s0)
    add t2, t0, s8
    sw t2, -88(s0)
    lw t0, -88(s0)
    add t2, t0, s11
    sw t2, -84(s0)
    lw t0, -84(s0)
    add t2, t0, s10
    sw t2, -96(s0)
    lw t0, -96(s0)
    sw t0, -80(s0)
    lw t0, -60(s0)
    lw t1, -80(s0)
    sub t2, t0, t1
    sw t2, -104(s0)
    lw t0, -104(s0)
    sw t0, -92(s0)
    li t0, 1
    neg t1, t0
    sw t1, -112(s0)
    lw t0, -92(s0)
    lw t1, -112(s0)
    mul t2, t0, t1
    sw t2, -108(s0)
    lw t0, -108(s0)
    sw t0, -100(s0)
    lw t0, -100(s0)
    mv a0, t0
    li t0, 100
    mv a1, t0
    call gcd
    mv s6, a0
    mv a0, s6
    j magic_epilogue
magic_epilogue:
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
    sw s4, 24(sp)
    sw s5, 20(sp)
    sw s6, 16(sp)
    sw s7, 12(sp)
    addi s0, sp, 48


entry_4:
    li t0, 1
    mv s2, t0
    li t0, 0
    mv s1, t0
while_cond_5:
    li t1, 10
    slt s4, t1, s2
    xori s4, s4, 1
    beq s4, zero, while_end_7
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
    mv s3, a0
    add s6, s1, s3
    mv s1, s6
    li t1, 1
    add s5, s2, t1
    mv s2, s5
    j while_cond_5
while_end_7:
    li t1, 250
    sub s7, s1, t1
    seqz s7, s7
    beq s7, zero, if_end_9
if_then_8:
    li a0, 0
    j main_epilogue
    j if_end_9
if_end_9:
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 36(sp)
    lw s2, 32(sp)
    lw s3, 28(sp)
    lw s4, 24(sp)
    lw s5, 20(sp)
    lw s6, 16(sp)
    lw s7, 12(sp)
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

