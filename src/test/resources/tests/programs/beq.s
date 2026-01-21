main:
    li x1, 3
    li x2, 1
    bgeu x1, x2, label_taken_1
    li x3, 0
    done
    nop
label_taken_1:
    li x3, 1

    li x1, 2
    li x2, 5
    bgeu x1, x2, label_not_taken_1
    li x3, 2
    done
    nop
label_not_taken_1:
    li x3, 3
    done
    nop
    li x1, 4
    li x2, 4
    bgeu x1, x2, label_taken_2
    li x3, 0
    done
    nop
label_taken_2:
    li x3, 4

    li x1, 1
    li x2, 2
    bgeu x1, x2, label_not_taken_2
    li x3, 5
    done
    nop
label_not_taken_2:
    li x3, 6
    done
    nop
    li x1, 0
    li x2, 5
loop:
    addi x1, x1, 1
    bgeu x1, x2, loop_end
    li x3, 7
    j loop

loop_end:
    li x3, 8
    done
