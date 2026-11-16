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
- `src/test/scala/`: testes automatizados dos blocos e do pipeline.
- `src/test/resources/`: programas e dados usados pelos testes.
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
sbt "testOnly riscv.ULATest"
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

## Testes RV32I e leitura por ciclo

A suíte `Rv32iArithmeticSpec`, `Rv32iControlSpec` e `Rv32iMemorySpec` testa as
instruções inteiras RV32I com um nome por instrução. `Pipeline3TrapSpec` já testa
`ECALL` e `EBREAK`. As extensões M, A, C e CSR ficam fora desta suíte básica.

```bash
sbt 'testOnly riscv.pipeline.Rv32iArithmeticSpec riscv.pipeline.Rv32iControlSpec riscv.pipeline.Rv32iMemorySpec riscv.pipeline.Pipeline3TrapSpec'
```

Para imprimir IF, ID, EX, operandos, imediato, controle, memória e writeback em
cada ciclo dos testes automáticos:

```bash
RV32I_TRACE=1 sbt 'testOnly riscv.pipeline.Rv32iArithmeticSpec -- -z ADD'
```

Para carregar seu próprio HEX (uma instrução de 8 dígitos por linha, como no
exemplo `src/test/resources/rv32i-add.hex`) e escolher quantos ciclos ler:

```bash
RV32I_HEX=src/test/resources/rv32i-add.hex RV32I_CYCLES=5 \
  sbt 'testOnly riscv.pipeline.Rv32iHexTraceSpec'
```

O HEX é lido diretamente pelo teste. Cada ciclo
mostra o PC e instrução buscada em IF, os registradores e sinais em ID, e os
operandos, resultado e escrita em EX. O formato segue a
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
