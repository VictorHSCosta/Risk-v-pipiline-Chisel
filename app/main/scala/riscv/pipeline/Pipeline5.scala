package riscv.pipeline

import chisel3._
import riscv.elementosbasicos.RegisterFile

/** Conecta os cinco estagios e os quatro registradores do pipeline. */
class Pipeline5(
    initialProgram: Seq[Long] = Seq.empty,
    memoryWords: Int = 1024,
    programFile: String = ""
) extends Module {
  val io = IO(new Bundle {
    val pc = Output(UInt(32.W)); val instr = Output(UInt(32.W));
    val aluResult = Output(UInt(32.W))
    val writebackData = Output(UInt(32.W)); val writebackRd = Output(UInt(5.W));
    val writebackEnable = Output(Bool())
    val illegal = Output(Bool()); val halted = Output(Bool());
    val trapValid = Output(Bool()); val trapCause = Output(UInt(4.W))
    val trapPc = Output(UInt(32.W)); val trapInstruction = Output(UInt(32.W))
  })

  val fetch = Module(new FetchStage(initialProgram, memoryWords, programFile))
  val decode = Module(new DecodeStage)
  val execute = Module(new ExecuteStage)
  val memory = Module(new MemoryAccessStage(memoryWords))
  val writeBack = Module(new WriteBackStage)
  val registerFile = Module(new RegisterFile)
  val hazards = Module(new HazardUnit)

  val pc = RegInit(0.U(32.W))
  val ifId = RegInit(0.U.asTypeOf(new IfId))
  val idEx = RegInit(0.U.asTypeOf(new IdEx))
  val exMem = RegInit(0.U.asTypeOf(new ExMem))
  val memWb = RegInit(0.U.asTypeOf(new MemWb))
  val draining = RegInit(false.B)
  val drainCount = RegInit(0.U(2.W))
  val halted = RegInit(false.B)
  val trapValid = RegInit(false.B)
  val trapCause = RegInit(0.U(4.W))
  val trapPc = RegInit(0.U(32.W))
  val trapInstruction = RegInit(0.U(32.W))

  fetch.io.pc := pc
  decode.io.in := ifId
  registerFile.io.rs1 := decode.io.rs1
  registerFile.io.rs2 := decode.io.rs2
  execute.io.in := idEx
  execute.io.exMemForward := exMem
  execute.io.memWbForward := memWb
  memory.io.in := exMem
  memory.io.allowWrite := !halted
  writeBack.io.in := memWb
  writeBack.io.enabled := !halted
  registerFile.io.rd := writeBack.io.rd
  registerFile.io.writeData := writeBack.io.data
  registerFile.io.regWrite := writeBack.io.writeEnable
  // A escrita e a leitura ocorrem na mesma borda; bypass evita usar o valor antigo em ID.
  decode.io.rs1Value := Mux(
    writeBack.io.writeEnable && writeBack.io.rd =/= 0.U && writeBack.io.rd === decode.io.rs1,
    writeBack.io.data,
    registerFile.io.readData1
  )
  decode.io.rs2Value := Mux(
    writeBack.io.writeEnable && writeBack.io.rd =/= 0.U && writeBack.io.rd === decode.io.rs2,
    writeBack.io.data,
    registerFile.io.readData2
  )

  hazards.io.decodeValid := ifId.valid
  hazards.io.usesRs1 := decode.io.usesRs1
  hazards.io.usesRs2 := decode.io.usesRs2
  hazards.io.rs1 := decode.io.rs1
  hazards.io.rs2 := decode.io.rs2
  hazards.io.executeLoad := idEx.valid && idEx.signals.regWrite && idEx.signals.writebackSel === 1.U
  hazards.io.executeRd := idEx.rd

  // MEM/WB advances even while a trap drains older instructions.
  memWb := memory.io.out
  when(execute.io.trap) {
    exMem := 0.U.asTypeOf(new ExMem)
    idEx := 0.U.asTypeOf(new IdEx)
    ifId := 0.U.asTypeOf(new IfId)
    draining := true.B; drainCount := 2.U
    trapValid := true.B; trapCause := execute.io.trapCause; trapPc := idEx.pc;
    trapInstruction := idEx.instr
  }.elsewhen(draining) {
    exMem := 0.U.asTypeOf(new ExMem)
    idEx := 0.U.asTypeOf(new IdEx)
    ifId := 0.U.asTypeOf(new IfId)
    when(drainCount === 0.U) { halted := true.B; draining := false.B }
      .otherwise { drainCount := drainCount - 1.U }
  }.elsewhen(!halted) {
    exMem := execute.io.out
    when(execute.io.redirect) {
      pc := execute.io.redirectTarget
      ifId := 0.U.asTypeOf(new IfId)
      idEx := 0.U.asTypeOf(new IdEx)
    }.elsewhen(hazards.io.stall) {
      idEx := 0.U.asTypeOf(new IdEx)
    }.otherwise {
      idEx := decode.io.out
      ifId.valid := true.B
      ifId.pc := pc
      ifId.instr := fetch.io.instr
      pc := pc + 4.U
    }
  }

  io.pc := pc; io.instr := ifId.instr; io.aluResult := execute.io.out.aluResult
  io.writebackData := writeBack.io.data; io.writebackRd := writeBack.io.rd;
  io.writebackEnable := writeBack.io.writeEnable
  io.illegal := idEx.valid && idEx.signals.illegal
  io.halted := halted; io.trapValid := trapValid; io.trapCause := trapCause;
  io.trapPc := trapPc; io.trapInstruction := trapInstruction
}
