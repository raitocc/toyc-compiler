    .data
    .globl DEPTH
DEPTH:
    .word 4000

    .globl ROUNDS
ROUNDS:
    .word 4000

    .text

    .globl tail_rec
tail_rec:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48

    sw a0, -12(s0)
    sw a1, -16(s0)

entry_0:
    lw t0, -12(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -32(s0)
    lw t0, -32(s0)
    beq t0, zero, if_end_2
if_then_1:
    lw a0, -16(s0)
    j tail_rec_epilogue
    j if_end_2
if_end_2:
    lw t0, -12(s0)
    li t1, 1
    sub t2, t0, t1
    sw t2, -36(s0)
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    li t1, 251
    rem t2, t0, t1
    sw t2, -24(s0)
    lw t0, -36(s0)
    mv a0, t0
    lw t0, -24(s0)
    mv a1, t0
    call tail_rec
    sw a0, -28(s0)
    lw a0, -28(s0)
    j tail_rec_epilogue
tail_rec_epilogue:
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

    .globl main
main:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48


entry_3:
    li t0, 0
    sw t0, -16(s0)
    li t0, 0
    sw t0, -20(s0)
while_cond_4:
    lw t0, -16(s0)
    la t1, ROUNDS
    lw t1, 0(t1)
    slt t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    beq t0, zero, while_end_6
while_body_5:
    lw t0, -16(s0)
    li t1, 7
    rem t2, t0, t1
    sw t2, -12(s0)
    la t0, DEPTH
    lw t0, 0(t0)
    mv a0, t0
    lw t0, -12(s0)
    mv a1, t0
    call tail_rec
    sw a0, -32(s0)
    lw t0, -20(s0)
    lw t1, -32(s0)
    add t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    li t1, 251
    rem t2, t0, t1
    sw t2, -40(s0)
    lw t0, -40(s0)
    sw t0, -20(s0)
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -16(s0)
    j while_cond_4
while_end_6:
    lw a0, -20(s0)
    j main_epilogue
main_epilogue:
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

