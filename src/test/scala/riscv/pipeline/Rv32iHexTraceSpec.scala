package riscv.pipeline

import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import scala.io.Source

/** RV32I_HEX=program.hex RV32I_CYCLES=20 sbt 'testOnly riscv.pipeline.Rv32iHexTraceSpec' */
class Rv32iHexTraceSpec extends AnyFlatSpec with ChiselScalatestTester {
  behavior of "Pipeline3: leitura por ciclo de um programa HEX"

  it should "mostrar IF, ID e EX até o número de ciclos pedido" in {
    val path = sys.env.getOrElse("RV32I_HEX", cancel("defina RV32I_HEX com o caminho do programa"))
    val source = Source.fromFile(path)
    val lines = try source.getLines().map(_.takeWhile(_ != '#').trim).filter(_.nonEmpty).toVector
      finally source.close()
    require(lines.nonEmpty, "o arquivo HEX está vazio")
    lines.zipWithIndex.foreach { case (line, index) =>
      require(line.matches("[0-9a-fA-F]{8}"), s"linha ${index + 1}: use 8 dígitos hexadecimais")
    }
    val program = lines.map(java.lang.Long.parseUnsignedLong(_, 16))
    val cycles = sys.env.get("RV32I_CYCLES").map(_.toInt).getOrElse(program.size + 3)
    require(cycles > 0, "RV32I_CYCLES deve ser positivo")

    test(new TracedPipeline3(program, words = math.max(64, program.size + cycles))) { dut =>
      println(s"Programa: $path (${program.size} instruções, $cycles ciclos)")
      for (cycle <- 0 until cycles) {
        Rv32iTrace.print(dut, cycle)
        dut.clock.step()
      }
    }
  }
}
