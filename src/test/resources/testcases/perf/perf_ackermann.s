    .text

    .globl ack
ack:
    addi sp, sp, -64
    sw ra, 60(sp)
    sw s0, 56(sp)
    addi s0, sp, 64

    sw a0, -12(s0)
    sw a1, -16(s0)

entry_0:
    lw t0, -12(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -56(s0)
    lw t0, -56(s0)
    beq t0, zero, if_end_2
if_then_1:
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -60(s0)
    lw a0, -60(s0)
    j ack_epilogue
    j if_end_2
if_end_2:
    li t0, 0
    sw t0, -20(s0)
    lw t0, -12(s0)
    li t1, 0
    slt t2, t1, t0
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, and_end_4
and_right_3:
    lw t0, -16(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -28(s0)
    lw t0, -28(s0)
    beq t0, zero, and_end_4
    li t0, 1
    sw t0, -20(s0)
    j and_end_4
and_end_4:
    lw t0, -20(s0)
    beq t0, zero, if_end_6
if_then_5:
    lw t0, -12(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -32(s0)
    lw t0, -32(s0)
    mv a0, t0
    li t0, 1
    mv a1, t0
    call ack
    sw a0, -36(s0)
    lw a0, -36(s0)
    j ack_epilogue
    j if_end_6
if_end_6:
    lw t0, -12(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -40(s0)
    lw t0, -16(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -44(s0)
    lw t0, -12(s0)
    mv a0, t0
    lw t0, -44(s0)
    mv a1, t0
    call ack
    sw a0, -52(s0)
    lw t0, -40(s0)
    mv a0, t0
    lw t0, -52(s0)
    mv a1, t0
    call ack
    sw a0, -48(s0)
    lw a0, -48(s0)
    j ack_epilogue
ack_epilogue:
    lw s0, 56(sp)
    lw ra, 60(sp)
    addi sp, sp, 64
    ret

    .globl main
main:
    addi sp, sp, -16
    sw ra, 12(sp)
    sw s0, 8(sp)
    addi s0, sp, 16


entry_7:
    li t0, 3
    mv a0, t0
    li t0, 6
    mv a1, t0
    call ack
    sw a0, -12(s0)
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw s0, 8(sp)
    lw ra, 12(sp)
    addi sp, sp, 16
    ret

