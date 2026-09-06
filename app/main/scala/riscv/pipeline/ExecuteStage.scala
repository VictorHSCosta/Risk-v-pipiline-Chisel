package riscv.pipeline

import chisel3._
import chisel3.util._
import riscv.elementosbasicos._

class ExecuteStage extends Module {
  val io = IO(new Bundle {
    val in = Input(new IdEx)
    val exMemForward = Input(new ExMem)
    val memWbForward = Input(new MemWb)
    val out = Output(new ExMem)
    val redirect = Output(Bool())
    val redirectTarget = Output(UInt(32.W))
    val trap = Output(Bool())
    val trapCause = Output(UInt(4.W))
  })
  import RV32I._

  val ula = Module(new ULA)
  def forward(rs: UInt, value: UInt): UInt = Mux(
    io.exMemForward.valid && io.exMemForward.regWrite && io.exMemForward.rd =/= 0.U &&
      io.exMemForward.writebackSel =/= WritebackSel.MEM && io.exMemForward.rd === rs,
    io.exMemForward.wbData,
    Mux(
      io.memWbForward.valid && io.memWbForward.regWrite && io.memWbForward.rd =/= 0.U && io.memWbForward.rd === rs,
      io.memWbForward.data,
      value
    )
  )
  val rs1 = forward(io.in.rs1, io.in.rs1Value)
  val rs2 = forward(io.in.rs2, io.in.rs2Value)
  ula.io.a := MuxLookup(io.in.signals.operandASel, rs1)(
    Seq(OperandASel.PC -> io.in.pc, OperandASel.ZERO -> 0.U)
  )
  ula.io.b := Mux(io.in.signals.operandBSel === OperandBSel.IMM, io.in.imm, rs2)
  ula.io.op := io.in.signals.aluOp

  val branchTaken = MuxLookup(io.in.signals.branchType, false.B)(
    Seq(
      BranchType.BEQ -> (rs1 === rs2),
      BranchType.BNE -> (rs1 =/= rs2),
      BranchType.BLT -> (rs1.asSInt < rs2.asSInt),
      BranchType.BGE -> (rs1.asSInt >= rs2.asSInt),
      BranchType.BLTU -> (rs1 < rs2),
      BranchType.BGEU -> (rs1 >= rs2)
    )
  )
  val misalignedMemory = MuxLookup(io.in.signals.memSize, false.B)(
    Seq(
      MemorySize.HALF -> ula.io.result(0),
      MemorySize.WORD -> ula.io.result(1, 0).orR
    )
  )
  io.trap := io.in.valid && (io.in
    .pc(1, 0)
    .orR || io.in.signals.illegal || io.in.signals.trap || (misalignedMemory && (io.in.signals.memWrite || io.in.signals.writebackSel === WritebackSel.MEM)))
  io.trapCause := Mux(
    io.in.pc(1, 0).orR,
    TrapCause.INSTRUCTION_MISALIGNED,
    Mux(
      io.in.signals.trap,
      io.in.signals.trapCause,
      Mux(
        io.in.signals.illegal,
        TrapCause.ILLEGAL_INSTRUCTION,
        Mux(
          io.in.signals.memWrite,
          TrapCause.STORE_MISALIGNED,
          TrapCause.LOAD_MISALIGNED
        )
      )
    )
  )
  io.redirect := io.in.valid && !io.trap && (io.in.signals.jump || (io.in.signals.branchType =/= BranchType.NONE && branchTaken))
  io.redirectTarget := Mux(
    io.in.signals.jalr,
    ula.io.result & "hfffffffe".U,
    (io.in.pc.asSInt + io.in.imm.asSInt).asUInt
  )
  io.out.valid := io.in.valid
  io.out.pc := io.in.pc; io.out.instr := io.in.instr; io.out.rd := io.in.rd
  io.out.aluResult := ula.io.result; io.out.writeData := rs2
  io.out.wbData := MuxLookup(io.in.signals.writebackSel, ula.io.result)(
    Seq(WritebackSel.PC4 -> (io.in.pc + 4.U), WritebackSel.IMM -> io.in.imm)
  )
  io.out.regWrite := io.in.signals.regWrite;
  io.out.writebackSel := io.in.signals.writebackSel
  io.out.memWrite := io.in.signals.memWrite;
  io.out.memSize := io.in.signals.memSize;
  io.out.memUnsigned := io.in.signals.memUnsigned
}
