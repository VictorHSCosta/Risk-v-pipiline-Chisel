package riscv.elementosbasicos

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class ControllerSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Controller"

  it should "decodificar instrucao ADD corretamente (Tipo R)" in {
    test(new Controller) { dut =>
      // ADD opcode = 0110011, funct3 = 000, funct7_5 = 0
      dut.io.opcode.poke("b0110011".U)
      dut.io.funct3.poke("b000".U)
      dut.io.funct7_5.poke(false.B)

      // Checagens
      dut.io.signals.regWrite.expect(true.B)
      dut.io.signals.aluSrc.expect(false.B)
      dut.io.signals.aluOp.expect(ALUOp.ADD)
      dut.io.signals.memRead.expect(false.B)
      dut.io.signals.memWrite.expect(false.B)
      dut.io.signals.branch.expect(false.B)
    }
  }

  it should "decodificar instrucao SUB corretamente (Tipo R)" in {
    test(new Controller) { dut =>
      // SUB opcode = 0110011, funct3 = 000, funct7_5 = 1
      dut.io.opcode.poke("b0110011".U)
      dut.io.funct3.poke("b000".U)
      dut.io.funct7_5.poke(true.B)

      dut.io.signals.aluOp.expect(ALUOp.SUB)
    }
  }

  it should "decodificar instrucao LOAD corretamente (Tipo I)" in {
    test(new Controller) { dut =>
      // Load opcode = 0000011
      dut.io.opcode.poke("b0000011".U)
      dut.io.funct3.poke("b010".U) // ex: LW
      dut.io.funct7_5.poke(false.B)

      dut.io.signals.regWrite.expect(true.B)
      dut.io.signals.aluSrc.expect(true.B)
      dut.io.signals.memRead.expect(true.B)
      dut.io.signals.memToReg.expect(true.B)
      dut.io.signals.aluOp.expect(ALUOp.ADD)
    }
  }
}
