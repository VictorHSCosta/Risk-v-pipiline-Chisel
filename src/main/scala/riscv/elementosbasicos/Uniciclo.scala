package riscv.elementosbasicos

import chisel3._
import chisel3.util._

class Uniciclo(initialProgram: Seq[Long] = Seq.empty) extends Module {
  val io = IO(new Bundle {
    // Portas para interagir e visualizar o estado interno durante testes
    val pcOut = Output(UInt(32.W))
    val ulaResultOut = Output(UInt(32.W))
  })

  // ==========================================
  // Instanciando os Módulos (Elementos Básicos)
  // ==========================================
  val pcReg = RegInit(0.U(32.W))
  val instrMem = Module(new InstructionMemory(depthWords = 1024, initialData = initialProgram))
  val dataMem = Module(new DataMemory(depthWords = 1024))
  val regFile = Module(new RegisterFile())
  val immGen = Module(new ImmGen())
  val ula = Module(new ULA())
  val adderPC4 = Module(new Somador32())
  val adderTarget = Module(new Somador32())
  val controller = Module(new Controller())

  // ==========================================
  // 1. Busca de Instrução (Instruction Fetch)
  // ==========================================
  instrMem.io.address := pcReg
  instrMem.io.writeEnable := false.B
  instrMem.io.writeData := 0.U
  val instruction = instrMem.io.readData

  // Somador PC + 4
  adderPC4.io.a := pcReg
  adderPC4.io.b := 4.U

  // ==========================================
  // 2. Decodificação da Instrução (Decode)
  // ==========================================
  val opcode = instruction(6, 0)
  val rd = instruction(11, 7)
  val funct3 = instruction(14, 12)
  val rs1 = instruction(19, 15)
  val rs2 = instruction(24, 20)
  val funct7 = instruction(31, 25)

  // Ligações para o Controller
  controller.io.opcode := opcode
  controller.io.funct3 := funct3
  controller.io.funct7_5 := funct7(5)

  val signals = controller.io.signals

  // Ligações para o Gerador de Imediatos
  immGen.io.instr := instruction
  val immediate = immGen.io.imm

  // Ligações para o Banco de Registradores (Leitura)
  regFile.io.rs1 := rs1
  regFile.io.rs2 := rs2

  // ==========================================
  // 3. Execução (Execute) e ULA
  // ==========================================
  // Preparando operandos da ULA
  // Tratamento especial LUI: Operand A é 0, Operand B é imediato
  val opA = Mux(signals.lui, 0.U, regFile.io.readData1)
  
  // Tratamento para AUIPC: PC + Immediato (pode ser feito no adderTarget)
  // Mas para AUIPC o dado que vai pro RD é PC + Imm. O Somador Target faz isso!
  
  val opB = Mux(signals.aluSrc, immediate, regFile.io.readData2)

  ula.io.a := opA
  ula.io.b := opB
  ula.io.op := signals.aluOp

  // Avaliação de Branches
  // O branch occurs se a instrução é branch E a condição é atendida.
  // BEQ e BNE verificam igualdade (SUB gera zero)
  // BLT, BGE usam SLT (gera 1 se menor)
  val zero = (ula.io.result === 0.U)
  val lessThan = (ula.io.result === 1.U)
  
  val branchCondition = WireDefault(false.B)
  switch(funct3) {
    is("b000".U) { branchCondition := zero } // BEQ
    is("b001".U) { branchCondition := !zero } // BNE
    is("b100".U, "b110".U) { branchCondition := lessThan } // BLT, BLTU
    is("b101".U, "b111".U) { branchCondition := !lessThan } // BGE, BGEU
  }

  val branchTaken = signals.branch && branchCondition

  // Somador de Endereço Alvo (Target Address: Branches e Jumps)
  // Para JALR o endereço base é o registrador rs1, para os outros é o PC atual
  adderTarget.io.a := Mux(signals.jalr, regFile.io.readData1, pcReg)
  adderTarget.io.b := immediate
  
  // O endereco alvo deve zerar o LSB (bit 0) no JALR, simulamos com AND
  val targetAddress = WireDefault(adderTarget.io.soma)
  when(signals.jalr) {
    targetAddress := adderTarget.io.soma & (~1.U(32.W))
  }

  // Próximo PC
  val nextPC = Mux(branchTaken || signals.jump, targetAddress, adderPC4.io.soma)
  pcReg := nextPC

  // ==========================================
  // 4. Memória de Dados (Memory)
  // ==========================================
  dataMem.io.address := ula.io.result
  dataMem.io.writeData := regFile.io.readData2
  dataMem.io.writeEnable := signals.memWrite

  // ==========================================
  // 5. Write Back (Escrita no Registrador)
  // ==========================================
  regFile.io.rd := rd
  regFile.io.regWrite := signals.regWrite

  // Decidindo o que escrever no rd
  val writeDataMemOrULA = Mux(signals.memToReg, dataMem.io.readData, ula.io.result)
  
  // Se for LUI, usa resultado da ULA. Se for JAL/JALR, guarda PC+4. Se for AUIPC, Target Address.
  regFile.io.writeData := Mux(signals.jump, adderPC4.io.soma,
                          Mux(signals.auipc, targetAddress, writeDataMemOrULA))

  // ==========================================
  // Saídas para Visualização / Teste
  // ==========================================
  io.pcOut := pcReg
  io.ulaResultOut := ula.io.result
}
