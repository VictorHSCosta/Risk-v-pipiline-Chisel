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
  * Modelo byte-addressable para loads/stores RV32I de byte, halfword e word.
  * A politica de trap para acessos desalinhados deve ser definida fora deste
  * bloco quando o pipeline ganhar controle de excecoes.
  */
class DataMemory(depthWords: Int = 1024) extends Module {
  require(depthWords > 0, "depthWords deve ser maior que zero")

  private val depthBytes = depthWords * 4
  private val addrBits = log2Ceil(depthBytes)

  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val writeData = Input(UInt(32.W))
    val writeEnable = Input(Bool())
    val memSize = Input(UInt(2.W))
    val unsignedLoad = Input(Bool())
    val readData = Output(UInt(32.W))
  })

  import RV32I.MemorySize

  val mem = RegInit(VecInit(Seq.fill(depthBytes)(0.U(8.W))))
  val byteAddress = io.address(addrBits - 1, 0)

  val byte0 = mem(byteAddress)
  val byte1 = mem(byteAddress + 1.U)
  val byte2 = mem(byteAddress + 2.U)
  val byte3 = mem(byteAddress + 3.U)
  val half = Cat(byte1, byte0)
  val word = Cat(byte3, byte2, byte1, byte0)

  io.readData := word
  switch(io.memSize) {
    is(MemorySize.BYTE) {
      io.readData := Mux(io.unsignedLoad, Cat(0.U(24.W), byte0), Cat(Fill(24, byte0(7)), byte0))
    }
    is(MemorySize.HALF) {
      io.readData := Mux(io.unsignedLoad, Cat(0.U(16.W), half), Cat(Fill(16, half(15)), half))
    }
    is(MemorySize.WORD) {
      io.readData := word
    }
  }

  when(io.writeEnable) {
    switch(io.memSize) {
      is(MemorySize.BYTE) {
        mem(byteAddress) := io.writeData(7, 0)
      }
      is(MemorySize.HALF) {
        mem(byteAddress) := io.writeData(7, 0)
        mem(byteAddress + 1.U) := io.writeData(15, 8)
      }
      is(MemorySize.WORD) {
        mem(byteAddress) := io.writeData(7, 0)
        mem(byteAddress + 1.U) := io.writeData(15, 8)
        mem(byteAddress + 2.U) := io.writeData(23, 16)
        mem(byteAddress + 3.U) := io.writeData(31, 24)
      }
    }
  }
}
