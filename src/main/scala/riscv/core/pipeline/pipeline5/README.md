# Pipeline 5

O módulo `Pipeline5` executa instruções inteiras RV32I e as instruções
comprimidas que expandem para operações suportadas. As etapas são IF, ID, EX,
MEM e WB. O núcleo sinaliza instruções SYSTEM (incluindo ECALL, EBREAK e
C.EBREAK), atômicas e outras codificações fora do escopo em `io.illegal`.

O forwarding prioriza EX/MEM sobre MEM/WB. Uma instrução dependente de load
espera um ciclo. Desvios tomados e saltos descartam as instruções mais novas.

- Teste de caixa preta com programa misto: `sbt "testOnly riscv.integration.pipeline5.CompressedProgramSpec"`
- Gerar Verilog: `sbt "runMain riscv.core.pipeline.pipeline5.Pipeline5Main --target-dir output/verilog"`
