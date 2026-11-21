package riscv.core.pipeline.pipeline5

import chisel3._
import riscv.core.models.memory.DataMemory

private[pipeline5] class MemoryStage(memoryWords: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(new ExecutePacket)
    val out = Output(new WritebackPacket)
  })
  val dataMem = Module(new DataMemory(memoryWords))
  val exMem = io.in
  dataMem.io.address := exMem.result
  dataMem.io.writeData := exMem.storeData
  dataMem.io.writeEnable := exMem.store
  dataMem.io.memSize := exMem.size
  dataMem.io.unsignedLoad := exMem.unsigned
  io.out.rd := exMem.rd
  io.out.enable := exMem.regWrite
  io.out.data := Mux(exMem.load, dataMem.io.readData, exMem.result)

}
