package riscv.core.pipeline.pipeline5

import chisel3._
import chisel3.util._
import riscv.core.models.execute.ULA
import riscv.isa.shared.RV32I._

private[pipeline5] class ExecutePacket extends Bundle {
  val rd = UInt(5.W)
  val regWrite = Bool()
  val load = Bool()
  val store = Bool()
  val size = UInt(2.W)
  val unsigned = Bool()
  val result = UInt(32.W)
  val storeData = UInt(32.W)
}

private[pipeline5] class ExecuteStage extends Module {
  val io = IO(new Bundle {
    val in = Input(new DecodePacket)
    val mem = Input(new ExecutePacket)
    val wb = Input(new WritebackPacket)
    val out = Output(new ExecutePacket)
    val redirect = Valid(UInt(32.W))
    val aluResult = Output(UInt(32.W))
    val illegal = Output(Bool())
  })
  val ula = Module(new ULA)

  val idEx = io.in
  val exMem = io.mem
  val memWb = io.wb
  def fromWriteback(reg: UInt, value: UInt): UInt =
    Mux(memWb.enable && memWb.rd === reg, memWb.data, value)

  def forward(reg: UInt, value: UInt): UInt =
    Mux(
      exMem.regWrite && !exMem.load && exMem.rd === reg,
      exMem.result,
      fromWriteback(reg, value)
    )

  val rs1 = forward(idEx.rs1, idEx.rs1Value)
  val rs2 = forward(idEx.rs2, idEx.rs2Value)

  val exOperandA = WireDefault(rs1)
  val exOperandB = WireDefault(rs2)
  switch(idEx.signals.operandASel) {
    is(OperandASel.PC) { exOperandA := idEx.pc }
    is(OperandASel.ZERO) { exOperandA := 0.U }
  }
  when(idEx.signals.operandBSel === OperandBSel.IMM) {
    exOperandB := idEx.imm
  }

  ula.io.a := exOperandA
  ula.io.b := exOperandB
  ula.io.op := idEx.signals.aluOp

  // Endereços de desvio
  val jalrTarget = ula.io.result & "hFFFFFFFE".U(32.W)
  val pcRelativeTarget = idEx.pc + idEx.imm

  val branchTaken = WireDefault(false.B)
  switch(idEx.signals.branchType) {
    is(BranchType.BEQ) { branchTaken := rs1 === rs2 }
    is(BranchType.BNE) { branchTaken := rs1 =/= rs2 }
    is(BranchType.BLT) {
      branchTaken := rs1.asSInt < rs2.asSInt
    }
    is(BranchType.BGE) {
      branchTaken := rs1.asSInt >= rs2.asSInt
    }
    is(BranchType.BLTU) { branchTaken := rs1 < rs2 }
    is(BranchType.BGEU) { branchTaken := rs1 >= rs2 }
  }

  val controlRedirect = idEx.valid && !idEx.signals.illegal &&
    (idEx.signals.jump || (idEx.signals.branchType =/= BranchType.NONE && branchTaken))
  val redirectTarget = Mux(idEx.signals.jalr, jalrTarget, pcRelativeTarget)

  val exValid = idEx.valid && !idEx.signals.illegal
  val exLoad = exValid && idEx.signals.writebackSel === WritebackSel.MEM
  io.out.rd := idEx.rd
  io.out.regWrite := exValid && idEx.signals.regWrite && idEx.rd =/= 0.U
  io.out.load := exLoad
  io.out.store := exValid && idEx.signals.memWrite
  io.out.size := idEx.signals.memSize
  io.out.unsigned := idEx.signals.memUnsigned
  io.out.result := MuxLookup(idEx.signals.writebackSel, ula.io.result)(
    Seq(
      WritebackSel.PC4 -> (idEx.pc + idEx.instrLength),
      WritebackSel.IMM -> idEx.imm
    )
  )
  io.out.storeData := rs2

  io.redirect.valid := controlRedirect
  io.redirect.bits := redirectTarget
  io.aluResult := ula.io.result
  io.illegal := idEx.valid && idEx.signals.illegal

}
