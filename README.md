# Projeto RISC-V em Chisel

Implementacao de uma CPU RISC-V RV32I em Chisel (Scala), organizada em blocos reutilizaveis, pipeline e testes.

## Requisitos

- Linux (testado em Ubuntu)
- Java 21
- sbt 1.12.8 ou compativel

Comandos uteis para conferir versoes:

```bash
java -version
sbt --version
```

## Estrutura atual

- `src/main/scala/`: codigo Chisel da CPU. `riscv/elementosbasicos/` tem os blocos reutilizaveis; `riscv/pipeline/` tem o pipeline.
- `src/test/scala/riscv/unit/`: testes de um bloco Chisel por vez (`execute`, `decode`, `memory`, `registers`, `fetch`, `compressed` e `privileged`).
- `src/test/scala/riscv/integration/rv32i/`: testes de aceitação RV32I agrupados por comportamento: aritmética, fluxo de controle e memória.
- `src/test/scala/riscv/integration/blackbox/`: exemplo explícito de teste caixa-preta do processador usando só resultados públicos.
- `src/test/scala/riscv/integration/rv32c/` e `privileged/`: testes de extensão comprimida e de traps/CSRs, fora do escopo da suíte RV32I base.
- `src/test/scala/riscv/integration/support/`: montadores de instruções e helper compartilhado pelos testes; não é uma suíte de testes.
- `src/test/scala/riscv/diagnostics/`: trace opcional por ciclo, para depuração; não é a suíte principal de comportamento arquitetural.
- `src/test/resources/diagnostics/rv32i-add.hex`: programa HEX pequeno usado como exemplo no teste de trace. `resources/` guarda dados de teste, não código Scala; o arquivo só é lido quando informado em `RV32I_HEX`.
- Ainda não há `CSRSpec` isolado: os CSRs são exercitados junto com o processador em `integration/privileged/TrapHandlingSpec.scala`.
- `project/`: configuracao do sbt, como a versao do sbt e plugins. Nao e codigo da CPU.
- `build.sbt`: configuracao principal do projeto Scala/Chisel e suas dependencias.
- `.github/workflows/`: automacao de lint e testes no GitHub Actions.
- `target/`, `project/target/` e `test_run_dir/`: arquivos temporarios criados pelo sbt e pelos testes. Nao edite nem versione esses arquivos.

Fluxo principal: `src/main` define o hardware, `src/test` verifica seu comportamento, e `build.sbt` e `project/` dizem ao sbt como compilar e testar.

## Como rodar o projeto

Entre na pasta do projeto:

```bash
cd "/home/victor-costa/Documentos/Risk-v-pipiline-Chisel"
```

Compile o codigo:

```bash
sbt compile
```

Rode todos os testes:

```bash
sbt test
```

Rode apenas o teste da ULA:

```bash
sbt "testOnly riscv.unit.execute.ULATest"
```

## CI e formatacao

O projeto usa GitHub Actions para validar o codigo a cada `push` e
`pull_request`. O workflow fica em:

```text
.github/workflows/ci.yml
```

O CI possui dois jobs separados, para ficar claro onde uma falha aconteceu:

- `Lint / Scalafmt`: verifica a formatacao.
- `Tests / ChiselTest`: roda os testes do projeto.

Eles executam:

```bash
sbt scalafmtCheckAll
sbt test
```

Para rodar a mesma verificacao localmente:

```bash
sbt scalafmtCheckAll test
```

Para formatar automaticamente os arquivos Scala/Chisel:

```bash
sbt scalafmtAll
```

Sobre linter: para Chisel, o mais comum neste projeto e tratar o codigo como
Scala e usar `scalafmt` para padronizar estilo. Erros estruturais do circuito
continuam sendo pegos por `sbt compile` e pelos testes com `chiseltest`.

## Testes do processador

Os testes de bloco ficam em `unit/` e testam módulos isolados. Os testes em
`integration/rv32i/` executam programas pelo processador: `arithmetic/` testa
operações com registradores e imediatos, `control/` testa branches e jumps, e
`memory/` testa loads, stores e dependências. `integration/rv32c/` testa
programas com instruções comprimidas. `integration/privileged/` testa traps e
CSRs. O `Rv32iBench` em `integration/support/` fornece as codificações e o
helper comum.

Os testes em `integration/rv32i/` são caixa-preta: carregam um pequeno programa
como fixture e comparam as gravações observáveis de registradores, sem consultar
sinais dos estágios nem exigir um número exato de ciclos. A especificação fica
agrupada por comportamento:

- `arithmetic/Rv32iArithmeticSpec`: ADD/SUB, shifts, comparações, lógica, imediatos, LUI e AUIPC.
- `control/Rv32iControlSpec`: os seis branches tomados e não tomados, JAL, JALR e FENCE.
- `memory/Rv32iMemorySpec`: LB/LH/LW/LBU/LHU, SB/SH/SW e dependência entre load e uso.
- `blackbox/rv32i/Rv32iPipelineBlackBoxSpec`: exemplo curto que torna visível a fronteira pública usada pela suíte.

| Grupo | Instruções com caso dirigido | O que o caso observa |
| --- | --- | --- |
| Registrador (`OP`) | ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND | Escrita de resultado para operandos negativos/positivos escolhidos |
| Imediato (`OP-IMM`) | ADDI, SLTI, SLTIU, XORI, ORI, ANDI, SLLI, SRLI, SRAI | Resultado escrito após extensão/shift |
| Controle | BEQ, BNE, BLT, BGE, BLTU, BGEU, JAL, JALR, FENCE | Caminho tomado/não tomado, destino e link de retorno |
| Memória | LB, LH, LW, LBU, LHU, SB, SH, SW | Valor lido de volta e extensão de sinal/zero |
| Imediato alto | LUI, AUIPC | Valor montado e PC somado |

Gaps conhecidos da matriz: não há varredura de todos os operandos, nem ainda
vetores sistemáticos para todos os limites de imediato e shifts, wraparound,
acessos desalinhados ou combinações extensas de hazards. Esses são os próximos
casos a acrescentar antes de alegar cobertura ampla; os 38 mnemônicos acima
significam presença de caso, não prova exaustiva da instrução.

O adaptador `integration/support/Rv32iBlackBox` concentra a ligação ao módulo
de topo atual. Ao substituir Pipeline3 por Pipeline5, atualize essa classe; os
casos de comportamento permanecem iguais. A memória de instruções é
precarregada pelo construtor para fins de simulação porque o módulo atual não
expõe uma porta externa de barramento de memória. Portanto, “caixa-preta” aqui
significa não depender da implementação dos estágios; a fixture de programa é
uma entrada de simulação, não um pino físico atual.

Essa suíte cobre as 38 instruções RV32I não privilegiadas com casos dirigidos;
ECALL e EBREAK continuam na suíte separada de traps/CSRs. Ela não testa todas
as combinações possíveis de operandos. Ainda vale ampliar vetores de borda para
limites de imediato, shifts, overflow modular e endereços desalinhados conforme
a política arquitetural escolhida. Os testes
oficiais [RISC-V Architectural Certification Tests](https://github.com/riscv/riscv-arch-test)
e [riscv-tests](https://github.com/riscv-software-src/riscv-tests) são boas
referências adicionais; eles não provam sozinhos a ausência de defeitos. Uma
interface de aposentadoria inspirada em [RVFI](https://github.com/YosysHQ/riscv-formal/blob/main/docs/source/rvfi.rst)
seria o próximo passo se for necessário comparar instrução por instrução sem
amarrar a verificação a sinais de write-back específicos.

```bash
sbt 'testOnly riscv.integration.rv32i.arithmetic.Rv32iArithmeticSpec riscv.integration.rv32i.control.Rv32iControlSpec riscv.integration.rv32i.memory.Rv32iMemorySpec riscv.integration.privileged.TrapHandlingSpec'
```

Para listar resultados públicos por ciclo limite durante o diagnóstico:

```bash
RV32I_HEX=src/test/resources/diagnostics/rv32i-add.hex RV32I_CYCLES=5 \
  sbt 'testOnly riscv.diagnostics.Rv32iHexTraceSpec'
```

O diagnóstico lê o HEX indicado, uma instrução de 8 dígitos por linha, e lista
as gravações públicas em registradores. O formato segue a
[especificação RV32I](https://docs.riscv.org/reference/isa/v20260120/unpriv/rv32.html).

## Fluxo recomendado no dia a dia

1. Editar o modulo em `src/main/scala/riscv/elementosbasicos/`.
2. Rodar `sbt scalafmtAll`.
3. Rodar `sbt scalafmtCheckAll test`.

## Escopo RV32I atual

- A ULA cobre as operacoes exigidas por `OP` e `OP-IMM`: `ADD`, `SUB`, `SLL`, `SLT`, `SLTU`, `XOR`, `SRL`, `SRA`, `OR`, `AND`.
- O controlador reconhece os opcodes RV32I usados pelo pipeline inicial: `LUI`, `AUIPC`, `JAL`, `JALR`, branches, loads, stores, `OP-IMM` e `OP`.
- Loads/stores ja saem do controlador com tamanho (`byte`, `half`, `word`) e sinalizacao de extensao sem sinal para `LBU`/`LHU`.
- `DataMemory` suporta `LB`, `LH`, `LW`, `LBU`, `LHU`, `SB`, `SH` e `SW` em memoria byte-addressable. O proximo passo para um core mais completo e decidir como o pipeline vai tratar acessos desalinhados e traps.

## Pipeline inicial

O modulo `Pipeline3` segue a organizacao didatica do capitulo 15:

1. IF: busca de instrucao e atualizacao do PC.
2. ID: decode, leitura do banco de registradores e geracao de imediato.
3. EX/MEM/WB: ULA, branch/jump, acesso a memoria de dados e write-back.

Ele ja possui forwarding simples do resultado do estagio EX para o decode e flush em branch/jump. Ainda nao implementa traps reais, CSRs, `FENCE`, `ECALL`, `EBREAK` nem politica final para acessos desalinhados.

## Erros comuns e como resolver

`java: command not found`:
- Instale o Java 21 e confirme com `java -version`.

`sbt: command not found`:
- Instale o sbt e confirme com `sbt --version`.

Falha de compilacao em teste:
- Rode primeiro `sbt compile` e depois `sbt test` para ver o erro completo de forma separada.

## Dependencias principais

- Scala `2.13.14`
- Chisel `3.6.1`
- chiseltest `0.6.1`
- Scalafmt `3.10.7`
