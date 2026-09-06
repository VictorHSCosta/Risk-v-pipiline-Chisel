package riscv.pipeline

import chisel3._
import riscv.elementosbasicos.ControlSignals

class IfId extends Bundle {
  val valid = Bool()
  val pc = UInt(32.W)
  val instr = UInt(32.W)
}

class IdEx extends Bundle {
  val valid = Bool()
  val pc = UInt(32.W)
  val instr = UInt(32.W)
  val rs1 = UInt(5.W)
  val rs2 = UInt(5.W)
  val rd = UInt(5.W)
  val rs1Value = UInt(32.W)
  val rs2Value = UInt(32.W)
  val imm = UInt(32.W)
  val signals = new ControlSignals
}

class ExMem extends Bundle {
  val valid = Bool()
  val pc = UInt(32.W)
  val instr = UInt(32.W)
  val rd = UInt(5.W)
  val aluResult = UInt(32.W)
  val writeData = UInt(32.W)
  val wbData = UInt(32.W)
  val regWrite = Bool()
  val writebackSel = UInt(2.W)
  val memWrite = Bool()
  val memSize = UInt(2.W)
  val memUnsigned = Bool()
}

class MemWb extends Bundle {
  val valid = Bool()
  val rd = UInt(5.W)
  val data = UInt(32.W)
  val regWrite = Bool()
}

class HazardUnit extends Module {
  val io = IO(new Bundle {
    val decodeValid = Input(Bool())
    val usesRs1 = Input(Bool())
    val usesRs2 = Input(Bool())
    val rs1 = Input(UInt(5.W))
    val rs2 = Input(UInt(5.W))
    val executeLoad = Input(Bool())
    val executeRd = Input(UInt(5.W))
    val stall = Output(Bool())
  })

  io.stall := io.decodeValid && io.executeLoad && io.executeRd =/= 0.U &&
    ((io.usesRs1 && io.executeRd === io.rs1) || (io.usesRs2 && io.executeRd === io.rs2))
}
