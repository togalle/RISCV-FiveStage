package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class InstructionFetch extends MultiIOModule {

  // Don't touch
  val testHarness = IO(
    new Bundle {
      val IMEMsetup = Input(new IMEMsetupSignals)
      val PC        = Output(UInt())
    }
  )

  val io = IO(new Bundle {
    // PC control
    val controlSignals = Input(new ControlSignals)
    val PC_in          = Input(UInt(32.W))
    val stall          = Input(Bool())
    val flush          = Input(Bool())

    // Branch Predictor
    val prediction = Input(UInt(32.W))

    val PC          = Output(UInt())
    val instruction = Output(new Instruction)
  })

  val IMEM = Module(new IMEM)
  val PC   = RegInit(UInt(32.W), 0.U)

  /** Setup. You should not change this code
    */
  IMEM.testHarness.setupSignals := testHarness.IMEMsetup
  testHarness.PC                := IMEM.testHarness.requestedAddress

  when(!io.stall) {
    PC := Mux(
      io.controlSignals.jump || io.controlSignals.branch,
      io.PC_in,
      Mux(io.prediction =/= 0.U, io.prediction, PC + 4.U)
    )
  }.otherwise {
    PC := PC
  }
  io.PC := PC

  val instruction = Wire(new Instruction)
  instruction := IMEM.io.instruction.asTypeOf(new Instruction)

  val lastInstr   = RegInit(0.U.asTypeOf(new Instruction))
  val laggedStall = RegInit(Bool(), false.B)
  laggedStall := io.stall

  val flush_reg = RegInit(false.B)
  flush_reg := io.flush

  // When flush is asserted, output NOP on next cycle
  when(io.flush || flush_reg) {
    io.instruction := Instruction.NOP
    lastInstr      := Instruction.NOP
  }.elsewhen(!laggedStall) {
    io.instruction := instruction
    lastInstr      := instruction
  }.otherwise {
    io.instruction := lastInstr
  }
  IMEM.io.instructionAddress := PC

  /** Setup. You should not change this code.
    */
  when(testHarness.IMEMsetup.setup) {
    PC          := 0.U
    instruction := Instruction.NOP
  }
}
