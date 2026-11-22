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

private[pipeline5] class FetchStage(
    initialProgram: Seq[Long],
    memoryWords: Int,
    resetPc: Long,
    externalMemory: Boolean
) extends Module {
  val io = IO(new Bundle {
    val external = Option.when(externalMemory)(new Bundle {
      val address = Output(UInt(32.W))
      val data = Input(UInt(32.W))
    })
    val stall = Input(Bool())
    val redirect = Input(chisel3.util.Valid(UInt(32.W)))
    val out = Output(new FetchPacket)
    val pc = Output(UInt(32.W))
  })
  val fetch = Module(new InstructionFetch)
  val pc = RegInit(resetPc.U(32.W))
  fetch.io.pc := pc
  fetch.io.enable := !io.stall
  fetch.io.flush := io.redirect.valid
  if (externalMemory) {
    io.external.get.address := fetch.io.memoryAddress
    fetch.io.memoryData := io.external.get.data
  } else {
    val instrMem = Module(new InstructionMemory(memoryWords, initialProgram))
    fetch.io.memoryData := instrMem.io.readData
    instrMem.io.address := fetch.io.memoryAddress
    instrMem.io.readEnable := fetch.io.memoryReadEnable
  }

  when(io.redirect.valid) { pc := io.redirect.bits }
    .elsewhen(fetch.io.valid) { pc := pc + fetch.io.length }
  io.pc := pc
  val next = Wire(new FetchPacket)
  next.pc := pc
  next.valid := fetch.io.valid
  next.instr := fetch.io.instr
  next.length := fetch.io.length
  next.illegal := fetch.io.illegal

  val output = RegInit(0.U.asTypeOf(new FetchPacket))
  when(io.redirect.valid) {
    output.valid := false.B
  }.elsewhen(!io.stall) {
    output := next
  }
  io.out := output
}
