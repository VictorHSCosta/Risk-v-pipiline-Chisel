package riscv.pipeline

import chisel3._
import chisel3.util._
import riscv.elementosbasicos.RvcDecompressor

/** Busca RV32 com RVC e um buffer de palavra, antes do decode.
  * A memória responde combinacionalmente ao endereço quando readEnable está ativo.
  * Uma instrução de 32 bits na metade superior usa a palavra guardada e a seguinte.
  * ponytail: memória combinacional; adicionar handshake ao conectar cache com latência.
  */
class InstructionFetch extends Module {
  val io = IO(new Bundle {
    // Endereco em bytes da instrucao que o processador quer executar.
    val pc = Input(UInt(32.W))
    // enable pausa a busca; flush descarta o dado guardado apos um desvio.
    val enable = Input(Bool())
    val flush = Input(Bool())
    // Palavra de 32 bits devolvida pela memoria para memoryAddress.
    val memoryData = Input(UInt(32.W))
    // Endereco em bytes pedido a memoria; memoryAddress nao e a instrucao.
    val memoryAddress = Output(UInt(32.W))
    val memoryReadEnable = Output(Bool())
    // Instrucao pronta para o decode, sempre representada em 32 bits.
    // Uma instrucao RVC de 16 bits e expandida antes de sair daqui.
    val instr = Output(UInt(32.W))
    // Tamanho original da instrucao: 2 ou 4 bytes, usado para avancar o PC.
    val length = Output(UInt(3.W))
    val illegal = Output(Bool())
    // So consuma instr quando valid estiver true.
    val valid = Output(Bool())
  })

  // Guarda a ultima palavra lida, para reutilizar a metade superior ou
  // montar uma instrucao de 32 bits que atravessa duas palavras.
  val word = RegInit(0.U(32.W))
  val wordAddress = RegInit(0.U(32.W))
  val wordValid = RegInit(false.B)

  // Memoria entrega palavras de 4 bytes. Zerar os 2 bits inferiores encontra
  // o inicio da palavra: PC 0 e PC 2 ficam na mesma palavra, em metades distintas.
  val alignedPc = Cat(io.pc(31, 2), 0.U(2.W))
  val hit = wordValid && wordAddress === alignedPc

  // A metade superior da palavra guardada comeca uma instrucao de 32 bits
  // quando seus bits [1:0] sao 11. Nesse caso, os outros 16 bits estao
  // na proxima palavra; [4:2] == 111 indica um tamanho maior nao suportado.
  val bufferedCross = hit && io.pc(1) && word(17, 16) === 3.U &&
    word(20, 18) =/= 7.U
  val active = io.enable && !io.flush
  // Le uma palavra se ela ainda nao esta no buffer, ou busca a proxima palavra
  // quando a instrucao comeca na metade superior e cruza o limite.
  io.memoryReadEnable := active && (!hit || bufferedCross)
  // Enderecos sao em bytes; a palavra seguinte fica 4 bytes adiante.
  io.memoryAddress := Mux(bufferedCross, alignedPc + 4.U, alignedPc)

  // Reusa a palavra guardada quando o PC aponta para ela; senao, usa a leitura atual.
  val firstWord = Mux(hit, word, io.memoryData)
  // pc(1) escolhe qual metade de 16 bits contem o inicio da instrucao.
  val half = Mux(io.pc(1), firstWord(31, 16), firstWord(15, 0))
  // Em RISC-V com extensao C, finais diferentes de 11 significam 16 bits.
  val compressed = half(1, 0) =/= 3.U
  // Prefixo 111 em [4:2] anuncia uma instrucao maior que 32 bits, nao suportada.
  val unsupportedLength = !compressed && half(4, 2) === 7.U
  // Instrucao normal de 32 bits iniciada na metade superior cruza duas palavras.
  val cross = io.pc(1) && !compressed && !unsupportedLength
  val decompressor = Module(new RvcDecompressor)
  decompressor.io.inst_c := half

  // Ao detectar cruzamento antes de ter a palavra guardada, espera um ciclo
  // pela leitura; valid impede que o pipeline consuma a instrucao incompleta.
  io.valid := active && (!cross || hit)
  io.length := Mux(compressed, 2.U, 4.U)
  io.illegal := io.pc(0) || unsupportedLength ||
    (compressed && decompressor.io.illegal)
  // Para cruzamento, concatena os 16 bits da palavra seguinte com os 16 atuais.
  // Instrucoes ilegais ou ainda invalidas aparecem como NOP para evitar lixo no decode.
  val fullInstr = Mux(cross, Cat(io.memoryData(15, 0), half), firstWord)
  io.instr := Mux(io.valid && !io.illegal,
    Mux(compressed, decompressor.io.inst_out, fullInstr), "h00000013".U)

  when(io.flush) {
    // Um desvio mudou o PC: a palavra antiga nao deve ser reutilizada.
    wordValid := false.B
  }.elsewhen(io.memoryReadEnable) {
    // Registra a palavra recebida e o endereco ao qual ela pertence.
    word := io.memoryData
    wordAddress := io.memoryAddress
    wordValid := true.B
  }
}
