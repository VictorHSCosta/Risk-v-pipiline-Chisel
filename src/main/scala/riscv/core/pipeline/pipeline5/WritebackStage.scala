package riscv.core.pipeline.pipeline5

import chisel3._

private[pipeline5] class WritebackPacket extends Bundle {
  val pc = UInt(32.W)
  val instr = UInt(32.W)
  val valid = Bool()

  val rd = UInt(5.W)
  val enable = Bool()
  val data = UInt(32.W)
}

private[pipeline5] class WritebackStage extends Module {
  val io = IO(new Bundle {
    val in = Input(new WritebackPacket)
    val out = Output(new WritebackPacket)
  })
  io.out := io.in
}
