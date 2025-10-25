package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule
import chisel3.util.log2Ceil

/*
  When the output prediction is 0, it indicates not taken (PC + 4), and taken when otherwise.
  BPT updates by writing the target address if taken, or 0 if not taken.

  Ensure branching from the EX stage is prioritized!
 */

class BranchPredictor extends MultiIOModule {
  val io = IO(new Bundle {
    // IF Input
    val PC    = Input(UInt(32.W))
    val PC_EX = Input(UInt(32.W))

    // EX Inputs
    val update        = Input(Bool())
    val update_PC     = Input(UInt(32.W))
    val update_target = Input(UInt(32.W))

    val prediction    = Output(UInt(32.W))
    val prediction_EX = Output(UInt(32.W))
  })

  // BPT setup
  val TableSize     = 1024
  val BPT           = RegInit(VecInit(Seq.fill(TableSize)(0.U(32.W))))
  val predictionReg = RegInit(0.U(32.W)) // Delay prediction by 1 cycle

  // Prediction
  val indexBits     = log2Ceil(TableSize)
  val index         = io.PC(indexBits + 1, 2)
  val index_EX      = io.PC_EX(indexBits + 1, 2)
  val prediction    = BPT(index)
  val prediction_EX = BPT(index_EX)

  predictionReg    := prediction
  io.prediction    := predictionReg
  io.prediction_EX := prediction_EX

  // Update BPT
  val update_index = io.update_PC(indexBits + 1, 2)

  when(io.update) {
    BPT(update_index) := io.update_target
  }
}
