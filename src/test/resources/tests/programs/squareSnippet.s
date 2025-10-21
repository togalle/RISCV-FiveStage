main:
addi sp, sp, -48
sw ra, 44(sp)
sw s0, 40(sp)
addi s0, sp, 48
li a4, 0
li a5, 5
sw a4, -24(s0)
sw a5, -36(s0)

.L2:
lw a4, -24(s0)
lw a5, -36(s0)
blt a4, a5, .L3
lw a5, -20(s0)
mv a0, a5
lw s0, 44(sp)
addi sp, sp, 48
jr ra

.L3:
lw a4, -20(s0)
lw a5, -40(s0)
add a5, a4, a5
sw a5, -20(s0)
lw a5, -24(s0)
addi a5, a5, 1
sw a5, -24(s0)
j .L2