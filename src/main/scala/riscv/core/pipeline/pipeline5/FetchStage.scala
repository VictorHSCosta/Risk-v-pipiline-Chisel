package riscv.core.pipeline.pipeline5

import chisel3._
import riscv.core.models.fetch.InstructionFetch
import riscv.core.models.memory.InstructionMemory

private[pipeline5] class FetchPacket extends Bundle {
  val valid = Bool()
  val illegal = Bool()
  val pc = UInt(32.W)
  val instr = UInt(32.W)
  val length = UInt(3.W)
}

private[pipeline5] class FetchStage(initialProgram: Seq[Long], memoryWords: Int)
    extends Module {
  val io = IO(new Bundle {
    val stall = Input(Bool())
    val redirect = Input(chisel3.util.Valid(UInt(32.W)))
    val out = Output(new FetchPacket)
    val pc = Output(UInt(32.W))
  })
  val instrMem = Module(new InstructionMemory(memoryWords, initialProgram))
  val fetch = Module(new InstructionFetch)
  val pc = RegInit(0.U(32.W))
  fetch.io.pc := pc
  fetch.io.enable := !io.stall
  fetch.io.flush := io.redirect.valid
  fetch.io.memoryData := instrMem.io.readData
  instrMem.io.address := fetch.io.memoryAddress
  instrMem.io.readEnable := fetch.io.memoryReadEnable

  when(io.redirect.valid) { pc := io.redirect.bits }
    .elsewhen(fetch.io.valid) { pc := pc + fetch.io.length }
  io.pc := pc
  io.out.pc := pc
  io.out.valid := fetch.io.valid
  io.out.instr := fetch.io.instr
  io.out.length := fetch.io.length
  io.out.illegal := fetch.io.illegal

}
