    .text

    .globl is_prime
is_prime:
    addi sp, sp, -48
    sw ra, 44(sp)
    sw s0, 40(sp)
    addi s0, sp, 48

    sw a0, -12(s0)

entry_0:
    li t0, 2
    sw t0, -16(s0)
while_cond_1:
    lw t0, -16(s0)
    lw t1, -16(s0)
    mul t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    lw t1, -12(s0)
    slt t2, t1, t0
    xori t2, t2, 1
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -12(s0)
    lw t1, -16(s0)
    rem t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    li t1, 0
    sub t2, t0, t1
    seqz t2, t2
    sw t2, -32(s0)
    lw t0, -32(s0)
    beq t0, zero, if_end_5
if_then_4:
    li a0, 0
    j is_prime_epilogue
    j if_end_5
if_end_5:
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -36(s0)
    lw t0, -36(s0)
    sw t0, -16(s0)
    j while_cond_1
while_end_3:
    li a0, 1
    j is_prime_epilogue
is_prime_epilogue:
    lw ra, 44(sp)
    lw s0, 40(sp)
    addi sp, sp, 48
    ret

    .globl main
main:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32


entry_6:
    li t0, 0
    sw t0, -12(s0)
    li t0, 2
    sw t0, -16(s0)
while_cond_7:
    lw t0, -16(s0)
    li t1, 200000
    slt t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    beq t0, zero, while_end_9
while_body_8:
    lw t0, -16(s0)
    mv a0, t0
    call is_prime
    sw a0, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, if_end_11
if_then_10:
    lw t0, -12(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -28(s0)
    lw t0, -28(s0)
    sw t0, -12(s0)
    j if_end_11
if_end_11:
    lw t0, -16(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -32(s0)
    lw t0, -32(s0)
    sw t0, -16(s0)
    j while_cond_7
while_end_9:
    lw a0, -12(s0)
    j main_epilogue
main_epilogue:
    lw ra, 28(sp)
    lw s0, 24(sp)
    addi sp, sp, 32
    ret

