package riscv.core.pipeline.pipeline5

import chisel3._
import riscv.core.models.memory.DataMemory

private[pipeline5] class MemoryStage(memoryWords: Int, externalMemory: Boolean)
    extends Module {
  val io = IO(new Bundle {
    val external = Option.when(externalMemory)(new Bundle {
      val address = Output(UInt(32.W))
      val load = Output(Bool())
      val store = Output(Bool())
      val size = Output(UInt(2.W))
      val unsigned = Output(Bool())
      val writeData = Output(UInt(32.W))
      val readData = Input(UInt(32.W))
    })
    val in = Input(new ExecutePacket)
    val out = Output(new WritebackPacket)
  })
  val exMem = io.in
  val readData = if (externalMemory) {
    val port = io.external.get
    port.address := exMem.result
    port.load := exMem.load
    port.store := exMem.store
    port.size := exMem.size
    port.unsigned := exMem.unsigned
    port.writeData := exMem.storeData
    port.readData
  } else {
    val dataMem = Module(new DataMemory(memoryWords))
    dataMem.io.address := exMem.result
    dataMem.io.writeData := exMem.storeData
    dataMem.io.writeEnable := exMem.store
    dataMem.io.memSize := exMem.size
    dataMem.io.unsignedLoad := exMem.unsigned
    dataMem.io.readData
  }
  val next = Wire(new WritebackPacket)
  next.valid := exMem.valid
  next.pc := exMem.pc
  next.instr := exMem.instr
  next.rd := exMem.rd
  next.enable := exMem.regWrite
  next.data := Mux(exMem.load, readData, exMem.result)

  io.out := RegNext(next, 0.U.asTypeOf(new WritebackPacket))
}
