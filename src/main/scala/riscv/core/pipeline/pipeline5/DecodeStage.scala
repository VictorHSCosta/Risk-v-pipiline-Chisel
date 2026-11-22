package riscv.core.pipeline.pipeline5

import chisel3._
import riscv.base.rv32i.decode.ImmGen
import riscv.base.rv32i.registers.RegisterFile
import riscv.core.models.decode.{ControlSignals, Controller}
import riscv.isa.shared.RV32I._

private[pipeline5] class DecodePacket extends Bundle {
  val valid = Bool()
  val instr = UInt(32.W)
  val pc = UInt(32.W)
  val instrLength = UInt(3.W)
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)
  val rd = UInt(5.W)
  val rs1Value = UInt(32.W)
  val rs2Value = UInt(32.W)
  val imm = UInt(32.W)
  val signals = new ControlSignals
}

private[pipeline5] class DecodeStage extends Module {
  val io = IO(new Bundle {
    val flush = Input(Bool())
    val in = Input(new FetchPacket)
    val wb = Input(new WritebackPacket)
    val loadInExecute = Input(Bool())
    val executeRd = Input(UInt(5.W))
    val stall = Output(Bool())
    val out = Output(new DecodePacket)
  })
  val regFile = Module(new RegisterFile)
  val immGen = Module(new ImmGen)
  val controller = Module(new Controller)
  val ifId = io.in
  val memWb = io.wb
  val opcode = ifId.instr(6, 0)
  val idRs1 = ifId.instr(19, 15)
  val idRs2 = ifId.instr(24, 20)
  controller.io.opcode := opcode
  controller.io.funct3 := ifId.instr(14, 12)
  controller.io.funct7 := ifId.instr(31, 25)
  immGen.io.instr := ifId.instr
  val isMExtension = opcode === Opcode.OP && ifId.instr(31, 25) === 1.U
  val idIllegal = ifId.illegal || controller.io.signals.illegal ||
    opcode === Opcode.ATOMIC || opcode === Opcode.SYSTEM || isMExtension

  regFile.io.rs1 := idRs1
  regFile.io.rs2 := idRs2
  regFile.io.rd := memWb.rd
  regFile.io.regWrite := memWb.enable
  regFile.io.writeData := memWb.data

  def fromWriteback(reg: UInt, value: UInt): UInt =
    Mux(memWb.enable && memWb.rd === reg, memWb.data, value)

  val usesRs2 =
    opcode === Opcode.OP || opcode === Opcode.STORE || opcode === Opcode.BRANCH
  val usesRs1 = usesRs2 || opcode === Opcode.OP_IMM || opcode === Opcode.LOAD ||
    opcode === Opcode.JALR
  val stall =
    ifId.valid && !idIllegal && io.loadInExecute && io.executeRd =/= 0.U &&
      ((usesRs1 && idRs1 === io.executeRd) || (usesRs2 && idRs2 === io.executeRd))

  io.stall := stall
  val next = Wire(new DecodePacket)
  next.instr := ifId.instr
  next.valid := ifId.valid
  next.pc := ifId.pc
  next.instrLength := ifId.length
  next.rs1 := idRs1
  next.rs2 := idRs2
  next.rd := ifId.instr(11, 7)
  next.rs1Value := fromWriteback(idRs1, regFile.io.readData1)
  next.rs2Value := fromWriteback(idRs2, regFile.io.readData2)
  next.imm := immGen.io.imm
  next.signals := controller.io.signals
  next.signals.illegal := idIllegal

  val output = RegInit(0.U.asTypeOf(new DecodePacket))
  output := next
  output.valid := next.valid && !stall && !io.flush
  io.out := output
}
