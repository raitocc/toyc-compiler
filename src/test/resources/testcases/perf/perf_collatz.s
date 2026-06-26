    .text

    .globl collatz_length
collatz_length:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48

    sw a0, -12(s0)

entry_0:
    li t0, 0
    sw t0, -36(s0)
while_cond_1:
    lw t0, -12(s0)
    li t1, 1
    slt t2, t1, t0
    sw t2, -40(s0)
    lw t0, -40(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -12(s0)
    li t1, 2
    rem t2, t0, t1
    sw t2, -44(s0)
    lw t0, -44(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -16(s0)
    lw t0, -16(s0)
    beq t0, zero, if_else_6
if_then_4:
    lw t0, -12(s0)
    li t1, 2
    div t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -12(s0)
    j if_end_5
if_else_6:
    li t0, 3
    lw t1, -12(s0)
    mul t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -12(s0)
    j if_end_5
if_end_5:
    lw t0, -36(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -32(s0)
    lw t0, -32(s0)
    sw t0, -36(s0)
    j while_cond_1
while_end_3:
    lw a0, -36(s0)
    j collatz_length_epilogue
collatz_length_epilogue:
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


entry_7:
    li t0, 0
    sw t0, -20(s0)
    li t0, 1
    sw t0, -12(s0)
while_cond_8:
    lw t0, -12(s0)
    li t1, 100000
    slt t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, while_end_10
while_body_9:
    lw t0, -12(s0)
    mv a0, t0
    call collatz_length
    sw a0, -32(s0)
    lw t0, -32(s0)
    sw t0, -16(s0)
    lw t0, -16(s0)
    lw t1, -20(s0)
    slt t2, t1, t0
    sw t2, -28(s0)
    lw t0, -28(s0)
    beq t0, zero, if_end_12
if_then_11:
    lw t0, -16(s0)
    sw t0, -20(s0)
    j if_end_12
if_end_12:
    lw t0, -12(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -12(s0)
    j while_cond_8
while_end_10:
    lw a0, -20(s0)
    j main_epilogue
main_epilogue:
    lw s0, 40(sp)
    lw ra, 44(sp)
    addi sp, sp, 48
    ret

