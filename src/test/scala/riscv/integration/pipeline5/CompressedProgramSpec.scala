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
      val words = CompressedProgram.words
      val counts =
        scala.collection.mutable.Map.empty[String, Int].withDefaultValue(0)
      def hit(name: String): Unit = counts(name) += 1
      CompressedProgram.check(
        dut.clock,
        dut.io.writebackEnable,
        dut.io.writebackRd,
        dut.io.writebackData,
        () => {
          if (dut.io.stalled.peekBoolean()) hit("load-use stall")
          if (dut.io.commitValid.peekBoolean()) {
            val pc = dut.io.commitPc.peekInt().toInt
            val half = (words(pc / 4) >>> ((pc % 4) * 8)) & 0xffffL
            if ((half & 3) != 3) hit("16-bit commit")
            else {
              hit("32-bit commit")
              if (pc % 4 == 2) hit("cross-word commit")
            }
            val opcode = dut.io.commitInstr.peekInt().toInt & 0x7f
            hit(f"opcode 0x$opcode%02x")
          }
        }
      )
      Seq("16-bit commit", "32-bit commit", "cross-word commit").foreach {
        point =>
          assert(counts(point) > 0, s"coverage point missed: $point")
      }
      val report =
        java.nio.file.Paths.get("target/pipeline5-functional-coverage.tsv")
      val rows = counts.toSeq.sorted.map { case (name, count) =>
        s"$name\t$count"
      }
      java.nio.file.Files
        .writeString(report, ("point\thits" +: rows).mkString("", "\n", "\n"))
    }
  }
}
