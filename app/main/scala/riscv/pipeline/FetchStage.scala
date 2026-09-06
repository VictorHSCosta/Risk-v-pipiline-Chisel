package riscv.pipeline

import chisel3._
import riscv.elementosbasicos.InstructionMemory

class FetchStage(
    initialProgram: Seq[Long],
    memoryWords: Int,
    programFile: String
) extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val instr = Output(UInt(32.W))
  })
  val instructionMemory = Module(
    new InstructionMemory(memoryWords, initialProgram, programFile)
  )
  instructionMemory.io.address := io.pc
  io.instr := instructionMemory.io.readData
}
