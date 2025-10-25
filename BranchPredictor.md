# Branch predictor

This CPU uses an equivalent of a 1-bit branch predictor. However, it's not explicitly implemented using this bit. Instead, the BPT is a list of target values, all initiated to 0. When the address is 0, it's equivalent to predicting not taken. When the value is not zero, it's an address and it's predicted as taken.

The BP has 3 parts: prediction, update, and pipeline flushing.

Prediction is simply connecting the IF to the BP and delaying the outgoing prediction by one cycle using a register.

Update is done in EX. First it is checked if the current instruction is a branch or a jump, and then it checks if the branch was taken or not to update the target prediction to either 0 or the appropriate address.

The branch predictor flushes the pipeline based on the following conditions:

1. Check if the instruction is a **jump or branch**:
   - `io.controlSignals.jump || io.controlSignals.branch`

2. If true, evaluate the following conditions:
   - **Case 1**: The branch is predicted as taken, but it was not actually taken:
     - `!alu.io.branchTaken && io.BP_prediction =/= 0.U`
   - **Case 2**: The branch is predicted as not taken, but it was actually taken, or it was predicted to an incorrect target:
     - `alu.io.branchTaken && io.BP_prediction =/= pc_calculator.io.PC_out`

3. If either condition is true, **flush** the pipeline.
