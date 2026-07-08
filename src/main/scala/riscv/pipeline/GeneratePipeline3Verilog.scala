package riscv.pipeline

import chisel3.stage.ChiselStage
import java.nio.file.Paths

object GeneratePipeline3Verilog extends App {
  val programFile = args.headOption.getOrElse("modelsim/program.hex")
  val outputDir = args.drop(1).headOption.getOrElse("generated")
  val absoluteProgramFile =
    Paths.get(programFile).toAbsolutePath.normalize.toString

  (new ChiselStage).emitVerilog(
    new Pipeline3(programFile = absoluteProgramFile),
    Array("--target-dir", outputDir)
  )
}
