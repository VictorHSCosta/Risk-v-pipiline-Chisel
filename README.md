# Projeto RISC-V em Chisel

CPU RISC-V RV32I educacional em Chisel/Scala.

## Comandos

```bash
npm run setup      # instala as ferramentas Ubuntu (usa sudo)
pnpm run building  # gera Verilog em output/verilog/
pnpm run run       # simula e abre o GTKWave
pnpm run linter    # verifica formatacao
pnpm run test      # roda os testes Chisel
```

`npm run setup` e `pnpm run setup` sao equivalentes. Depois de executar o
setup uma vez, o fluxo normal começa em `npm run building`.

Requisitos: Java 21, sbt, pnpm, Icarus Verilog e GTKWave.

## Arquitetura

- `app/main/scala/`: hardware Chisel e geradores.
- `app/test/scala/`: testes.
- `app/sim/`: testbench Verilog versionado.
- `config/`: configuracoes auxiliares, como Scalafmt.
- `docs/`: arquitetura e comandos detalhados.
- `programs/`: programas `.hex` usados pela memoria de instrucoes.
- `output/`: Verilog, VCD e binarios gerados; ignorado pelo Git.
- `bin/dev`: implementacao dos comandos do `pnpm`.
- `build.sbt` e `project/`: configuracao obrigatoria do sbt.

Para trocar o programa da simulacao:

```bash
PROGRAM=programs/outro.hex pnpm run run
```

Mais detalhes em [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).
