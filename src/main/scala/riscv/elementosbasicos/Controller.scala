package riscv.elementosbasicos

import chisel3._
import chisel3.util._

class Controller extends Module {
  val io = IO(new Bundle {
    val opcode = Input(UInt(7.W))
    val funct3 = Input(UInt(3.W))
    val funct7_5 = Input(Bool()) // Bit 30 da instrucao
    
    val signals = Output(new Bundle {
      val regWrite = Output(Bool())
      val aluOp = Output(UInt(4.W))
      val aluSrc = Output(Bool())
      val memRead = Output(Bool())
      val memWrite = Output(Bool())
      val branch = Output(Bool())
      val memToReg = Output(Bool())
      val jump = Output(Bool())
      // Sinais auxiliares para JALR e U-types
      val auipc = Output(Bool())
      val jalr = Output(Bool())
      val lui = Output(Bool())
    })
  })

  // Valores padroes
  io.signals.regWrite := false.B
  io.signals.aluOp := ALUOp.ADD
  io.signals.aluSrc := false.B
  io.signals.memRead := false.B
  io.signals.memWrite := false.B
  io.signals.branch := false.B
  io.signals.memToReg := false.B
  io.signals.jump := false.B
  io.signals.auipc := false.B
  io.signals.jalr := false.B
  io.signals.lui := false.B

  switch(io.opcode) {
    // Tipo R
    is("b0110011".U) {
      io.signals.regWrite := true.B
      switch(io.funct3) {
        is("b000".U) { io.signals.aluOp := Mux(io.funct7_5, ALUOp.SUB, ALUOp.ADD) }
        is("b001".U) { io.signals.aluOp := ALUOp.SLL }
        is("b010".U) { io.signals.aluOp := ALUOp.SLT }
        is("b011".U) { io.signals.aluOp := ALUOp.SLTU }
        is("b100".U) { io.signals.aluOp := ALUOp.XOR }
        is("b101".U) { io.signals.aluOp := Mux(io.funct7_5, ALUOp.SRA, ALUOp.SRL) }
        is("b110".U) { io.signals.aluOp := ALUOp.OR }
        is("b111".U) { io.signals.aluOp := ALUOp.AND }
      }
    }

    // Tipo I
    is("b0010011".U) {
      io.signals.regWrite := true.B
      io.signals.aluSrc := true.B
      switch(io.funct3) {
        is("b000".U) { io.signals.aluOp := ALUOp.ADD }
        is("b001".U) { io.signals.aluOp := ALUOp.SLL }
        is("b010".U) { io.signals.aluOp := ALUOp.SLT }
        is("b011".U) { io.signals.aluOp := ALUOp.SLTU }
        is("b100".U) { io.signals.aluOp := ALUOp.XOR }
        is("b101".U) { io.signals.aluOp := Mux(io.funct7_5, ALUOp.SRA, ALUOp.SRL) }
        is("b110".U) { io.signals.aluOp := ALUOp.OR }
        is("b111".U) { io.signals.aluOp := ALUOp.AND }
      }
    }

    // Tipo I - Loads
    is("b0000011".U) {
      io.signals.regWrite := true.B
      io.signals.aluSrc := true.B
      io.signals.memToReg := true.B
      io.signals.memRead := true.B
      io.signals.aluOp := ALUOp.ADD
    }

    // Tipo S - Stores
    is("b0100011".U) {
      io.signals.aluSrc := true.B
      io.signals.memWrite := true.B
      io.signals.aluOp := ALUOp.ADD
    }

    // Tipo B - Branches
    is("b1100011".U) {
      io.signals.branch := true.B
      switch(io.funct3) {
        is("b000".U, "b001".U) { io.signals.aluOp := ALUOp.SUB }
        is("b100".U, "b101".U) { io.signals.aluOp := ALUOp.SLT }
        is("b110".U, "b111".U) { io.signals.aluOp := ALUOp.SLTU }
      }
    }

    // Tipo U - LUI
    is("b0110111".U) {
      io.signals.regWrite := true.B
      io.signals.lui := true.B
    }

    // Tipo U - AUIPC
    is("b0010111".U) {
      io.signals.regWrite := true.B
      io.signals.auipc := true.B
    }

    // Tipo J - JAL
    is("b1101111".U) {
      io.signals.regWrite := true.B
      io.signals.jump := true.B
    }

    // Tipo I - JALR
    is("b1100111".U) {
      io.signals.regWrite := true.B
      io.signals.jump := true.B
      io.signals.jalr := true.B
      io.signals.aluSrc := true.B
      io.signals.aluOp := ALUOp.ADD
    }
  }
}
