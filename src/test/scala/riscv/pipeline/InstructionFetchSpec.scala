package riscv.pipeline

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable.ArrayBuffer

class InstructionFetchSpec extends AnyFlatSpec with ChiselScalatestTester {
  import Rv32iCode._

  // Empacota bytes consecutivos: uma instrução de 32 bits pode cruzar palavras.
  private def pack(instructions: (Long, Int)*): Seq[Long] = {
    val bytes = instructions.flatMap { case (instr, size) =>
      (0 until size).map(i => (instr >> (8 * i)) & 0xffL)
    }
    bytes.grouped(4).map(_.zipWithIndex.map { case (byte, i) =>
      byte << (8 * i)
    }.foldLeft(0L)(_ | _)).toSeq
  }

  behavior of "InstructionFetch"

  it should "reutilizar metades e montar a sequencia 16, 32, 32, 16 bits" in {
    val b = addi(2, 1, 3)
    val c = r(0, rd = 3, rs1 = 1, rs2 = 2)
    val words = pack((0x4095L, 2), (b, 4), (c, 4), (0x0185L, 2),
      (0x0001L, 2), (0x0001L, 2), (nop, 4))
    test(new InstructionFetch) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.flush.poke(false.B)
      for ((pc, data, instr, size, address) <- Seq(
        (0, words(0), addi(1, 0, 5), 2, 0),
        (2, words(1), b, 4, 4),
        (6, words(2), c, 4, 8),
        (10, 0L, addi(3, 3, 1), 2, -1),
        (12, words(3), nop, 2, 12),
        (14, 0L, nop, 2, -1),
        (16, words(4), nop, 4, 16)
      )) {
        dut.io.pc.poke(pc.U)
        dut.io.memoryData.poke(data.U)
        dut.io.valid.expect(true.B)
        dut.io.illegal.expect(false.B)
        dut.io.instr.expect(instr.U)
        dut.io.length.expect(size.U)
        dut.io.memoryReadEnable.expect((address >= 0).B)
        if (address >= 0) dut.io.memoryAddress.expect(address.U)
        dut.clock.step()
      }
    }
  }

  it should "esperar a continuacao apos salto, congelar no stall e invalidar no flush" in {
    val instr = addi(5, 0, 42)
    val words = pack((0x0001L, 2), (instr, 4), (0x0001L, 2))
    test(new InstructionFetch) { dut =>
      dut.io.pc.poke(2.U)
      dut.io.enable.poke(true.B)
      dut.io.flush.poke(false.B)
      dut.io.memoryData.poke(words(0).U)
      dut.io.memoryReadEnable.expect(true.B)
      dut.io.memoryAddress.expect(0.U)
      dut.io.valid.expect(false.B)
      dut.clock.step()

      dut.io.enable.poke(false.B)
      dut.io.memoryData.poke(0.U)
      dut.io.memoryReadEnable.expect(false.B)
      dut.io.valid.expect(false.B)
      dut.clock.step()

      dut.io.enable.poke(true.B)
      dut.io.memoryData.poke(words(1).U)
      dut.io.memoryAddress.expect(4.U)
      dut.io.valid.expect(true.B)
      dut.io.instr.expect(instr.U)
      dut.clock.step()

      dut.io.pc.poke(6.U)
      dut.io.memoryData.poke(0.U)
      dut.io.memoryReadEnable.expect(false.B)
      dut.io.instr.expect(nop.U)
      dut.io.flush.poke(true.B)
      dut.io.valid.expect(false.B)
      dut.clock.step()

      dut.io.flush.poke(false.B)
      dut.io.memoryData.poke(words(1).U)
      dut.io.memoryReadEnable.expect(true.B)
      dut.io.memoryAddress.expect(4.U)
      dut.io.instr.expect(nop.U)
    }
  }

  it should "preservar ilegalidade de comprimidas e rejeitar comprimentos maiores" in {
    test(new InstructionFetch) { dut =>
      dut.io.enable.poke(true.B)
      dut.io.flush.poke(false.B)
      for ((pc, instr) <- Seq((0, 0L), (4, 0x001fL))) {
        dut.io.pc.poke(pc.U)
        dut.io.memoryData.poke(instr.U)
        dut.io.valid.expect(true.B)
        dut.io.illegal.expect(true.B)
        dut.io.instr.expect(nop.U)
        dut.clock.step()
      }
    }
  }

  behavior of "Pipeline3 com RVC"

  it should "executar instrucoes misturadas sem repetir a instrucao anterior nas bolhas" in {
    val program = pack(
      (0x4095L, 2), // c.li x1, 5
      (addi(2, 1, 3), 4),
      (r(0, rd = 3, rs1 = 1, rs2 = 2), 4),
      (0x0185L, 2), // c.addi x3, 1
      (j(0, 0), 4)
    )
    test(new Pipeline3(initialProgram = program, memoryWords = 32)) { dut =>
      val writes = ArrayBuffer.empty[(Int, BigInt)]
      for (_ <- 0 until 20) {
        if (dut.io.writebackEnable.peekBoolean() && dut.io.writebackRd.peekInt() != 0)
          writes += ((dut.io.writebackRd.peekInt().toInt, dut.io.writebackData.peekInt()))
        dut.clock.step()
      }
      assert(writes.toSeq == Seq(1 -> BigInt(5), 2 -> BigInt(8),
        3 -> BigInt(13), 3 -> BigInt(14)))
    }
  }

  it should "gravar PC+2 em C.JAL e C.JALR e descartar o caminho errado" in {
    val program = pack(
      (0x2019L, 2), // PC 0: c.jal +6, ra = 2
      (addi(10, 0, 99), 4), // PC 2: descartada
      (0x42a5L, 2), // PC 6: c.li x5, 9
      (0x4139L, 2), // PC 8: c.li x2, 14
      (0x9102L, 2), // PC 10: c.jalr x2, ra = 12
      (0x459dL, 2), // PC 12: c.li x11, 7, descartada
      (0x4329L, 2), // PC 14: c.li x6, 10
      (j(0, 0), 4)
    )
    test(new Pipeline3(initialProgram = program, memoryWords = 32)) { dut =>
      val writes = ArrayBuffer.empty[(Int, BigInt)]
      for (_ <- 0 until 24) {
        if (dut.io.writebackEnable.peekBoolean() && dut.io.writebackRd.peekInt() != 0)
          writes += ((dut.io.writebackRd.peekInt().toInt, dut.io.writebackData.peekInt()))
        dut.clock.step()
      }
      assert(writes.toSeq == Seq(1 -> BigInt(2), 5 -> BigInt(9),
        2 -> BigInt(14), 1 -> BigInt(12), 6 -> BigInt(10)))
    }
  }

  it should "manter load-use e forwarding quando a instrucao dependente e comprimida" in {
    val program = pack(
      (addi(1, 0, 64), 4), (addi(2, 0, 42), 4),
      (s(2, 1, 2), 4), (load(2, 3, 1), 4),
      (0x0185L, 2), // c.addi x3, 1
      (addi(4, 3, 2), 4), (j(0, 0), 4)
    )
    test(new Pipeline3(initialProgram = program, memoryWords = 32)) { dut =>
      val results = ArrayBuffer.empty[BigInt]
      var stalls = 0
      for (_ <- 0 until 24) {
        if (dut.io.stalled.peekBoolean()) stalls += 1
        if (dut.io.writebackEnable.peekBoolean() && dut.io.writebackRd.peekInt() == 4)
          results += dut.io.writebackData.peekInt()
        dut.clock.step()
      }
      assert(stalls == 1)
      assert(results.toSeq == Seq(BigInt(45)))
    }
  }
}
