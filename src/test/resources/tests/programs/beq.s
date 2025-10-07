# Test bgeu: branch should be taken (x1 >= x2, unsigned)
main:
    li x1, 3
    li x2, 1
    bgeu x1, x2, label_taken
    li x3, 0      # Should NOT execute
    done

label_taken:
    li x3, 1      # Should execute (branch taken)
    done

# Test bgeu: branch should NOT be taken (x1 < x2, unsigned)
main2:
    li x1, 2
    li x2, 5
    bgeu x1, x2, label_not_taken
    li x3, 2      # Should execute (branch not taken)
    done

label_not_taken:
    li x3, 3      # Should NOT execute
    done
