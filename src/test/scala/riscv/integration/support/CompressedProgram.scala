package riscv.integration.support

import chisel3.{Bool, Clock, UInt}
import chiseltest._
import scala.collection.mutable
import scala.io.Source

private[integration] object CompressedProgram {
  // The same image and expected register values can be used with another core.
  def words: Seq[Long] = {
    val source = Source.fromResource("rv32c/integer-program.hex")
    try source.getLines().map(java.lang.Long.parseUnsignedLong(_, 16)).toVector
    finally source.close()
  }

  private val expected: Map[Int, BigInt] = Map(
    1 -> 126,
    2 -> 144,
    8 -> 132,
    9 -> -5,
    10 -> -4096,
    11 -> -5,
    12 -> 3,
    14 -> 6,
    15 -> 1,
    16 -> 102,
    20 -> 2,
    21 -> 1,
    22 -> 3,
    23 -> 3
  ).map { case (rd, value) => rd -> (BigInt(value) & 0xffffffffL) }

  def check(
      clock: Clock,
      writebackEnable: Bool,
      writebackRd: UInt,
      writebackData: UInt
  ): Unit = {
    val writes = mutable.Map.empty[Int, BigInt]
    var cycles = 0
    while (!writes.contains(31) && cycles < 256) {
      if (writebackEnable.peekBoolean()) {
        val rd = writebackRd.peekInt().toInt
        if (rd != 0) writes(rd) = writebackData.peekInt()
      }
      clock.step()
      cycles += 1
    }

    assert(writes.get(31).contains(BigInt(1)), "program did not complete")
    assert(!writes.contains(30), "an instruction on a skipped path wrote x30")
    expected.foreach { case (rd, value) =>
      assert(
        writes.get(rd).contains(value),
        s"x$rd expected $value, got ${writes.get(rd)}"
      )
    }
  }
}
