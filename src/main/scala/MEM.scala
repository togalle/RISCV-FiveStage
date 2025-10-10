package FiveStage
import chisel3._
import chisel3.util._
import chisel3.experimental.MultiIOModule


class MemoryFetch() extends MultiIOModule {


  // Don't touch the test harness
  val testHarness = IO(
    new Bundle {
      val DMEMsetup      = Input(new DMEMsetupSignals)
      val DMEMpeek       = Output(UInt(32.W))

      val testUpdates    = Output(new MemUpdates)
    })

  val io = IO(
    new Bundle {
      val decodedSignals_in = Input(new DecodedSignals)
      val aluResult_in     = Input(UInt(32.W))
      val readData2_in     = Input(UInt(32.W))
      val instruction_in   = Input(new Instruction)

      val decodedSignals_out = Output(new DecodedSignals)
      val aluResult_out    = Output(UInt(32.W))
      val instruction_out  = Output(new Instruction)
      val DMEMData_out     = Output(UInt(32.W))
    })


  val DMEM = Module(new DMEM)


  /**
    * Setup. You should not change this code
    */
  DMEM.testHarness.setup  := testHarness.DMEMsetup
  testHarness.DMEMpeek    := DMEM.io.dataOut
  testHarness.testUpdates := DMEM.testHarness.testUpdates

  DMEM.io.writeEnable := io.decodedSignals_in.controlSignals.memWrite
  DMEM.io.dataAddress := io.aluResult_in(11, 0)
  DMEM.io.dataIn      := io.readData2_in
  io.DMEMData_out := DMEM.io.dataOut

  // Connect inputs to outputs
  io.decodedSignals_out := io.decodedSignals_in
  io.aluResult_out      := io.aluResult_in
  io.instruction_out    := io.instruction_in

  // when reading from memory, print the address and data
      // printf(p"MEM: address 0x${Hexadecimal(io.aluResult_in)} data 0x${Hexadecimal(DMEM.io.dataOut)}\n")

}
