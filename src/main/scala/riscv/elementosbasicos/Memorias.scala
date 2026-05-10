package riscv.elementosbasicos

import chisel3._
import chisel3.util._

/**
  * Memoria de palavras de 32 bits com interface comum para uso em blocos basicos.
  *
  * Modelo simples para fase inicial do projeto:
  * - endereco em bytes (32 bits)
  * - indexacao por palavra (ignora os 2 bits menos significativos)
  * - escrita sincronizada ao clock
  * - leitura combinacional
  */
class CacheL1Base(val depthWords: Int = 1024, val allowWrite: Boolean = true, initialData: Seq[Long] = Seq.empty) extends Module {
  require(depthWords > 0, "depthWords deve ser maior que zero")

  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val writeEnable = Input(Bool())
    val readData = Output(UInt(32.W))
  })

  val paddedData = initialData.padTo(depthWords, 0L)
  private val mem = RegInit(VecInit(paddedData.map(_.U(32.W))))
  private val addrBits = log2Ceil(depthWords)
  private val wordAddress = io.address(addrBits + 1, 2)

  if (allowWrite) {
    when(io.writeEnable) {
      mem(wordAddress) := io.writeData
    }
  }

  io.readData := mem(wordAddress)
}

/**
  * Memoria de instrucoes.
  *
  * Nesta versao inicial, herda a mesma interface da memoria base,
  * mas bloqueia escrita para funcionar como somente leitura.
  */
class InstructionMemory(depthWords: Int = 1024, initialData: Seq[Long] = Seq.empty)
    extends CacheL1Base(depthWords = depthWords, allowWrite = false, initialData = initialData)

/**
  * Memoria de dados.
  *
  * Herda a interface da memoria base e permite leitura/escrita.
  */
class DataMemory(depthWords: Int = 1024)
    extends CacheL1Base(depthWords = depthWords, allowWrite = true)
