package riscv.elementosbasicos

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class UnicicloSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Uniciclo"

  it should "executar um pequeno programa em linguagem de maquina (ADDI e ADD)" in {
    // Programa:
    // 1. ADDI x1, x0, 10 -> 0x00A00093
    // 2. ADD x2, x1, x1  -> 0x00108133
    // 3. ADDI x0, x0, 0  -> 0x00000013 (NOP)
    val program = Seq(
      0x00A00093L,
      0x00108133L,
      0x00000013L
    )

    test(new Uniciclo(program)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Ciclo 0: PC = 0. A instrucao ADDI x1, x0, 10 esta sendo lida e executada.
      dut.io.pcOut.expect(0.U)
      dut.io.ulaResultOut.expect(10.U) // ULA calcula x0(0) + 10 = 10
      dut.clock.step(1) // clock avanca na borda de subida, x1 guarda 10

      // Ciclo 1: PC = 4. A instrucao ADD x2, x1, x1 esta sendo executada.
      dut.io.pcOut.expect(4.U)
      dut.io.ulaResultOut.expect(20.U) // ULA calcula x1(10) + x1(10) = 20
      dut.clock.step(1) // clock avanca, x2 guarda 20

      // Ciclo 2: PC = 8. NOP.
      dut.io.pcOut.expect(8.U)
      dut.io.ulaResultOut.expect(0.U) // ULA calcula x0(0) + 0 = 0
    }
  }
}
