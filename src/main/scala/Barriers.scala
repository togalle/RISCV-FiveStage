package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class IFIDBarrier extends MultiIOModule {
  
  val io = IO(new Bundle {
    // Inputs from IF stage
    val PC_in          = Input(UInt(32.W))
    val instruction_in = Input(new Instruction)
    val stall          = Input(Bool())
    
    // Outputs to ID stage  
    val PC_out          = Output(UInt(32.W))
    val instruction_out = Output(new Instruction)
  })
  
  // PC is delayed by one cycle using a register
  val PC_reg = RegInit(UInt(32.W), 0.U)
  when (!io.stall) {
    PC_reg := io.PC_in
  } .otherwise {
    PC_reg := PC_reg
  }
  io.PC_out := PC_reg
  
  // Instruction passes through without delay (since IMEM already adds delay)
  val instruction_reg = RegInit(0.U.asTypeOf(new Instruction))
  when (!io.stall) {
    instruction_reg := io.instruction_in
    io.instruction_out := io.instruction_in
  } .otherwise {
    io.instruction_out := instruction_reg
  }
}

class IDEXBarrier extends MultiIOModule {
  val io = IO(new Bundle {
    // Inputs from ID stage
    val PC_in                     = Input(UInt(32.W))
    val decodedSignals_in         = Input(new DecodedSignals)
    val readData1_in              = Input(UInt(32.W))
    val readData2_in              = Input(UInt(32.W))
    val instruction_in            = Input(new Instruction)
    val immediate_in              = Input(SInt(32.W))
    val stall                     = Input(Bool())
    val flush                     = Input(Bool())

    val WB_Rd                     = Input(UInt(5.W))
    val WB_RegWrite               = Input(Bool())
    val WB_Data                   = Input(UInt(32.W))

    // Outputs for EX stage
    val PC_out                     = Output(UInt(32.W))
    val decodedSignals_out         = Output(new DecodedSignals)
    val readData1_out              = Output(UInt(32.W))
    val readData2_out              = Output(UInt(32.W))
    val instruction_out            = Output(new Instruction)
    val immediate_out              = Output(SInt(32.W))
  })

  // All values have to be stored in registers
  val PC_reg = RegInit(UInt(32.W), 0.U)
  val decodedSignals_reg = RegInit(0.U.asTypeOf(new DecodedSignals))
  val readData1_reg = RegInit(UInt(32.W), 0.U)
  val readData2_reg = RegInit(UInt(32.W), 0.U)
  val instruction_reg = RegInit(0.U.asTypeOf(new Instruction))
  val immediate_reg = RegInit(0.S)

  when (!io.stall) {
    PC_reg := io.PC_in
    decodedSignals_reg := io.decodedSignals_in
    readData1_reg := io.readData1_in
    readData2_reg := io.readData2_in
    instruction_reg := io.instruction_in
    immediate_reg := io.immediate_in
  } .otherwise {
    PC_reg := PC_reg
    decodedSignals_reg := decodedSignals_reg
    readData1_reg := readData1_reg
    readData2_reg := readData2_reg
    instruction_reg := instruction_reg
    immediate_reg := immediate_reg

    // TODO: check if instruction reg should be used or io.instruction_in

    // If there is a stall, check if the Rd should be updated from the WB stage
    when (io.WB_Rd === instruction_reg.registerRs1 &&
          io.WB_RegWrite &&
          io.WB_Rd =/= 0.U) {
      readData1_reg := io.WB_Data
    }
    when (io.WB_Rd === instruction_reg.registerRs2 &&
          io.WB_RegWrite &&
          io.WB_Rd =/= 0.U) {
      readData2_reg := io.WB_Data
    }
  }

  // Connect all registers with the outputs
  io.PC_out := PC_reg
  io.decodedSignals_out := decodedSignals_reg
  io.readData1_out := readData1_reg
  io.readData2_out := readData2_reg
  io.instruction_out := instruction_reg
  io.immediate_out := immediate_reg

    when (io.flush && io.PC_in =/= 0.U) {
    // Insert bubble on flush
    // decodedSignals_reg := 0.U.asTypeOf(new DecodedSignals)
    // decodedSignals_reg.branchType := branchType.DC
    // instruction_reg := Instruction.NOP

    io.decodedSignals_out := 0.U.asTypeOf(new DecodedSignals)
    io.decodedSignals_out.branchType := branchType.DC
    io.instruction_out := Instruction.NOP
  }
}

class EXMEMBarrier extends MultiIOModule {
  val io = IO(new Bundle {
    // Inputs from EX stage
    val PC_in                     = Input(UInt(32.W))
    val decodedSignals_in         = Input(new DecodedSignals)
    val aluResult_in              = Input(UInt(32.W))
    val readData2_in              = Input(UInt(32.W))
    val instruction_in            = Input(new Instruction)
    val stall                     = Input(Bool())
    val flush_in                  = Input(Bool())

    // Outputs for MEM stage
    val PC_out                     = Output(UInt(32.W))
    val decodedSignals_out         = Output(new DecodedSignals)
    val aluResult_out              = Output(UInt(32.W))
    val readData2_out              = Output(UInt(32.W))
    val instruction_out            = Output(new Instruction)
    val flush_out                  = Output(Bool())
  })

  // All values have to be stored in registers
  val PC_reg = RegInit(UInt(32.W), 0.U)
  val decodedSignals_reg = RegInit(0.U.asTypeOf(new DecodedSignals))
  val aluResult_reg = RegInit(UInt(32.W), 0.U)
  val readData2_reg = RegInit(UInt(32.W), 0.U)
  val instruction_reg = RegInit(0.U.asTypeOf(new Instruction))
  val flush_reg = RegInit(false.B)

  // Connect all inputs with the registers
  when (!io.stall) {
    PC_reg := io.PC_in
    decodedSignals_reg := io.decodedSignals_in
    aluResult_reg := io.aluResult_in
    readData2_reg := io.readData2_in
    instruction_reg := io.instruction_in
    flush_reg := io.flush_in
  } .otherwise {
    // Insert bubble on stall
    PC_reg := 0.U
    decodedSignals_reg := 0.U.asTypeOf(new DecodedSignals)
    decodedSignals_reg.branchType := branchType.DC
    decodedSignals_reg.controlSignals := ControlSignals.nop
    // set the branchtaken to false when stalling
    
    aluResult_reg := 0.U
    readData2_reg := 0.U
    instruction_reg := Instruction.NOP
    flush_reg := false.B
  }

  // Connect all registers with the outputs
  io.PC_out := PC_reg
  io.decodedSignals_out := decodedSignals_reg
  io.aluResult_out := aluResult_reg
  io.readData2_out := readData2_reg
  io.instruction_out := instruction_reg
  io.flush_out := flush_reg
}

class MEMWBBarrier extends MultiIOModule {
  val io = IO(new Bundle {
    // Inputs from MEM stage
    val decodedSignals_in         = Input(new DecodedSignals)
    val aluResult_in              = Input(UInt(32.W))
    val instruction_in            = Input(new Instruction)
    val DMEMData_in               = Input(UInt(32.W))

    // Outputs for WB stage
    val decodedSignals_out         = Output(new DecodedSignals)
    val aluResult_out              = Output(UInt(32.W))
    val DMEMData_out               = Output(UInt(32.W))
    val instruction_out            = Output(new Instruction)
  })

  // All values have to be stored in registers
  val decodedSignals_reg = RegInit(0.U.asTypeOf(new DecodedSignals))
  val aluResult_reg = RegInit(UInt(32.W), 0.U)
  val instruction_reg = RegInit(0.U.asTypeOf(new Instruction))

  // Connect all inputs with the registers
  decodedSignals_reg := io.decodedSignals_in
  aluResult_reg := io.aluResult_in
  instruction_reg := io.instruction_in

  // Connect all registers with the outputs
  io.decodedSignals_out := decodedSignals_reg
  io.aluResult_out := aluResult_reg
  io.DMEMData_out := io.DMEMData_in
  io.instruction_out := instruction_reg
}