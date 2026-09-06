package riscv.pipeline

import chisel3._

class WriteBackStage extends Module {
  val io = IO(new Bundle {
    val in = Input(new MemWb)
    val enabled = Input(Bool())
    val rd = Output(UInt(5.W))
    val data = Output(UInt(32.W))
    val writeEnable = Output(Bool())
  })
  io.rd := io.in.rd
  io.data := io.in.data
  io.writeEnable := io.enabled && io.in.valid && io.in.regWrite
}
