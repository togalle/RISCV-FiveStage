package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule
import chisel3.util.log2Ceil

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
  val TableSize     = 256
  val BPT           = RegInit(VecInit(Seq.fill(TableSize)(0.U(32.W))))
  val predictionReg = RegInit(0.U(32.W)) // Delay prediction by 1 cycle

  val updateTargetReg = RegInit(0.U(32.W))
  val updateReg       = RegInit(false.B)
  val updatePCReg     = RegInit(0.U(32.W))

  updateReg       := io.update
  updatePCReg     := io.update_PC
  updateTargetReg := io.update_target

  // Prediction
  val indexBits  = log2Ceil(TableSize)
  val index      = io.PC(indexBits + 1, 2) // Word aligned, PC takes jumps of 4
  val index_EX   = io.PC_EX(indexBits + 1, 2)
  val prediction = BPT(index)
  val prediction_EX = BPT(index_EX)

  predictionReg    := prediction
  io.prediction    := predictionReg
  io.prediction_EX := prediction_EX

  // Update BPT
  val update_index = io.update_PC(indexBits + 1, 2)

  when(updateReg) {
    BPT(update_index) := updateTargetReg
  }
}
