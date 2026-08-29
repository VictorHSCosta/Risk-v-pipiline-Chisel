package riscv.pipeline

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Pipeline3Spec
    extends AnyFlatSpec
    with ChiselScalatestTester
    with Matchers {
  behavior of "Pipeline3"

  private case class Commit(rd: Int, data: BigInt)

  private val Nop = addi(0, 0, 0)

  private def u32(value: BigInt): BigInt = value & BigInt("ffffffff", 16)

  private def r(
      funct7: Int,
      rs2: Int,
      rs1: Int,
      funct3: Int,
      rd: Int,
      opcode: Int = 0x33
  ): Long = {
    ((funct7 & 0x7f).toLong << 25) |
      ((rs2 & 0x1f).toLong << 20) |
      ((rs1 & 0x1f).toLong << 15) |
      ((funct3 & 0x7).toLong << 12) |
      ((rd & 0x1f).toLong << 7) |
      (opcode & 0x7f).toLong
  }

  private def i(
      imm: Int,
      rs1: Int,
      funct3: Int,
      rd: Int,
      opcode: Int
  ): Long = {
    ((imm & 0xfff).toLong << 20) |
      ((rs1 & 0x1f).toLong << 15) |
      ((funct3 & 0x7).toLong << 12) |
      ((rd & 0x1f).toLong << 7) |
      (opcode & 0x7f).toLong
  }

  private def s(imm: Int, rs2: Int, rs1: Int, funct3: Int): Long = {
    val encodedImm = imm & 0xfff
    (((encodedImm >> 5) & 0x7f).toLong << 25) |
      ((rs2 & 0x1f).toLong << 20) |
      ((rs1 & 0x1f).toLong << 15) |
      ((funct3 & 0x7).toLong << 12) |
      ((encodedImm & 0x1f).toLong << 7) |
      0x23L
  }

  private def b(imm: Int, rs2: Int, rs1: Int, funct3: Int): Long = {
    val encodedImm = imm & 0x1fff
    (((encodedImm >> 12) & 0x1).toLong << 31) |
      (((encodedImm >> 5) & 0x3f).toLong << 25) |
      ((rs2 & 0x1f).toLong << 20) |
      ((rs1 & 0x1f).toLong << 15) |
      ((funct3 & 0x7).toLong << 12) |
      (((encodedImm >> 1) & 0xf).toLong << 8) |
      (((encodedImm >> 11) & 0x1).toLong << 7) |
      0x63L
  }

  private def u(imm20: Int, rd: Int, opcode: Int): Long = {
    ((imm20 & 0xfffff).toLong << 12) |
      ((rd & 0x1f).toLong << 7) |
      (opcode & 0x7f).toLong
  }

  private def j(imm: Int, rd: Int): Long = {
    val encodedImm = imm & 0x1fffff
    (((encodedImm >> 20) & 0x1).toLong << 31) |
      (((encodedImm >> 12) & 0xff).toLong << 12) |
      (((encodedImm >> 11) & 0x1).toLong << 20) |
      (((encodedImm >> 1) & 0x3ff).toLong << 21) |
      ((rd & 0x1f).toLong << 7) |
      0x6fL
  }

  private def addi(rd: Int, rs1: Int, imm: Int): Long =
    i(imm, rs1, funct3 = 0, rd, opcode = 0x13)

  private def slti(rd: Int, rs1: Int, imm: Int): Long =
    i(imm, rs1, funct3 = 2, rd, opcode = 0x13)

  private def add(rd: Int, rs1: Int, rs2: Int): Long =
    r(funct7 = 0x00, rs2, rs1, funct3 = 0, rd)

  private def sub(rd: Int, rs1: Int, rs2: Int): Long =
    r(funct7 = 0x20, rs2, rs1, funct3 = 0, rd)

  private def lw(rd: Int, rs1: Int, imm: Int): Long =
    i(imm, rs1, funct3 = 2, rd, opcode = 0x03)

  private def sw(rs2: Int, rs1: Int, imm: Int): Long =
    s(imm, rs2, rs1, funct3 = 2)

  private def beq(rs1: Int, rs2: Int, imm: Int): Long =
    b(imm, rs2, rs1, funct3 = 0)

  private def bne(rs1: Int, rs2: Int, imm: Int): Long =
    b(imm, rs2, rs1, funct3 = 1)

  private def blt(rs1: Int, rs2: Int, imm: Int): Long =
    b(imm, rs2, rs1, funct3 = 4)

  private def lui(rd: Int, imm20: Int): Long = u(imm20, rd, opcode = 0x37)

  private def auipc(rd: Int, imm20: Int): Long = u(imm20, rd, opcode = 0x17)

  private def jal(rd: Int, imm: Int): Long = j(imm, rd)

  private def jalr(rd: Int, rs1: Int, imm: Int): Long =
    i(imm, rs1, funct3 = 0, rd, opcode = 0x67)

  private def invalidR(rd: Int): Long =
    r(funct7 = 0x7f, rs2 = 0, rs1 = 0, funct3 = 0, rd)

  private def runProgram(
      program: Seq[Long],
      cycles: Int = 24,
      memoryWords: Int = 64
  ): Seq[Commit] = {
    var commits = Vector.empty[Commit]

    test(new Pipeline3(initialProgram = program, memoryWords = memoryWords)) {
      dut =>
        for (_ <- 0 until cycles) {
          dut.clock.step(1)

          val writesRegister = dut.io.writebackEnable.peek().litToBoolean
          val rd = dut.io.writebackRd.peekInt().toInt
          if (writesRegister && rd != 0) {
            commits = commits :+ Commit(rd, u32(dut.io.writebackData.peekInt()))
          }
        }
    }

    commits
  }

  private def expectProgram(
      program: Seq[Long],
      expected: Seq[Commit],
      cycles: Int = 24
  ): Unit = {
    runProgram(program :+ Nop, cycles) shouldBe expected
  }

  it should "executar uma sequencia aritmetica pequena com forwarding basico" in {
    val program = Seq(
      addi(1, 0, 5), // x1 = 5
      addi(2, 1, 3), // x2 = x1 + 3
      add(3, 1, 2) // x3 = x1 + x2
    )

    expectProgram(
      program,
      Seq(Commit(1, 5), Commit(2, 8), Commit(3, 13))
    )
  }

  it should "encadear dependencias RAW consecutivas por forwarding" in {
    val program = Seq(
      addi(1, 0, 1),
      addi(1, 1, 1),
      addi(1, 1, 1),
      addi(2, 1, 7)
    )

    expectProgram(
      program,
      Seq(Commit(1, 1), Commit(1, 2), Commit(1, 3), Commit(2, 10))
    )
  }

  it should "preservar x0 mesmo quando uma instrucao tenta escrever nele" in {
    val program = Seq(
      addi(0, 0, 99),
      addi(1, 0, 7)
    )

    expectProgram(program, Seq(Commit(1, 7)))
  }

  it should "fazer store de dado recem produzido e carregar esse valor depois" in {
    val program = Seq(
      addi(1, 0, 20), // endereco
      addi(2, 0, 77), // dado
      sw(2, 1, 0),
      lw(3, 1, 0)
    )

    expectProgram(
      program,
      Seq(Commit(1, 20), Commit(2, 77), Commit(3, 77))
    )
  }

  it should "usar imediatamente o resultado de um load na instrucao seguinte" in {
    val program = Seq(
      addi(1, 0, 24),
      addi(2, 0, 41),
      sw(2, 1, 0),
      lw(3, 1, 0),
      addi(4, 3, 1)
    )

    expectProgram(
      program,
      Seq(Commit(1, 24), Commit(2, 41), Commit(3, 41), Commit(4, 42))
    )
  }

  it should "fazer flush da instrucao seguinte quando um branch e tomado" in {
    val program = Seq(
      addi(1, 0, 5),
      addi(2, 0, 5),
      beq(1, 2, 8),
      addi(3, 0, 99), // deve ser descartada pelo flush
      addi(3, 0, 42)
    )

    expectProgram(
      program,
      Seq(Commit(1, 5), Commit(2, 5), Commit(3, 42))
    )
  }

  it should "manter a instrucao seguinte quando um branch nao e tomado" in {
    val program = Seq(
      addi(1, 0, 5),
      addi(2, 0, 6),
      beq(1, 2, 8),
      addi(3, 0, 11),
      addi(4, 0, 22)
    )

    expectProgram(
      program,
      Seq(Commit(1, 5), Commit(2, 6), Commit(3, 11), Commit(4, 22))
    )
  }

  it should "resolver tipos diferentes de branch com operandos encaminhados" in {
    val program = Seq(
      addi(1, 0, 3),
      addi(2, 0, 7),
      blt(1, 2, 8),
      addi(3, 0, 99), // descartada
      addi(3, 0, 12),
      bne(1, 2, 8),
      addi(4, 0, 99), // descartada
      addi(4, 0, 34)
    )

    expectProgram(
      program,
      Seq(Commit(1, 3), Commit(2, 7), Commit(3, 12), Commit(4, 34))
    )
  }

  it should "executar jal gravando pc mais quatro e pulando a instrucao seguinte" in {
    val program = Seq(
      jal(1, 8),
      addi(2, 0, 99), // descartada
      addi(2, 0, 55)
    )

    expectProgram(program, Seq(Commit(1, 4), Commit(2, 55)))
  }

  it should "executar jalr com alvo calculado por registrador e imediato" in {
    val program = Seq(
      addi(5, 0, 16),
      jalr(1, 5, 0),
      addi(2, 0, 99), // descartada
      addi(3, 0, 88), // descartada pelo salto para PC 16
      addi(2, 0, 66)
    )

    expectProgram(program, Seq(Commit(5, 16), Commit(1, 8), Commit(2, 66)))
  }

  it should "executar lui e auipc com os valores de imediato e pc corretos" in {
    val program = Seq(
      lui(1, 0x12345),
      auipc(2, 0x10),
      add(3, 1, 2)
    )

    expectProgram(
      program,
      Seq(
        Commit(1, BigInt("12345000", 16)),
        Commit(2, BigInt("00010004", 16)),
        Commit(3, BigInt("12355004", 16))
      )
    )
  }

  it should "nao escrever registrador quando a instrucao e ilegal" in {
    val program = Seq(
      invalidR(5),
      addi(6, 5, 1)
    )

    expectProgram(program, Seq(Commit(6, 1)))
  }

  it should "cobrir comparacao signed no caminho completo do pipeline" in {
    val program = Seq(
      addi(1, 0, -1),
      slti(2, 1, 0),
      slti(3, 0, -1),
      sub(4, 2, 3)
    )

    expectProgram(
      program,
      Seq(
        Commit(1, BigInt("ffffffff", 16)),
        Commit(2, 1),
        Commit(3, 0),
        Commit(4, 1)
      )
    )
  }
}
