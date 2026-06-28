# Projeto RISC-V em Chisel

Implementacao inicial de blocos modulares para uma CPU RISC-V RV32I usando Chisel (Scala).
O projeto foi limpo para preparar um pipeline futuro: nao ha mais top-level uniciclo, apenas blocos reutilizaveis e testes unitarios.

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

- `src/main/scala/riscv/elementosbasicos/RV32I.scala`: constantes de opcode e seletores de controle RV32I.
- `src/main/scala/riscv/elementosbasicos/Controller.scala`: decodificador/controlador combinacional para RV32I base.
- `src/main/scala/riscv/elementosbasicos/ULA.scala`: ULA de 32 bits para operacoes aritmeticas/logicas RV32I.
- `src/main/scala/riscv/elementosbasicos/ImmGen.scala`: gerador de imediatos I/S/B/U/J.
- `src/main/scala/riscv/elementosbasicos/RegisterFile.scala`: banco de 32 registradores x 32 bits.
- `src/main/scala/riscv/elementosbasicos/Memorias.scala`: memoria de instrucao por palavra e memoria de dados por byte.
- `src/main/scala/riscv/pipeline/Pipeline3.scala`: top-level inicial de pipeline RV32I em 3 estagios.
- `src/test/scala/riscv/`: testes com ScalaTest + chiseltest
- `build.sbt`: dependencias e configuracoes do build

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

## Documentacao HTML (Scaladoc)

Este projeto ja esta configurado para gerar documentacao HTML a partir de comentarios Scaladoc (`/** ... */`).

Gerar documentacao:

```bash
sbt docs
```

Comando equivalente:

```bash
sbt "Compile / doc"
```

Arquivo principal gerado:

`target/scala-2.13/api/index.html`

Abrir no navegador no Linux:

```bash
xdg-open "target/scala-2.13/api/index.html"
```

## Fluxo recomendado no dia a dia

1. Editar o modulo em `src/main/scala/riscv/elementosbasicos/`.
2. Rodar `sbt test`.
3. Se alterou comentarios Scaladoc, rodar `sbt docs`.

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
