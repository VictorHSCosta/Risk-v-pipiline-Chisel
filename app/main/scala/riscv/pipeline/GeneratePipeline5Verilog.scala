package riscv.pipeline

import chisel3.stage.ChiselStage
import java.nio.file.Paths

object GeneratePipeline5Verilog extends App {
  val programFile = args.headOption.getOrElse("programs/rv32i_all.hex")
  val outputDir = args.drop(1).headOption.getOrElse("output/verilog")
  (new ChiselStage).emitVerilog(
    new Pipeline5(programFile =
      Paths.get(programFile).toAbsolutePath.normalize.toString
    ),
    Array("--target-dir", outputDir)
  )
}
