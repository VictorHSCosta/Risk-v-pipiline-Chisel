package riscv.elementosbasicos

import chisel3._

object RV32I {
  object Opcode {
    val LOAD     = "b0000011".U(7.W)
    val OP_IMM   = "b0010011".U(7.W)
    val AUIPC    = "b0010111".U(7.W)
    val STORE    = "b0100011".U(7.W)
    val OP       = "b0110011".U(7.W)
    val LUI      = "b0110111".U(7.W)
    val BRANCH   = "b1100011".U(7.W)
    val JALR     = "b1100111".U(7.W)
    val JAL      = "b1101111".U(7.W)
  }

  object BranchType {
    val NONE = 0.U(3.W)
    val BEQ  = 1.U(3.W)
    val BNE  = 2.U(3.W)
    val BLT  = 3.U(3.W)
    val BGE  = 4.U(3.W)
    val BLTU = 5.U(3.W)
    val BGEU = 6.U(3.W)
  }

  object MemorySize {
    val BYTE = 0.U(2.W)
    val HALF = 1.U(2.W)
    val WORD = 2.U(2.W)
  }

  object OperandASel {
    val RS1  = 0.U(2.W)
    val PC   = 1.U(2.W)
    val ZERO = 2.U(2.W)
  }

  object OperandBSel {
    val RS2 = 0.U(1.W)
    val IMM = 1.U(1.W)
  }

  object WritebackSel {
    val ALU = 0.U(2.W)
    val MEM = 1.U(2.W)
    val PC4 = 2.U(2.W)
    val IMM = 3.U(2.W)
  }
}
