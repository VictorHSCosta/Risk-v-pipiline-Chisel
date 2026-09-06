package riscv.pipeline

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Pipeline5TrapSpec
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Matchers {
  behavior of "Pipeline5 traps"

  private def i(imm: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): Long =
    ((imm & 0xfff).toLong << 20) | (rs1.toLong << 15) | (funct3.toLong << 12) | (rd.toLong << 7) | opcode
  private def addi(rd: Int, rs1: Int, imm: Int) = i(imm, rs1, 0, rd, 0x13)
  private def lw(rd: Int, rs1: Int, imm: Int) = i(imm, rs1, 2, rd, 0x03)
  private def sw(rs2: Int, rs1: Int, imm: Int): Long =
    (((imm >> 5) & 0x7f).toLong << 25) | (rs2.toLong << 20) | (rs1.toLong << 15) | (2L << 12) | ((imm & 0x1f).toLong << 7) | 0x23

  it should "halt with the architectural cause for each trap" in {
    val cases = Seq(
      Seq(0x00000073L) -> 11,
      Seq(0x00100073L) -> 3,
      Seq(0x0000000fL, 0x00100073L) -> 3,
      Seq(0xffffffffL) -> 2,
      Seq(0x0020006fL) -> 0,
      Seq(addi(1, 0, 1), lw(2, 1, 0)) -> 4,
      Seq(addi(1, 0, 2), sw(0, 1, 0)) -> 6
    )
    for ((program, cause) <- cases)
      test(new Pipeline5(program, memoryWords = 16)) { dut =>
        for (_ <- 0 until 12 if !dut.io.halted.peek().litToBoolean)
          dut.clock.step()
        dut.io.halted.expect(true.B)
        dut.io.trapValid.expect(true.B)
        dut.io.trapCause.expect(cause.U)
      }
  }
}
