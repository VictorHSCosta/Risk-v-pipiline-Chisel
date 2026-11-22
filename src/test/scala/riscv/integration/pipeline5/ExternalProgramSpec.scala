package riscv.integration.pipeline5

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.pipeline.pipeline5.Pipeline5
import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters._

class ExternalProgramSpec extends AnyFlatSpec with ChiselScalatestTester {
  private def lines(path: String): Seq[String] =
    Files.readAllLines(Paths.get(path)).asScala.toVector

  it should "run an external program to its x31 completion marker" in {
    val image = sys.env.getOrElse("PIPELINE5_HEX", cancel("set PIPELINE5_HEX"))
    val words = lines(image).map(java.lang.Long.parseUnsignedLong(_, 16))
    require(words.nonEmpty && words.size <= 1024)
    val expected = sys.env.get("PIPELINE5_TRACE").map(lines)
    val limit = sys.env.getOrElse("PIPELINE5_CYCLES", "10000").toInt
    val depth = math.max(64, Integer.highestOneBit(words.size * 2 - 1))
    test(new Pipeline5(words, memoryWords = depth)) { dut =>
      val commits = Vector.newBuilder[String]
      var result = Option.empty[BigInt]
      for (_ <- 0 until limit if result.isEmpty) {
        assert(
          !dut.io.illegal.peekBoolean(),
          "program executed an illegal instruction"
        )
        if (dut.io.commitValid.peekBoolean()) {
          val pc = dut.io.commitPc.peekInt()
          val rd =
            if (dut.io.writebackEnable.peekBoolean())
              dut.io.writebackRd.peekInt().toInt
            else 0
          val data = if (rd == 0) BigInt(0) else dut.io.writebackData.peekInt()
          commits += f"${pc.toLong}%08x $rd%d ${data.toLong}%08x"
          if (rd == 31) result = Some(data)
        }
        dut.clock.step()
      }
      assert(
        result.contains(BigInt(1)),
        s"expected x31=1 before timeout, got $result"
      )
      expected.foreach(trace => assert(commits.result() == trace))
    }
  }
}
