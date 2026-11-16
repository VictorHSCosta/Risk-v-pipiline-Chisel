package riscv.pipeline

import chiseltest._
import chisel3._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable.ArrayBuffer

/** Monta as instruções dos programas de teste sem depender de um arquivo HEX.
  */
object Rv32iCode {
  // ADDI x0,x0,0: ocupa espaço sem alterar o resultado do programa.
  val nop: Long = 0x00000013L

  // Cada formato coloca os campos da instrução nos espaços definidos pelo RV32I.
  // Os deslocamentos e máscaras abaixo são a montagem desses campos em uma palavra de 32 bits.
  // R recebe dois registradores; por exemplo, ADD rd, rs1, rs2.
  def r(f3: Int, f7: Int = 0, rd: Int = 5, rs1: Int = 1, rs2: Int = 2): Long =
    (f7.toLong << 25) | (rs2.toLong << 20) | (rs1.toLong << 15) | (f3.toLong << 12) | (rd.toLong << 7) | 0x33L

  // I recebe um registrador e um número; serve para ADDI e também para LOAD.
  def i(op: Int, f3: Int, rd: Int, rs1: Int, imm: Int): Long =
    ((imm & 0xfff).toLong << 20) | (rs1.toLong << 15) | (f3.toLong << 12) | (rd.toLong << 7) | op.toLong

  // Em STORE, o imediato fica dividido em duas partes na instrução.
  def s(f3: Int, rs1: Int, rs2: Int, imm: Int = 0): Long =
    (((imm >> 5) & 0x7f).toLong << 25) | (rs2.toLong << 20) | (rs1.toLong << 15) |
      (f3.toLong << 12) | ((imm & 31).toLong << 7) | 0x23L
  // B compara dois registradores; o imediato indica quantos bytes pular se a condição for verdadeira.
  def b(f3: Int, rs1: Int, rs2: Int, imm: Int = 8): Long =
    (((imm >> 12) & 1).toLong << 31) | (((imm >> 5) & 63).toLong << 25) |
      (rs2.toLong << 20) | (rs1.toLong << 15) | (f3.toLong << 12) |
      (((imm >> 1) & 15).toLong << 8) | (((imm >> 11) & 1).toLong << 7) | 0x63L
  // U guarda um número grande na parte superior da instrução (LUI/AUIPC).
  def u(op: Int, rd: Int, imm: Int): Long =
    (imm.toLong & 0xfffff000L) | (rd.toLong << 7) | op.toLong

  // J guarda o destino de um salto; como em B, os pedaços do endereço são reorganizados.
  def j(rd: Int, imm: Int): Long =
    (((imm >> 20) & 1).toLong << 31) | (((imm >> 1) & 1023).toLong << 21) |
      (((imm >> 11) & 1).toLong << 20) | (((imm >> 12) & 255).toLong << 12) |
      (rd.toLong << 7) | 0x6fL

  // Atalhos para os formatos I mais usados nos testes.
  def addi(rd: Int, rs1: Int, imm: Int): Long = i(0x13, 0, rd, rs1, imm)
  def load(f3: Int, rd: Int, rs1: Int, imm: Int = 0): Long =
    i(0x03, f3, rd, rs1, imm)
}

/** Adiciona saídas para o teste observar sinais internos, sem mudar o
  * funcionamento do pipeline.
  */
class TracedPipeline3(program: Seq[Long], words: Int = 64)
    extends Pipeline3(initialProgram = program, memoryWords = words) {
  // program é a sequência de instruções; words define o tamanho da memória do processador.
  // Estas janelas deixam o teste observar a busca e a instrução que avança pelo pipeline.
  val probeIdEx = IO(Output(new DecodeExecuteBundle)); probeIdEx := idEx
  val probeIfIdPc = IO(Output(UInt(32.W))); probeIfIdPc := ifIdPc
  val probeFetchInstr = IO(Output(UInt(32.W)));
  probeFetchInstr := fetch.io.instr

  // Mostram os valores lidos ou encaminhados para a instrução que está sendo preparada.
  val probeForwardedRs1 = IO(Output(UInt(32.W)));
  probeForwardedRs1 := forwardedRs1
  val probeForwardedRs2 = IO(Output(UInt(32.W)));
  probeForwardedRs2 := forwardedRs2
  val probeDecodeImm = IO(Output(UInt(32.W))); probeDecodeImm := immGen.io.imm
  val probeDecodeControl =
    IO(Output(new riscv.elementosbasicos.ControlSignals));
  probeDecodeControl := controller.io.signals
  val probeFlush = IO(Output(Bool())); probeFlush := flushPipeline

  // Mostram entradas e resultado da ULA, além da leitura e escrita na memória.
  val probeAluA = IO(Output(UInt(32.W))); probeAluA := ula.io.a
  val probeAluB = IO(Output(UInt(32.W))); probeAluB := ula.io.b
  val probeMemRead = IO(Output(UInt(32.W))); probeMemRead := dataMem.io.readData
  val probeMemWrite = IO(Output(Bool()));
  probeMemWrite := dataMem.io.writeEnable
}

/** Converte os números das instruções em nomes mais fáceis de reconhecer no
  * relatório.
  */
object Rv32iDisasm {
  // Nomes curtos usados nas montagens RISC-V: por exemplo, t0 é o registrador x5.
  private val abi = Array(
    "zero",
    "ra",
    "sp",
    "gp",
    "tp",
    "t0",
    "t1",
    "t2",
    "s0",
    "s1",
    "a0",
    "a1",
    "a2",
    "a3",
    "a4",
    "a5",
    "a6",
    "a7",
    "s2",
    "s3",
    "s4",
    "s5",
    "s6",
    "s7",
    "s8",
    "s9",
    "s10",
    "s11",
    "t3",
    "t4",
    "t5",
    "t6"
  )
  // Exemplo: x5 aparece como t0, junto do número e dos cinco bits do registrador.
  def reg(n: Int): String = s"${abi(n)}(x$n/${(n | 32).toBinaryString.drop(1)})"

  /** Lê os campos da instrução e monta algo como "ADD t0, t1, t2". */
  def apply(word: BigInt, imm: BigInt): String = {
    val op = (word & 127).toInt
    val f3 = ((word >> 12) & 7).toInt
    val f7 = ((word >> 25) & 127).toInt
    val rd = ((word >> 7) & 31).toInt
    val rs1 = ((word >> 15) & 31).toInt
    val rs2 = ((word >> 20) & 31).toInt
    // O opcode escolhe a família da instrução; funct3/funct7 escolhem a operação dentro dela.
    val mnemonic = op match {
      case 0x33 if f7 != 0 && f7 != 0x20 => "OP-EXT"
      case 0x33                          =>
        (f3, f7) match {
          case (0, 0x20) => "SUB"; case (5, 0x20) => "SRA"
          case (0, _)    => "ADD"; case (1, _)    => "SLL"; case (2, _) => "SLT"
          case (3, _)    => "SLTU"; case (4, _)   => "XOR"; case (5, _) => "SRL"
          case (6, _)    => "OR"; case (7, _)     => "AND"
        }
      case 0x13 =>
        f3 match {
          case 0 => "ADDI"; case 1 => "SLLI"; case 2 => "SLTI";
          case 3 => "SLTIU"
          case 4 => "XORI";
          case 5 => if (f7 == 0x20) "SRAI" else if (f7 == 0) "SRLI" else ".word"
          case 6 => "ORI"; case 7  => "ANDI"
        }
      case 0x03 =>
        Map(0 -> "LB", 1 -> "LH", 2 -> "LW", 4 -> "LBU", 5 -> "LHU")
          .getOrElse(f3, "LOAD?")
      case 0x23 => Map(0 -> "SB", 1 -> "SH", 2 -> "SW").getOrElse(f3, "STORE?")
      case 0x63 =>
        Map(
          0 -> "BEQ",
          1 -> "BNE",
          4 -> "BLT",
          5 -> "BGE",
          6 -> "BLTU",
          7 -> "BGEU"
        ).getOrElse(f3, "BRANCH?")
      case 0x37 => "LUI"; case 0x17  => "AUIPC"; case 0x6f => "JAL"
      case 0x67 => "JALR"; case 0x0f => "FENCE"
      case 0x73 =>
        if (word == 0x73) "ECALL"
        else if (word == 0x100073) "EBREAK"
        else "SYSTEM"
      case _ => ".word"
    }
    // Os argumentos impressos dependem do formato: registradores, imediato ou endereço.
    val args = op match {
      case 0x33               => s"${reg(rd)}, ${reg(rs1)}, ${reg(rs2)}"
      case 0x13 | 0x03 | 0x67 => s"${reg(rd)}, ${reg(rs1)}, ${imm.toLong.toInt}"
      case 0x23 => s"${reg(rs2)}, ${imm.toLong.toInt}(${reg(rs1)})"
      case 0x63 => s"${reg(rs1)}, ${reg(rs2)}, ${imm.toLong.toInt}"
      case 0x37 | 0x17 | 0x6f => s"${reg(rd)}, ${imm.toLong.toInt}"
      case _                  => ""
    }
    s"$mnemonic $args".trim
  }
}

/** Imprime o que o pipeline está vendo em cada ciclo; RV32I_TRACE=1 ativa isso
  * nos testes.
  */
object Rv32iTrace {
  // Mantém os valores no tamanho de 32 bits e facilita a leitura em hexadecimal.
  private def hex(n: BigInt): String =
    f"${(n & BigInt(0xffffffffL)).toLong}%08x"
  // Torna sinais ligados/desligados mais fáceis de ver no terminal.
  private def bit(n: Boolean): Int = if (n) 1 else 0
  // Nomes para os números de operação usados internamente pelo processador.
  private val aluNames = Array(
    "ADD",
    "SUB",
    "AND",
    "OR",
    "XOR",
    "SLL",
    "SRL",
    "SRA",
    "SLT",
    "SLTU",
    "MUL",
    "MULH",
    "MULHU",
    "MULHSU"
  )
  private val branchNames =
    Array("nenhum", "BEQ", "BNE", "BLT", "BGE", "BLTU", "BGEU")
  private val wbNames = Array("ALU", "MEM", "PC+tamanho", "IMM", "CSR")
  private def name(names: Array[String], n: BigInt): String =
    names.lift(n.toInt).getOrElse(s"$n")
  // Em instruções imediatas, alguns bits parecem indicar rs2, mas na verdade são parte do número.
  private def usesRs1(op: Int): Boolean =
    Set(0x33, 0x13, 0x03, 0x23, 0x63, 0x67).contains(op)
  private def usesRs2(op: Int): Boolean = Set(0x33, 0x23, 0x63).contains(op)

  /** Imprime uma fotografia do pipeline antes de avançar o relógio para o
    * próximo ciclo.
    */
  def print(dut: TracedPipeline3, cycle: Int): Unit = {
    val id = dut.io.instr.peekInt()
    val ex = dut.probeIdEx.instr.peekInt()
    val idRs1 = ((id >> 15) & 31).toInt
    val idRs2 = ((id >> 20) & 31).toInt
    val idRd = ((id >> 7) & 31).toInt
    val idOp = (id & 127).toInt
    val exOp = (ex & 127).toInt
    val c = dut.probeDecodeControl
    val wb =
      if (
        dut.io.writebackEnable
          .peekBoolean() && dut.io.writebackRd.peekInt() != 0
      )
        s"x${dut.io.writebackRd.peekInt()}=0x${hex(dut.io.writebackData.peekInt())}"
      else "-"
    val id1 =
      if (usesRs1(idOp))
        s"${Rv32iDisasm.reg(idRs1)}:0x${hex(dut.probeForwardedRs1.peekInt())}"
      else s"${Rv32iDisasm.reg(idRs1)} (não usado)"
    val id2 =
      if (usesRs2(idOp))
        s"${Rv32iDisasm.reg(idRs2)}:0x${hex(dut.probeForwardedRs2.peekInt())}"
      else s"${Rv32iDisasm.reg(idRs2)} (bits do imediato)"
    val exRs1 = dut.probeIdEx.rs1.peekInt().toInt
    val exRs2 = dut.probeIdEx.rs2.peekInt().toInt
    val ex1 =
      if (usesRs1(exOp))
        s"${Rv32iDisasm.reg(exRs1)}:0x${hex(dut.probeIdEx.rs1Value.peekInt())}"
      else "não usado"
    val ex2 =
      if (usesRs2(exOp))
        s"${Rv32iDisasm.reg(exRs2)}:0x${hex(dut.probeIdEx.rs2Value.peekInt())}"
      else "não usado"

    // IF busca; ID entende e prepara; EX calcula; MEM acessa dados; WB grava no registrador.
    println(
      f"ciclo $cycle%02d | IF pc=0x${hex(dut.io.pc.peekInt())} instr=0x${hex(dut.probeFetchInstr.peekInt())} | stall=${bit(dut.io.stalled.peekBoolean())} flush=${bit(dut.probeFlush.peekBoolean())}"
    )
    println(
      s"  ID pc=0x${hex(dut.probeIfIdPc.peekInt())} instr=0x${hex(id)} ${Rv32iDisasm(id, dut.probeDecodeImm.peekInt())} | opcode=0x${idOp.toHexString} rd=${Rv32iDisasm.reg(idRd)} rs1=$id1 rs2=$id2 imm=0x${hex(dut.probeDecodeImm.peekInt())}"
    )
    val active = Seq(
      if (c.regWrite.peekBoolean()) "regWrite" else "",
      if (c.memWrite.peekBoolean()) "memWrite" else "",
      if (c.memUnsigned.peekBoolean()) "unsignedLoad" else "",
      if (c.jump.peekBoolean()) "jump" else "",
      if (c.jalr.peekBoolean()) "jalr" else "",
      if (c.illegal.peekBoolean()) "illegal" else ""
    ).filter(_.nonEmpty).mkString(",")
    val opA = Array("rs1", "PC", "zero")
      .lift(c.operandASel.peekInt().toInt)
      .getOrElse("?")
    val opB = Array("rs2", "imediato")
      .lift(c.operandBSel.peekInt().toInt)
      .getOrElse("?")
    val memSize = Array("byte", "half", "word")
      .lift(c.memSize.peekInt().toInt)
      .getOrElse("?")
    println(
      s"     sinais ativos=[$active] aluOp=${name(aluNames, c.aluOp.peekInt())} opA=$opA opB=$opB memSize=$memSize branch=${name(branchNames, c.branchType.peekInt())} wbSel=${name(wbNames, c.writebackSel.peekInt())}"
    )
    println(
      s"  EX pc=0x${hex(dut.probeIdEx.pc.peekInt())} instr=0x${hex(ex)} ${Rv32iDisasm(ex, dut.probeIdEx.imm.peekInt())} | opcode=0x${exOp.toHexString} valid=${bit(dut.probeIdEx.valid.peekBoolean())} rs1=$ex1 rs2=$ex2 imm=0x${hex(dut.probeIdEx.imm.peekInt())}"
    )
    val mem = if (exOp == 0x03 || exOp == 0x23)
      s"endereço=0x${hex(dut.probeIdEx.memAddress.peekInt())} escrita=0x${hex(dut.probeIdEx.memWriteData.peekInt())} write=${bit(dut.probeMemWrite.peekBoolean())} leitura=0x${hex(dut.probeMemRead.peekInt())}"
    else "inativa"
    println(
      s"     ALU A=0x${hex(dut.probeAluA.peekInt())} B=0x${hex(dut.probeAluB.peekInt())} resultado=0x${hex(dut.io.aluResult.peekInt())} | MEM $mem | WB=$wb"
    )
  }
}

trait Rv32iBench extends AnyFlatSpec with ChiselScalatestTester {
  // Permite chamar addi/load/etc. diretamente nos arquivos de teste.
  import Rv32iCode._

  /** Executa o programa e compara as gravações reais com os resultados
    * esperados. `program` são as instruções; `expected` diz o que deve ser
    * gravado. `absent` lista registradores que não podem ser escritos;
    * `expectStall` diz se esperamos uma pausa. `cycles` escolhe a duração; zero
    * usa o tamanho do programa mais alguns ciclos para esvaziar o pipeline.
    */
  protected def check(
      program: Seq[Long],
      expected: Map[Int, BigInt],
      absent: Set[Int] = Set.empty,
      expectStall: Boolean = false,
      cycles: Int = 0
  ): Unit = {
    test(new TracedPipeline3(program)) { dut =>
      // Registra o que o processador realmente tenta gravar e quantas pausas faz.
      val writes = ArrayBuffer.empty[(Int, BigInt)]
      var stalls = 0
      val steps = if (cycles == 0) program.size + 6 else cycles
      for (cycle <- 0 until steps) {
        // Primeiro observa o ciclo atual; depois avança o relógio.
        if (sys.env.get("RV32I_TRACE").contains("1"))
          Rv32iTrace.print(dut, cycle)
        if (dut.io.stalled.peekBoolean()) stalls += 1
        // A memória após o programa tem zeros; só cobramos instruções que pertencem ao programa.
        if (
          dut.probeIdEx.valid
            .peekBoolean() && dut.probeIdEx.pc.peekInt() < program.size * 4
        )
          assert(
            !dut.io.illegal.peekBoolean(),
            s"instrução ilegal em PC 0x${dut.probeIdEx.pc.peekInt().toString(16)}"
          )
        // x0 é fixo em zero pela arquitetura; gravações nele não contam como resultado.
        if (
          dut.io.writebackEnable
            .peekBoolean() && dut.io.writebackRd.peekInt() != 0
        )
          writes += ((
            dut.io.writebackRd.peekInt().toInt,
            dut.io.writebackData.peekInt()
          ))
        dut.clock.step()
      }

      // Cada registrador esperado deve receber exatamente um valor igual ao informado.
      // BigInt é limitado a 32 bits para comparar também resultados negativos, como -128.
      expected.foreach { case (rd, value) =>
        assert(
          writes.collect { case (`rd`, data) => data }.toSeq == Seq(
            value & BigInt(0xffffffffL)
          ),
          s"x$rd esperado 0x${value.toString(16)} uma vez; escritas: $writes"
        )
      }
      absent.foreach(rd =>
        assert(
          !writes.exists(_._1 == rd),
          s"x$rd deveria ser descartado; escritas: $writes"
        )
      )
      if (expectStall)
        assert(stalls == 1, s"esperado um ciclo de stall, observado $stalls")
      else assert(stalls == 0, s"stall inesperado: $stalls ciclos")
    }
  }

  /** Prepara x1=-8 e x2=3 antes da instrução aritmética recebida. */
  protected def registers(target: Long): Seq[Long] =
    Seq(addi(1, 0, -8), addi(2, 0, 3), target, nop)
}
