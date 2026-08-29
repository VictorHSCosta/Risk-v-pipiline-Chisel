# Arquitetura do projeto

```text
app/
  main/scala/   hardware Chisel e geradores
  test/scala/   testes automatizados
  sim/          testbench Verilog
config/         configuracoes auxiliares
docs/           documentacao
programs/       programas `.hex` versionados
output/         artefatos gerados, ignorados pelo Git
bin/dev         comandos amigaveis do projeto
```

`build.sbt` e `project/` permanecem na raiz porque sao o layout padrao
obrigatorio do sbt. O `build.sbt` aponta as fontes Scala para `app/`.

## Fluxo

1. `building` chama o gerador Chisel e grava Verilog em `output/verilog/`.
2. `run` compila o Verilog com Icarus, executa o testbench e abre o GTKWave.
3. `linter` roda o Scalafmt em modo de verificacao.
4. `test` roda os testes Chisel/Scala.

Somente fontes, documentacao, configuracao e programas devem ser enviados ao
GitHub. Verilog, FIRRTL, VCD e executaveis ficam em `output/`.
