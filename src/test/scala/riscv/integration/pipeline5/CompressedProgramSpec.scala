package riscv.integration.pipeline5

import chiseltest._
import chisel3._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.pipeline.pipeline5.Pipeline5
import riscv.integration.support.CompressedProgram

class CompressedProgramSpec extends AnyFlatSpec with ChiselScalatestTester {
  it should "report commits in order, including instructions without register writes" in {
    val program =
      Seq(0x00100093L, 0x0330000fL, 0x00000073L, 0x00100073L, 0x00108113L)
    test(new Pipeline5(program, memoryWords = 16)) { dut =>
      val commits = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt)]
      for (_ <- 0 until 16) {
        if (dut.io.commitValid.peek().litToBoolean) {
          commits += ((
            dut.io.commitPc.peek().litValue,
            dut.io.commitInstr.peek().litValue
          ))
        }
        dut.clock.step()
      }
      assert(
        commits.toSeq == Seq(0, 4, 16).map(pc =>
          (BigInt(pc), BigInt(program(pc / 4)))
        )
      )
    }
  }

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
