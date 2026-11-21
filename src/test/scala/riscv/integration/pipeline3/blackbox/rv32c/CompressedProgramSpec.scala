package riscv.integration.blackbox.rv32c

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.integration.support.{CompressedProgram, Rv32iBlackBox}

class CompressedProgramSpec extends AnyFlatSpec with ChiselScalatestTester {
  it should "execute a mixed RV32I and compressed program through the public interface" in {
    test(new Rv32iBlackBox(CompressedProgram.words)) { dut =>
      CompressedProgram.check(
        dut.clock,
        dut.io.writebackEnable,
        dut.io.writebackRd,
        dut.io.writebackData
      )
    }
  }
}
