    .text

    .globl hanoi
hanoi:
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
    sw s8, 8(sp)
    addi s0, sp, 48

    mv s1, a0

entry_0:
    li t1, 1
    sub s6, s1, t1
    seqz s6, s6
    beq s6, zero, if_end_2
if_then_1:
    li a0, 1
    j hanoi_epilogue
    j if_end_2
if_end_2:
    li t1, 1
    sub s7, s1, t1
    mv a0, s7
    call hanoi
    mv s8, a0
    li t1, 1
    add s2, s8, t1
    li t1, 1
    sub s3, s1, t1
    mv a0, s3
    call hanoi
    mv s4, a0
    add s5, s2, s4
    mv a0, s5
    j hanoi_epilogue
hanoi_epilogue:
    lw s1, 36(sp)
    lw s2, 32(sp)
    lw s3, 28(sp)
    lw s4, 24(sp)
    lw s5, 20(sp)
    lw s6, 16(sp)
    lw s7, 12(sp)
    lw s8, 8(sp)
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    sw s1, 4(sp)
    addi s0, sp, 16


entry_3:
    li t0, 25
    mv a0, t0
    call hanoi
    mv s1, a0
    mv a0, s1
    j main_epilogue
main_epilogue:
    lw s1, 4(sp)
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

