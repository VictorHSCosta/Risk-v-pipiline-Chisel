# Pipeline 5

O módulo `Pipeline5` executa instruções inteiras RV32I e as instruções
comprimidas que expandem para operações suportadas. As etapas são IF, ID, EX,
MEM e WB. O núcleo sinaliza instruções SYSTEM (incluindo ECALL, EBREAK e
C.EBREAK), atômicas e outras codificações fora do escopo em `io.illegal`.

O forwarding prioriza EX/MEM sobre MEM/WB. Uma instrução dependente de load
espera um ciclo. Desvios tomados e saltos descartam as instruções mais novas.

IF, ID, EX e MEM mantêm os quatro registradores entre estágios. WB consome
MEM/WB no mesmo ciclo e fornece os sinais de escrita ao banco de registradores
em ID. O topo liga os módulos e encaminha stall e redirect.

`commitValid` indica uma instrução concluída em WB, mesmo quando ela não escreve
um registrador. `commitPc` e `commitInstr` mostram o PC da instrução e sua
codificação de 32 bits após a expansão. Os sinais `writebackEnable`,
`writebackRd` e `writebackData` descrevem a escrita no banco.

- Teste de caixa preta com programa misto: `sbt "testOnly riscv.integration.pipeline5.CompressedProgramSpec"`
- Gerar Verilog: `sbt "runMain riscv.core.pipeline.pipeline5.Pipeline5Main --target-dir output/verilog"`

## CI e proteção da main

- `python3 scripts/pipeline5-spike.py` requer Spike com `--instructions` e
  `--log-commits` no PATH. Compara a sequência de PCs e escritas de registradores
  de um programa misto curto, até a escrita de 1 em x31.
- `rtk bash scripts/pipeline5-act.sh` gera e executa os ELFs selecionados pelo
  perfil I. Veja a [configuração ACT4](../../../../../../../config/act4/pipeline5-rv32i/README.md).
- O teste de caixa preta escreve cobertura funcional observada em
  `target/pipeline5-functional-coverage.tsv`. O CI publica o TSV e o HTML
  navegável da hierarquia de módulos gerada do Verilog em `pipeline5-reports`.

O workflow CI verifica formatação, executa a suíte inteira e elabora o Verilog
do Pipeline5. Depois que o check `CI Required` aparecer no GitHub, configure um
ruleset ativo para `main` que exija pull request e esse check de GitHub Actions
antes do merge. Use a opção de exigir que a branch esteja atualizada e deixe a
lista de bypass vazia para aplicar a regra também a administradores.
