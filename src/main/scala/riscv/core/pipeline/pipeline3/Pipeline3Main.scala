package riscv.core.pipeline.pipeline3

import chisel3.stage.ChiselStage

/** Entrypoint for elaborating the current three-stage processor. */
object Pipeline3Main extends App {
  (new ChiselStage).emitVerilog(new Pipeline3(), args)
}
