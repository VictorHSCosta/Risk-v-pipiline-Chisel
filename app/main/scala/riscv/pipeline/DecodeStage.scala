package riscv.pipeline

import chisel3._
import riscv.elementosbasicos._

class DecodeStage extends Module {
  val io = IO(new Bundle {
    val in = Input(new IfId)
    val rs1Value = Input(UInt(32.W))
    val rs2Value = Input(UInt(32.W))
    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))
    val usesRs1 = Output(Bool())
    val usesRs2 = Output(Bool())
    val out = Output(new IdEx)
  })
  import RV32I.Opcode

  val immGen = Module(new ImmGen)
  val controller = Module(new Controller)
  val opcode = io.in.instr(6, 0)
  immGen.io.instr := io.in.instr
  controller.io.opcode := opcode
  controller.io.funct3 := io.in.instr(14, 12)
  controller.io.funct7 := io.in.instr(31, 25)
  controller.io.funct12 := io.in.instr(31, 20)

  io.rs1 := io.in.instr(19, 15)
  io.rs2 := io.in.instr(24, 20)
  io.usesRs1 := opcode === Opcode.OP || opcode === Opcode.OP_IMM || opcode === Opcode.LOAD ||
    opcode === Opcode.STORE || opcode === Opcode.BRANCH || opcode === Opcode.JALR
  io.usesRs2 := opcode === Opcode.OP || opcode === Opcode.STORE || opcode === Opcode.BRANCH
  io.out.valid := io.in.valid
  io.out.pc := io.in.pc
  io.out.instr := io.in.instr
  io.out.rs1 := io.rs1
  io.out.rs2 := io.rs2
  io.out.rd := io.in.instr(11, 7)
  io.out.rs1Value := io.rs1Value
  io.out.rs2Value := io.rs2Value
  io.out.imm := immGen.io.imm
  io.out.signals := controller.io.signals
}
