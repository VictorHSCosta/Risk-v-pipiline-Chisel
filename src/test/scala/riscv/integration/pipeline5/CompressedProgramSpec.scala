package riscv.integration.pipeline5

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.pipeline.pipeline5.Pipeline5
import riscv.integration.support.CompressedProgram

class CompressedProgramSpec extends AnyFlatSpec with ChiselScalatestTester {
  it should "execute a mixed RV32I and compressed program through the public interface" in {
    test(new Pipeline5(CompressedProgram.words, memoryWords = 64)) { dut =>
      CompressedProgram.check(
        dut.clock,
        dut.io.writebackEnable,
        dut.io.writebackRd,
        dut.io.writebackData
      )
    }
  }
}
