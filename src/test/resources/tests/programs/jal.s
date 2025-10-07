main:
  addi x2, x0, -4
  addi x5, x0, 0
  jal ra, l1
  addi x5, x5, 1
l1:
  jalr ra, x2, l2
  addi x5, x5, 1
  addi x5, x5, 1
l2:
  addi x6, x5, 1
  addi x6, x5, 1
  done