# Projeto RISC-V em Chisel

Implementacao inicial de blocos de CPU RISC-V usando Chisel (Scala), com foco atual na ULA e testes automatizados.

## Requisitos

- Linux (testado em Ubuntu)
- Java 21
- sbt 1.12.8 ou compativel

Comandos uteis para conferir versoes:

```bash
java -version
sbt --version
```

## Estrutura do projeto

- `src/main/scala/riscv/`: modulos de hardware (ex.: `ULA.scala`)
- `src/test/scala/riscv/`: testes com ScalaTest + chiseltest
- `build.sbt`: dependencias e configuracoes do build

## Como rodar o projeto

Entre na pasta do projeto:

```bash
cd "/home/victor-costa/Documentos/aprendendo tcc"
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

1. Editar o modulo em `src/main/scala/riscv/`.
2. Rodar `sbt test`.
3. Se alterou comentarios Scaladoc, rodar `sbt docs`.

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
