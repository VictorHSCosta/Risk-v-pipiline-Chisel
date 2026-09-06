package riscv.pipeline

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Pipeline5HazardSpec
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Matchers {
  behavior of "Pipeline5 hazards"

  private def i(imm: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): Long =
    ((imm & 0xfff).toLong << 20) | (rs1.toLong << 15) | (funct3.toLong << 12) | (rd.toLong << 7) | opcode
  private def addi(rd: Int, rs1: Int, imm: Int) = i(imm, rs1, 0, rd, 0x13)
  private def lw(rd: Int, rs1: Int, imm: Int) = i(imm, rs1, 2, rd, 0x03)
  private def sw(rs2: Int, rs1: Int, imm: Int): Long =
    (((imm >> 5) & 0x7f).toLong << 25) | (rs2.toLong << 20) | (rs1.toLong << 15) | (2L << 12) | ((imm & 0x1f).toLong << 7) | 0x23
  private def beq(rs1: Int, rs2: Int, imm: Int): Long =
    (((imm >> 12 & 1).toLong << 31) | ((imm >> 5 & 0x3f).toLong << 25) | (rs2.toLong << 20) | (rs1.toLong << 15) | ((imm >> 1 & 0xf).toLong << 8) | ((imm >> 11 & 1).toLong << 7) | 0x63)

  private def commits(program: Seq[Long]): Vector[(Int, BigInt)] = {
    var result = Vector.empty[(Int, BigInt)]
    test(new Pipeline5(program, memoryWords = 16)) { dut =>
      for (_ <- 0 until 18) {
        dut.clock.step()
        if (dut.io.writebackEnable.peek().litToBoolean)
          result :+= (
            dut.io.writebackRd.peekInt().toInt,
            dut.io.writebackData.peekInt()
          )
      }
    }
    result
  }

  it should "forward ALU results, store data, and a load-use dependency" in {
    commits(
      Seq(
        addi(1, 0, 8),
        addi(2, 0, 41),
        sw(2, 1, 0),
        lw(3, 1, 0),
        addi(4, 3, 1)
      )
    ) should contain((4, BigInt(42)))
  }

  it should "flush both younger instructions after a taken branch" in {
    commits(
      Seq(
        addi(1, 0, 1),
        beq(1, 1, 12),
        addi(2, 0, 99),
        addi(3, 0, 99),
        addi(4, 0, 7)
      )
    ) should contain((4, BigInt(7)))
  }
}
