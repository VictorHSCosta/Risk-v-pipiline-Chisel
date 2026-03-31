package riscv.elementosbasicos

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class Somador32Test extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Somador32"

  it should "somar dois valores de 32 bits" in {
    test(new Somador32) { dut =>
      dut.io.a.poke(10.U(32.W))
      dut.io.b.poke(5.U(32.W))
      dut.clock.step(1)
      dut.io.soma.expect(15.U(32.W))
    }
  }

  it should "fazer wrap-around em overflow de 32 bits" in {
    test(new Somador32) { dut =>
      dut.io.a.poke("hFFFFFFFF".U(32.W))
      dut.io.b.poke(1.U(32.W))
      dut.clock.step(1)
      dut.io.soma.expect(0.U(32.W))
    }
  }
}
