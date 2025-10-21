package FiveStage

import chisel3._
import chisel3.core.Input
import chisel3.experimental.MultiIOModule
import chisel3.experimental._


class CPU extends MultiIOModule {

  val testHarness = IO(
    new Bundle {
      val setupSignals = Input(new SetupSignals)
      val testReadouts = Output(new TestReadouts)
      val regUpdates   = Output(new RegisterUpdates)
      val memUpdates   = Output(new MemUpdates)
      val currentPC    = Output(UInt(32.W))
    }
  )

  /**
    You need to create the classes for these yourself
    */
  val IFIDBarrier  = Module(new IFIDBarrier).io
  val IDEXBarrier  = Module(new IDEXBarrier).io
  val EXMEMBarrier  = Module(new EXMEMBarrier).io
  val MEMWBBarrier = Module(new MEMWBBarrier).io

  val ID  = Module(new InstructionDecode)
  val IF  = Module(new InstructionFetch)
  val EX  = Module(new Execute)
  val FU  = Module(new ForwardingUnit)
  val MEM = Module(new MemoryFetch)
  // val WB  = Module(new Execute) (You may not need this one?)

  // Sign extend the immediate and connect to output


  /**
    * Setup. You should not change this code
    */
  IF.testHarness.IMEMsetup     := testHarness.setupSignals.IMEMsignals
  ID.testHarness.registerSetup := testHarness.setupSignals.registerSignals
  MEM.testHarness.DMEMsetup    := testHarness.setupSignals.DMEMsignals

  testHarness.testReadouts.registerRead := ID.testHarness.registerPeek
  testHarness.testReadouts.DMEMread     := MEM.testHarness.DMEMpeek

  /**
    spying stuff
    */
  testHarness.regUpdates := ID.testHarness.testUpdates
  testHarness.memUpdates := MEM.testHarness.testUpdates
  testHarness.currentPC  := IF.testHarness.PC

  // IF/ID
  IF.io.stall                 := FU.io.stall
  IF.io.flush                 := EXMEMBarrier.flush_out
  IFIDBarrier.PC_in           := IF.io.PC
  IFIDBarrier.instruction_in  := IF.io.instruction
  IFIDBarrier.stall := FU.io.stall

  ID.io.instruction_in := IFIDBarrier.instruction_out
  
  // ID/EX
  IDEXBarrier.PC_in                     := IFIDBarrier.PC_out
  IDEXBarrier.decodedSignals_in         := ID.io.decodedSignals_out
  IDEXBarrier.readData1_in              := ID.io.readData1_out
  IDEXBarrier.readData2_in              := ID.io.readData2_out
  IDEXBarrier.instruction_in            := ID.io.instruction_out
  IDEXBarrier.immediate_in              := ID.io.immediate_out
  IDEXBarrier.stall                     := FU.io.stall
  IDEXBarrier.flush                     := EXMEMBarrier.flush_out

  IDEXBarrier.WB_Rd                     := MEMWBBarrier.instruction_out.registerRd
  IDEXBarrier.WB_RegWrite               := MEMWBBarrier.decodedSignals_out.controlSignals.regWrite
  IDEXBarrier.WB_Data                   := Mux(
    MEMWBBarrier.decodedSignals_out.controlSignals.memRead,
    MEMWBBarrier.DMEMData_out,
    MEMWBBarrier.aluResult_out
  )

  // Forwarding Unit
  FU.io.IR_EX               := IDEXBarrier.instruction_out
  FU.io.IR_MEM              := EXMEMBarrier.instruction_out
  FU.io.IR_WB               := MEMWBBarrier.instruction_out
  FU.io.EX_decodedSignals   := IDEXBarrier.decodedSignals_out
  FU.io.PC                  := IDEXBarrier.PC_out
  FU.io.rs1                 := IDEXBarrier.readData1_out
  FU.io.rs2                 := IDEXBarrier.readData2_out
  FU.io.imm                 := IDEXBarrier.immediate_out
  FU.io.aluRes_MEM          := EXMEMBarrier.aluResult_out
  FU.io.readData1_in        := IDEXBarrier.readData1_out
  FU.io.readData2_in        := IDEXBarrier.readData2_out
  FU.io.MEM_isLoad          := EXMEMBarrier.decodedSignals_out.controlSignals.memRead
  FU.io.res_WB              := Mux(
    MEMWBBarrier.decodedSignals_out.controlSignals.memRead,
    MEMWBBarrier.DMEMData_out,
    MEMWBBarrier.aluResult_out
  )
  FU.io.decodedSignals_MEM  := EXMEMBarrier.decodedSignals_out
  FU.io.decodedSignals_WB   := MEMWBBarrier.decodedSignals_out

  EX.io.PC                      := IDEXBarrier.PC_out
  EX.io.decodedSignals_in       := IDEXBarrier.decodedSignals_out
  EX.io.immediate               := IDEXBarrier.immediate_out
  EX.io.aluOp1                  := FU.io.aluOp1
  EX.io.aluOp2                  := FU.io.aluOp2
  EX.io.readData1               := FU.io.readData1_out
  EX.io.readData2               := FU.io.readData2_out

  // Load-use hazard detection
  ID.io.EX_memRead_in     := IDEXBarrier.decodedSignals_out.controlSignals.memRead
  ID.io.EX_registerRd_in  := IDEXBarrier.instruction_out.registerRd

  // EX/MEM
  EXMEMBarrier.PC_in                     := EX.io.PC_out
  EXMEMBarrier.decodedSignals_in         := EX.io.decodedSignals_out
  EXMEMBarrier.aluResult_in              := EX.io.aluResult
  EXMEMBarrier.readData2_in              := EX.io.readData2_out
  EXMEMBarrier.flush_in                  := EX.io.branchTaken
  EXMEMBarrier.instruction_in            := IDEXBarrier.instruction_out
  EXMEMBarrier.stall                     := FU.io.stall

  MEM.io.decodedSignals_in  := EXMEMBarrier.decodedSignals_out
  MEM.io.aluResult_in       := EXMEMBarrier.aluResult_out
  MEM.io.readData2_in       := EXMEMBarrier.readData2_out
  MEM.io.instruction_in     := EXMEMBarrier.instruction_out
  IF.io.PC_in               := EXMEMBarrier.PC_out

  // MEM/WB
  MEMWBBarrier.decodedSignals_in  := MEM.io.decodedSignals_out
  MEMWBBarrier.aluResult_in       := MEM.io.aluResult_out
  MEMWBBarrier.instruction_in     := MEM.io.instruction_out
  MEMWBBarrier.DMEMData_in        := MEM.io.DMEMData_out

  ID.io.WB_aluResult_in := Mux(
    MEMWBBarrier.decodedSignals_out.controlSignals.memRead,
    MEMWBBarrier.DMEMData_out,
    MEMWBBarrier.aluResult_out
  )
  ID.io.WB_decodedSignals_in           := MEMWBBarrier.decodedSignals_out
  ID.io.WB_instruction_in              := MEMWBBarrier.instruction_out

  IF.io.controlSignals  := EXMEMBarrier.decodedSignals_out.controlSignals
  IF.io.PC_in           := EXMEMBarrier.PC_out

}
