package FiveStage
import chisel3._
import chisel3.util._
import chisel3.experimental.MultiIOModule

class ForwardingUnit extends MultiIOModule {
  val io = IO(new Bundle {
    val readData1_in = Input(UInt(32.W))
    val readData2_in = Input(UInt(32.W))

    // Control signals for multiplexer
    val IR_EX              = Input(new Instruction)
    val IR_MEM             = Input(new Instruction)
    val decodedSignals_MEM = Input(new DecodedSignals)
    val IR_WB              = Input(new Instruction)
    val decodedSignals_WB  = Input(new DecodedSignals)
    val EX_decodedSignals  = Input(new DecodedSignals)

    // Inputs for multiplexer
    val PC         = Input(UInt(32.W))
    val rs1        = Input(UInt(32.W))
    val rs2        = Input(UInt(32.W))
    val imm        = Input(SInt(32.W))
    val aluRes_MEM = Input(UInt(32.W))
    val res_WB     = Input(UInt(32.W))

    // Inputs for load-use hazard detection
    val MEM_isLoad = Input(Bool())

    // Output values for ALU
    val aluOp1        = Output(UInt(32.W))
    val aluOp2        = Output(UInt(32.W))
    val readData1_out = Output(UInt(32.W))
    val readData2_out = Output(UInt(32.W))
    val stall         = Output(Bool())
  })

  io.aluOp1 := Mux(
    io.EX_decodedSignals.op1Select === Op1Select.PC,
    io.PC,
    io.rs1
  )
  io.aluOp2 := Mux(
    io.EX_decodedSignals.op2Select === Op2Select.imm,
    io.imm.asUInt,
    io.rs2
  )
  io.stall         := false.B
  io.readData1_out := io.readData1_in
  io.readData2_out := io.readData2_in

  // ALU operand 1
  when(
    io.IR_EX.registerRs1 =/= io.IR_MEM.registerRd &&
      io.IR_EX.registerRs1 =/= io.IR_WB.registerRd
  ) {
    io.aluOp1 := Mux(
      io.EX_decodedSignals.op1Select === Op1Select.PC,
      io.PC,
      io.rs1
    )
    io.readData1_out := io.readData1_in
  }
    .elsewhen(
      io.IR_EX.registerRs1 === io.IR_WB.registerRd &&
        io.IR_EX.registerRs1 =/= io.IR_MEM.registerRd &&
        io.IR_WB.registerRd =/= 0.U &&
        io.decodedSignals_WB.controlSignals.regWrite
    ) {
      io.aluOp1        := io.res_WB
      io.readData1_out := io.res_WB
    }
    .elsewhen(
      io.IR_EX.registerRs1 === io.IR_MEM.registerRd &&
        io.IR_MEM.registerRd =/= 0.U &&
        io.decodedSignals_MEM.controlSignals.regWrite
    ) {
      io.aluOp1        := io.aluRes_MEM
      io.readData1_out := io.aluRes_MEM
    }

  // ALU operand 2
  when(io.EX_decodedSignals.op2Select === Op2Select.imm) {
    io.aluOp2 := io.imm.asUInt
  }.elsewhen(
    io.IR_EX.registerRs2 =/= io.IR_MEM.registerRd &&
      io.IR_EX.registerRs2 =/= io.IR_WB.registerRd
  ) {
    io.aluOp2 := Mux(
      io.EX_decodedSignals.op2Select === Op2Select.imm,
      io.imm.asUInt,
      io.rs2
    )
  }.elsewhen(
    io.IR_EX.registerRs2 === io.IR_WB.registerRd &&
      io.IR_EX.registerRs2 =/= io.IR_MEM.registerRd &&
      io.IR_WB.registerRd =/= 0.U &&
      io.decodedSignals_WB.controlSignals.regWrite
  ) {
    io.aluOp2 := io.res_WB
  }.elsewhen(
    io.IR_EX.registerRs2 === io.IR_MEM.registerRd &&
      io.IR_MEM.registerRd =/= 0.U &&
      io.decodedSignals_MEM.controlSignals.regWrite
  ) {
    io.aluOp2 := io.aluRes_MEM
  }

  val forwardedData2 = Wire(UInt(32.W))
  forwardedData2 := io.readData2_in

  // Apply forwarding logic for store data (similar to aluOp2 logic but specifically for rs2)
  when(
    io.IR_EX.registerRs2 =/= io.IR_MEM.registerRd &&
      io.IR_EX.registerRs2 =/= io.IR_WB.registerRd
  ) {
    forwardedData2 := io.readData2_in
  }.elsewhen(
    io.IR_EX.registerRs2 === io.IR_WB.registerRd &&
      io.IR_EX.registerRs2 =/= io.IR_MEM.registerRd &&
      io.decodedSignals_WB.controlSignals.regWrite &&
      io.IR_WB.registerRd =/= 0.U
  ) {
    forwardedData2 := io.res_WB
  }.elsewhen(
    io.IR_EX.registerRs2 === io.IR_MEM.registerRd &&
      io.IR_MEM.registerRd =/= 0.U &&
      io.decodedSignals_MEM.controlSignals.regWrite
  ) {
    forwardedData2 := io.aluRes_MEM
  }

  // Use the forwarded value for store data
  io.readData2_out := forwardedData2

  // Check for load-use hazard
  when(
    io.MEM_isLoad &&
      (io.IR_MEM.registerRd === io.IR_EX.registerRs1 || io.IR_MEM.registerRd === io.IR_EX.registerRs2) &&
      io.IR_MEM.registerRd =/= 0.U
  ) {
    io.stall := true.B
  }
}
