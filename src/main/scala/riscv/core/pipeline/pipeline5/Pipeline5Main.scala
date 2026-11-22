package riscv.core.pipeline.pipeline5

import chisel3.stage.ChiselStage

/** Entrypoint for elaborating the five-stage processor. */
object Pipeline5Main extends App {
  (new ChiselStage).emitVerilog(new Pipeline5(), args)
}
