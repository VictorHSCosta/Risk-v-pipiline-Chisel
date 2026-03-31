package riscv.elementosbasicos

import chisel3._

/**
  * Somador combinacional de 32 bits.
  *
  * Recebe dois operandos de 32 bits sem sinal e devolve a soma em 32 bits.
  * Em caso de overflow, o resultado segue o comportamento modular de 32 bits.
  */
class Somador32 extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(32.W))
    val b = Input(UInt(32.W))
    val soma = Output(UInt(32.W))
  })

  io.soma := io.a + io.b
}
