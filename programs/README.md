# Programas

Os programas usados pela memoria de instrucoes ficam aqui em `.hex`: uma
instrucao RV32I de 32 bits por linha, sem `0x`.

O programa padrao é `rv32i_smoke.hex`. Para simular outro:

```bash
PROGRAM=programs/meu_programa.hex pnpm run run
```

`.obj` e `.bin` nao sao necessarios neste fluxo: o `InstructionMemory` ja
carrega hexadecimal diretamente.
