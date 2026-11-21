package riscv.core.pipeline.pipeline5

import chisel3._

/** Núcleo em ordem com estágios IF, ID, EX, MEM e WB. */
class Pipeline5(initialProgram: Seq[Long] = Seq.empty, memoryWords: Int = 1024)
    extends Module {
  val io = IO(new Bundle {
    val pc = Output(UInt(32.W))
    val instr = Output(UInt(32.W))
    val aluResult = Output(UInt(32.W))
    val writebackData = Output(UInt(32.W))
    val writebackRd = Output(UInt(5.W))
    val writebackEnable = Output(Bool())
    val commitValid = Output(Bool())
    val commitPc = Output(UInt(32.W))
    val commitInstr = Output(UInt(32.W))
    val illegal = Output(Bool())
    val stalled = Output(Bool())
  })

  private val fetch = Module(new FetchStage(initialProgram, memoryWords))
  private val decode = Module(new DecodeStage)
  private val execute = Module(new ExecuteStage)
  private val memory = Module(new MemoryStage(memoryWords))
  private val writeback = Module(new WritebackStage)

  fetch.io.stall := decode.io.stall
  fetch.io.redirect := execute.io.redirect
  decode.io.flush := execute.io.redirect.valid
  decode.io.in := fetch.io.out
  decode.io.wb := writeback.io.out
  decode.io.loadInExecute := execute.io.loadInExecute
  decode.io.executeRd := execute.io.executeRd
  execute.io.in := decode.io.out
  execute.io.mem := execute.io.out
  execute.io.wb := writeback.io.out
  memory.io.in := execute.io.out
  writeback.io.in := memory.io.out

  io.commitValid := writeback.io.out.valid
  io.commitPc := writeback.io.out.pc
  io.commitInstr := writeback.io.out.instr
  io.pc := fetch.io.pc
  io.instr := fetch.io.out.instr
  io.aluResult := execute.io.aluResult
  io.illegal := execute.io.illegal
  io.stalled := decode.io.stall
  io.writebackEnable := writeback.io.out.enable
  io.writebackRd := writeback.io.out.rd
  io.writebackData := writeback.io.out.data
}
