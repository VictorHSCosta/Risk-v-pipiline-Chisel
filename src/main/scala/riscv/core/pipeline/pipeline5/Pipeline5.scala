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
    val illegal = Output(Bool())
    val stalled = Output(Bool())
  })

  private val fetch = Module(new FetchStage(initialProgram, memoryWords))
  private val decode = Module(new DecodeStage)
  private val execute = Module(new ExecuteStage)
  private val memory = Module(new MemoryStage(memoryWords))
  private val writeback = Module(new WritebackStage)

  private val ifId = RegInit(0.U.asTypeOf(new FetchPacket))
  private val idEx = RegInit(0.U.asTypeOf(new DecodePacket))
  private val exMem = RegInit(0.U.asTypeOf(new ExecutePacket))
  private val memWb = RegInit(0.U.asTypeOf(new WritebackPacket))

  fetch.io.stall := decode.io.stall
  fetch.io.redirect := execute.io.redirect
  decode.io.in := ifId
  decode.io.wb := writeback.io.out
  decode.io.loadInExecute := execute.io.out.load
  decode.io.executeRd := execute.io.out.rd
  execute.io.in := idEx
  execute.io.mem := exMem
  execute.io.wb := writeback.io.out
  memory.io.in := exMem
  writeback.io.in := memWb

  when(execute.io.redirect.valid) {
    ifId.valid := false.B
    idEx.valid := false.B
  }.elsewhen(decode.io.stall) {
    idEx.valid := false.B
  }.otherwise {
    ifId := fetch.io.out
    idEx := decode.io.out
  }
  exMem := execute.io.out
  memWb := memory.io.out

  io.pc := fetch.io.pc
  io.instr := ifId.instr
  io.aluResult := execute.io.aluResult
  io.illegal := execute.io.illegal
  io.stalled := decode.io.stall
  io.writebackEnable := writeback.io.out.enable
  io.writebackRd := writeback.io.out.rd
  io.writebackData := writeback.io.out.data
}
