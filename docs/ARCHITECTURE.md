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

## Pipeline5 RV32I

```text
IF -> IF/ID -> ID -> ID/EX -> EX -> EX/MEM -> MEM -> MEM/WB -> WB
                    ^             |                         |
                    |             +-- forwarding -----------+
                    +-- stall (load-use)                     
```

Branches e saltos sao resolvidos em EX e limpam IF/ID e ID/EX. Um load seguido
por consumidor congela PC/IF/ID por um ciclo e insere uma bolha. `FENCE` e
inofensivo na RAM interna in-order. `ECALL` (11), `EBREAK` (3), ilegais (2) e
acessos/instrucoes desalinhados (0/4/6) drenam as instrucoes anteriores e
congelam o nucleo com `trapValid`, `trapCause`, `trapPc` e `trapInstruction`.

| Instrucoes RV32I | Implementacao | Verificacao |
| --- | --- | --- |
| LUI, AUIPC, JAL, JALR | `Controller`, `Pipeline5` | `Pipeline5HazardSpec` |
| BEQ, BNE, BLT, BGE, BLTU, BGEU | `Controller`, `Pipeline5` | `Pipeline5HazardSpec` |
| LB, LH, LW, LBU, LHU | `Controller`, `DataMemory` | `Pipeline5Spec` |
| SB, SH, SW | `Controller`, `DataMemory` | `Pipeline5Spec` |
| ADDI, SLTI, SLTIU, XORI, ORI, ANDI, SLLI, SRLI, SRAI | `Controller`, `ULA` | `Pipeline5HazardSpec` |
| ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND | `Controller`, `ULA` | `Pipeline5HazardSpec` |
| FENCE, ECALL, EBREAK | `Controller`, `Pipeline5` | `Pipeline5Spec` |

## Fluxo

1. `building` chama o gerador Chisel, compila o testbench e grava Verilog/VCD.
2. `run` apenas abre o VCD existente no GTKWave.
3. `linter` roda o Scalafmt em modo de verificacao.
4. `test` roda os testes Chisel/Scala.

`setup` instala as ferramentas de sistema do Ubuntu. Ele fica separado do
`pnpm install` para nao executar `sudo apt` automaticamente.

Somente fontes, documentacao, configuracao e programas devem ser enviados ao
GitHub. Verilog, FIRRTL, VCD e executaveis ficam em `output/`.
