package FiveStage
import chisel3._
import chisel3.util.BitPat
import chisel3.util.ListLookup


/**
  * This module is mostly done, but you will have to fill in the blanks in opcodeMap.
  * You may want to add more signals to be decoded in this module depending on your
  * design if you so desire.
  *
  * In the "classic" 5 stage decoder signals such as op1select and immType
  * are not included, however I have added them to my design, and similarily you might
  * find it useful to add more
 */
class Decoder() extends Module {

  val io = IO(new Bundle {
                val instruction    = Input(new Instruction)

                val decodedSignals = Output(new DecodedSignals)
              })

  import lookup._
  import Op1Select._
  import Op2Select._
  import branchType._
  import ImmFormat._

  val N = 0.asUInt(1.W)
  val Y = 1.asUInt(1.W)

  /**
    * In scala we sometimes (ab)use the `->` operator to create tuples.
    * The reason for this is that it serves as convenient sugar to make maps.
    *
    * This doesn't matter to you, just fill in the blanks in the style currently
    * used, I just want to demystify some of the scala magic.
    *
    * `a -> b` == `(a, b)` == `Tuple2(a, b)`
    */
  val opcodeMap: Array[(BitPat, List[UInt])] = Array(

    // signal      regWrite, memRead, memWrite, branch,  jump, branchType,    Op1Select, Op2Select, ImmSelect,    ALUOp
    LW     -> List(Y,        Y,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.ADD),
    SW     -> List(N,        N,       Y,        N,       N,    branchType.DC, rs1,       imm,       STYPE,        ALUOps.ADD),
    SUB    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.SUB),
    
    ADDI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.ADD),

    SLTI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.SLT),
    SLTIU  -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.SLTU),
    SLLI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.SLL),
    SRAI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.SRA),
    SRLI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.SRL),
    ANDI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.AND),
    ORI    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.OR),
    XORI   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       imm,       ITYPE,        ALUOps.XOR),
    
    SLT    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.SLT),
    SLTU   -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.SLTU),
    SLL    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.SLL),
    SRL    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.SRL),
    SRA    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.SRA),
    AND    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.AND),
    OR     -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.OR),
    XOR    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.XOR),
    ADD    -> List(Y,        N,       N,        N,       N,    branchType.DC, rs1,       rs2,       ImmFormat.DC, ALUOps.ADD),

    LUI    -> List(Y,        N,       N,        N,       N,    branchType.DC,Op1Select.DC,imm,      UTYPE,        ALUOps.ADD),

    BEQ    -> List(N,        N,       N,        Y,       N,    beq,          rs1,       rs2,       BTYPE,        ALUOps.SUB),
    BNE    -> List(N,        N,       N,        Y,       N,    neq,          rs1,       rs2,       BTYPE,        ALUOps.SUB),
    BLT    -> List(N,        N,       N,        Y,       N,    lt,           rs1,       rs2,       BTYPE,        ALUOps.SLT),
    BGE    -> List(N,        N,       N,        Y,       N,    gte,          rs1,       rs2,       BTYPE,        ALUOps.SLT),
    BLTU   -> List(N,        N,       N,        Y,       N,    ltu,          rs1,       rs2,       BTYPE,        ALUOps.SLTU),
    BGEU   -> List(N,        N,       N,        Y,       N,    gteu,         rs1,       rs2,       BTYPE,        ALUOps.SLTU),

    JAL    -> List(Y,        N,       N,        N,       Y,    jump,         Op1Select.PC,imm,     JTYPE,        ALUOps.ADD),
    JALR   -> List(Y,        N,       N,        Y,       Y,    jump,         rs1,       imm,       ITYPE,        ALUOps.ADD)
)
    // signal      regWrite, memRead, memWrite, branch,  jump, branchType,    Op1Select, Op2Select, ImmSelect,    ALUOp


  val NOP = List(N, N, N, N, N, branchType.DC, rs1, rs2, ImmFormat.DC, ALUOps.DC)

  val decodedControlSignals = ListLookup(
    io.instruction.asUInt(),
    NOP,
    opcodeMap)

  io.decodedSignals.controlSignals.regWrite   := decodedControlSignals(0)
  io.decodedSignals.controlSignals.memRead    := decodedControlSignals(1)
  io.decodedSignals.controlSignals.memWrite   := decodedControlSignals(2)
  io.decodedSignals.controlSignals.branch     := decodedControlSignals(3)
  io.decodedSignals.controlSignals.jump       := decodedControlSignals(4)

  io.decodedSignals.branchType := decodedControlSignals(5)
  io.decodedSignals.op1Select  := decodedControlSignals(6)
  io.decodedSignals.op2Select  := decodedControlSignals(7)
  io.decodedSignals.immType    := decodedControlSignals(8)
  io.decodedSignals.ALUop      := decodedControlSignals(9)
}
