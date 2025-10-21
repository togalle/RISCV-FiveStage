package FiveStage
import chisel3._
import chisel3.util._
import chisel3.experimental.MultiIOModule

class Execute extends MultiIOModule {
  val io = IO(new Bundle {
    // Inputs from ID stage
    val PC                = Input(UInt(32.W))
    val readData1         = Input(UInt(32.W))
    val readData2         = Input(UInt(32.W))
    val decodedSignals_in = Input(new DecodedSignals)
    val immediate         = Input(SInt(32.W))
    val aluOp1            = Input(UInt(32.W))
    val aluOp2            = Input(UInt(32.W))

    // Outputs to MEM stage
    val aluResult          = Output(UInt(32.W))
    val PC_out             = Output(UInt(32.W))
    val readData2_out      = Output(UInt(32.W))
    val decodedSignals_out = Output(new DecodedSignals)
    val branchTaken        = Output(Bool())
    val debugFlag          = Output(Bool())
  })

  val alu           = Module(new ALU)
  val pc_calculator = Module(new PC_Calculator)
  io.debugFlag := false.B

  val op1 = io.aluOp1
  val op2 = io.aluOp2

  // Connect ALU
  alu.io.op1        := op1
  alu.io.op2        := op2
  alu.io.aluOp      := io.decodedSignals_in.ALUop
  alu.io.branchType := io.decodedSignals_in.branchType

  // Connect PC Calculator
  pc_calculator.io.PC             := io.PC
  pc_calculator.io.immediate      := io.immediate
  pc_calculator.io.branchTaken    := alu.io.branchTaken
  pc_calculator.io.controlSignals := io.decodedSignals_in.controlSignals
  pc_calculator.io.readData1      := io.readData1

  // Outputs
  io.aluResult := Mux(
    io.decodedSignals_in.branchType === branchType.jump,
    io.PC + 4.U,
    alu.io.result
  )
  io.readData2_out      := io.readData2
  io.PC_out             := pc_calculator.io.PC_out
  io.decodedSignals_out := io.decodedSignals_in
  when(!alu.io.branchTaken) {
    io.decodedSignals_out.controlSignals.branch := false.B
    io.decodedSignals_out.controlSignals.jump   := false.B
  }
  io.branchTaken := alu.io.branchTaken && (io.decodedSignals_in.controlSignals.branch || io.decodedSignals_in.controlSignals.jump)
}

class ALU extends MultiIOModule {
  val io = IO(new Bundle {
    val op1        = Input(UInt(32.W))
    val op2        = Input(UInt(32.W))
    val aluOp      = Input(UInt(4.W))
    val branchType = Input(UInt(3.W))

    val result      = Output(UInt(32.W))
    val branchTaken = Output(Bool())
  })

  import lookup._

  val ALUopMap = Array(
    ALUOps.ADD    -> (io.op1 + io.op2),
    ALUOps.SUB    -> (io.op1 - io.op2),
    ALUOps.AND    -> (io.op1 & io.op2),
    ALUOps.OR     -> (io.op1 | io.op2),
    ALUOps.XOR    -> (io.op1 ^ io.op2),
    ALUOps.SLT    -> (io.op1.asSInt < io.op2.asSInt).asUInt,
    ALUOps.SLL    -> (io.op1 << io.op2(4, 0)),
    ALUOps.SLTU   -> (io.op1 < io.op2).asUInt,
    ALUOps.SRL    -> (io.op1 >> io.op2(4, 0)),
    ALUOps.SRA    -> (io.op1.asSInt >> io.op2(4, 0)).asUInt,
    ALUOps.COPY_A -> io.op1,
    ALUOps.COPY_B -> io.op2,
    ALUOps.DC     -> 0.U
  )

  val branchMap = Array(
    branchType.beq  -> (io.op1 === io.op2),
    branchType.neq  -> (io.op1 =/= io.op2),
    branchType.gte  -> (io.op1.asSInt >= io.op2.asSInt),
    branchType.lt   -> (io.op1.asSInt < io.op2.asSInt),
    branchType.gteu -> (io.op1 >= io.op2),
    branchType.ltu  -> (io.op1 < io.op2),
    branchType.jump -> true.B,
    branchType.DC   -> false.B
  )

  io.result      := MuxLookup(io.aluOp, 0.U(32.W), ALUopMap)
  io.branchTaken := MuxLookup(io.branchType, false.B, branchMap)
}

class PC_Calculator extends MultiIOModule {
  val io = IO(new Bundle {
    val PC             = Input(UInt(32.W))
    val immediate      = Input(SInt(32.W))
    val branchTaken    = Input(Bool())
    val controlSignals = Input(new ControlSignals)
    val readData1      = Input(UInt(32.W))

    val PC_out = Output(UInt(32.W))
  })

  // Default output
  io.PC_out := io.PC + 4.U

  when(io.controlSignals.jump) {
    when(io.controlSignals.branch) {
      // JALR
      io.PC_out := (io.readData1 + io.immediate.asUInt) & "h_fffffffe".U
    }.otherwise {
      // JAL
      io.PC_out := io.PC + io.immediate.asUInt
    }
  }.elsewhen(io.controlSignals.branch && io.branchTaken) {
    // Branch taken
    io.PC_out := io.PC + io.immediate.asUInt
  }.otherwise {
    // PC + 4
    io.PC_out := io.PC + 4.U
  }
}
