main:
    addi	sp,sp,-48
	sw	s0,44(sp)
	addi	s0,sp,48
    li a0,11
    li a1,5
    li a5,4
    li t0,7

    sw t0,-20(s0)
	sw	a0,-36(s0)
	sw	a1,-40(s0)
	lw	a5,-20(s0)
	slli	a5,a5,2
	lw	a4,-40(s0)
	add	a5,a4,a5
    done