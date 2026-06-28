package riscv.elementosbasicos

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Controller"

  private def pokeBase(dut: Controller, opcode: UInt, funct3: UInt = 0.U, funct7: UInt = 0.U): Unit = {
    dut.io.opcode.poke(opcode)
    dut.io.funct3.poke(funct3)
    dut.io.funct7.poke(funct7)
  }

  it should "decodificar instrucao ADD corretamente (Tipo R)" in {
    test(new Controller) { dut =>
      pokeBase(dut, RV32I.Opcode.OP, "b000".U, "b0000000".U)

      dut.io.signals.regWrite.expect(true.B)
      dut.io.signals.operandBSel.expect(RV32I.OperandBSel.RS2)
      dut.io.signals.aluOp.expect(ALUOp.ADD)
      dut.io.signals.memRead.expect(false.B)
      dut.io.signals.memWrite.expect(false.B)
      dut.io.signals.branchType.expect(RV32I.BranchType.NONE)
      dut.io.signals.illegal.expect(false.B)
    }
  }

  it should "decodificar instrucao SUB corretamente (Tipo R)" in {
    test(new Controller) { dut =>
      pokeBase(dut, RV32I.Opcode.OP, "b000".U, "b0100000".U)

      dut.io.signals.aluOp.expect(ALUOp.SUB)
      dut.io.signals.illegal.expect(false.B)
    }
  }

  it should "decodificar loads RV32I com tamanho e extensao corretos" in {
    test(new Controller) { dut =>
      pokeBase(dut, RV32I.Opcode.LOAD, "b010".U)

      dut.io.signals.regWrite.expect(true.B)
      dut.io.signals.operandBSel.expect(RV32I.OperandBSel.IMM)
      dut.io.signals.memRead.expect(true.B)
      dut.io.signals.writebackSel.expect(RV32I.WritebackSel.MEM)
      dut.io.signals.memSize.expect(RV32I.MemorySize.WORD)
      dut.io.signals.memUnsigned.expect(false.B)
      dut.io.signals.aluOp.expect(ALUOp.ADD)

      pokeBase(dut, RV32I.Opcode.LOAD, "b100".U)
      dut.io.signals.memSize.expect(RV32I.MemorySize.BYTE)
      dut.io.signals.memUnsigned.expect(true.B)
    }
  }

  it should "decodificar branches RV32I explicitamente" in {
    test(new Controller) { dut =>
      pokeBase(dut, RV32I.Opcode.BRANCH, "b110".U)

      dut.io.signals.branchType.expect(RV32I.BranchType.BLTU)
      dut.io.signals.aluOp.expect(ALUOp.SLTU)
      dut.io.signals.illegal.expect(false.B)
    }
  }

  it should "marcar combinacoes invalidas como ilegais" in {
    test(new Controller) { dut =>
      pokeBase(dut, RV32I.Opcode.OP, "b001".U, "b0100000".U)

      dut.io.signals.illegal.expect(true.B)
      dut.io.signals.regWrite.expect(false.B)
    }
  }
}
