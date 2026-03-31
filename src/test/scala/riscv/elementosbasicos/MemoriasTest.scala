package riscv.elementosbasicos

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MemoriasTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "DataMemory"

  it should "escrever e ler uma palavra de 32 bits" in {
    test(new DataMemory(depthWords = 16)) { dut =>
      dut.io.address.poke(8.U(32.W)) // palavra de indice 2
      dut.io.writeData.poke("hDEADBEEF".U(32.W))
      dut.io.writeEnable.poke(true.B)
      dut.clock.step(1)

      dut.io.writeEnable.poke(false.B)
      dut.io.address.poke(8.U(32.W))
      dut.clock.step(1)

      dut.io.readData.expect("hDEADBEEF".U(32.W))
    }
  }

  behavior of "InstructionMemory"

  it should "ignorar escrita e permanecer somente leitura na versao inicial" in {
    test(new InstructionMemory(depthWords = 16)) { dut =>
      dut.io.address.poke(12.U(32.W)) // palavra de indice 3
      dut.io.writeData.poke("h12345678".U(32.W))
      dut.io.writeEnable.poke(true.B)
      dut.clock.step(1)

      dut.io.writeEnable.poke(false.B)
      dut.io.address.poke(12.U(32.W))
      dut.clock.step(1)

      dut.io.readData.expect(0.U(32.W))
    }
  }
}
