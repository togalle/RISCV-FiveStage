package FiveStage
import chisel3._
import chisel3.util.{ BitPat, MuxCase }
import chisel3.experimental.MultiIOModule
import ImmFormat._

class InstructionDecode extends MultiIOModule {

  // Don't touch the test harness
  val testHarness = IO(
    new Bundle {
      val registerSetup = Input(new RegisterSetupSignals)
      val registerPeek  = Output(UInt(32.W))

      val testUpdates   = Output(new RegisterUpdates)
    })


  val io = IO(
    new Bundle {
      val instruction_in             = Input(new Instruction)
      // WB
      val WB_aluResult_in               = Input(UInt(32.W))
      val WB_decodedSignals_in         = Input(new DecodedSignals)
      val WB_instruction_in            = Input(new Instruction)
      // Load-use hazard detection
      val EX_memRead_in                 = Input(Bool())
      val EX_registerRd_in              = Input(UInt(5.W))

      val decodedSignals_out         = Output(new DecodedSignals)
      val readData1_out              = Output(UInt(32.W))
      val readData2_out              = Output(UInt(32.W))
      val instruction_out            = Output(new Instruction)
      val immediate_out              = Output(SInt(32.W))
    }
  )

  val registers = Module(new Registers)
  val decoder   = Module(new Decoder).io

  /**
    * Setup. You should not change this code
    */
  registers.testHarness.setup := testHarness.registerSetup
  testHarness.registerPeek    := registers.io.readData1
  testHarness.testUpdates     := registers.testHarness.testUpdates

  // Connect registers
  registers.io.readAddress1 := io.instruction_in.registerRs1
  registers.io.readAddress2 := io.instruction_in.registerRs2

  // If WB writes to register that's not used, connect as normal
  registers.io.writeEnable  := io.WB_decodedSignals_in.controlSignals.regWrite
  registers.io.writeAddress := io.WB_instruction_in.registerRd
  registers.io.writeData    := io.WB_aluResult_in

  io.readData1_out := registers.io.readData1
  io.readData2_out := registers.io.readData2

  // Connect instruction
  decoder.instruction := io.instruction_in
  io.instruction_out := io.instruction_in

  // Connect decoder bundle
  io.decodedSignals_out := decoder.decodedSignals

  // Connect immediate (sign-extended based on the instruction and the decoder.immType)
  io.immediate_out := MuxCase(0.S, Array(
    (decoder.decodedSignals.immType === ITYPE) -> io.instruction_in.immediateIType.asSInt,
    (decoder.decodedSignals.immType === STYPE) -> io.instruction_in.immediateSType.asSInt,
    (decoder.decodedSignals.immType === BTYPE) -> io.instruction_in.immediateBType.asSInt,
    (decoder.decodedSignals.immType === UTYPE) -> io.instruction_in.immediateUType.asSInt,
    (decoder.decodedSignals.immType === JTYPE) -> io.instruction_in.immediateJType.asSInt,
    (decoder.decodedSignals.immType === DC)    -> 0.S
  ))

  // Check if forwarding from WB to ID is needed
  when (io.WB_decodedSignals_in.controlSignals.regWrite &&
        (io.WB_instruction_in.registerRd =/= 0.U) && // Don't forward if writing to x0
        (io.WB_instruction_in.registerRd === io.instruction_in.registerRs1)) {
    io.readData1_out := io.WB_aluResult_in
  }

  when (io.WB_decodedSignals_in.controlSignals.regWrite &&
        (io.WB_instruction_in.registerRd =/= 0.U) && // Don't forward if writing to x0
        (io.WB_instruction_in.registerRd === io.instruction_in.registerRs2)) {
    // Forward to readData2_out
    io.readData2_out := io.WB_aluResult_in
  }
}
