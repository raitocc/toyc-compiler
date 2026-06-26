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
while_cond_1:
    lw t0, -16(s0)
    li t1, 0
    sub t2, t0, t1
    snez t2, t2
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, while_end_3
while_body_2:
    lw t0, -16(s0)
    sw t0, -28(s0)
    lw t0, -12(s0)
    lw t1, -16(s0)
    rem t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -16(s0)
    lw t0, -28(s0)
    sw t0, -12(s0)
    j while_cond_1
while_end_3:
    lw a0, -12(s0)
    j gcd_epilogue
gcd_epilogue:
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

    .globl main
main:
    addi sp, sp, -32
    sw ra, 28(sp)
    sw s0, 24(sp)
    addi s0, sp, 32


entry_4:
    li t0, 0
    sw t0, -12(s0)
    li t0, 0
    sw t0, -16(s0)
while_cond_5:
    lw t0, -12(s0)
    li t1, 1000000
    slt t2, t0, t1
    sw t2, -24(s0)
    lw t0, -24(s0)
    beq t0, zero, while_end_7
while_body_6:
    li t0, 48
    mv a0, t0
    li t0, 18
    mv a1, t0
    call gcd
    sw a0, -28(s0)
    lw t0, -16(s0)
    lw t1, -28(s0)
    add t2, t0, t1
    sw t2, -32(s0)
    lw t0, -32(s0)
    sw t0, -16(s0)
    lw t0, -12(s0)
    li t1, 1
    add t2, t0, t1
    sw t2, -20(s0)
    lw t0, -20(s0)
    sw t0, -12(s0)
    j while_cond_5
while_end_7:
    lw a0, -16(s0)
    j main_epilogue
main_epilogue:
    lw s0, 24(sp)
    lw ra, 28(sp)
    addi sp, sp, 32
    ret

