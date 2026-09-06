package riscv.pipeline

import chisel3._
import riscv.elementosbasicos.{DataMemory, RV32I}

class MemoryAccessStage(memoryWords: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(new ExMem)
    val allowWrite = Input(Bool())
    val out = Output(new MemWb)
  })
  val dataMemory = Module(new DataMemory(memoryWords))
  dataMemory.io.address := io.in.aluResult
  dataMemory.io.writeData := io.in.writeData
  dataMemory.io.writeEnable := io.in.valid && io.in.memWrite && io.allowWrite
  dataMemory.io.memSize := io.in.memSize
  dataMemory.io.unsignedLoad := io.in.memUnsigned
  io.out.valid := io.in.valid
  io.out.rd := io.in.rd
  io.out.data := Mux(
    io.in.writebackSel === RV32I.WritebackSel.MEM,
    dataMemory.io.readData,
    io.in.wbData
  )
  io.out.regWrite := io.in.regWrite
}
