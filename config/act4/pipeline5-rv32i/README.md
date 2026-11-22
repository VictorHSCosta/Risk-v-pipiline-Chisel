# ACT4 no Pipeline5

O perfil foi adaptado do Core-V Wally RV32I na revisão
[12f451d do ACT4](https://github.com/riscv/riscv-arch-test/tree/12f451d032197aeb747642f83c8d1f8b17064ad6/config/cores/cvw/cvw-rv32i).
O arquivo UDB segue o modelo de configuração sem CSRs desse exemplo. O runner
seleciona a extensão I e desabilita testes privilegiados. FENCE é aceito como
no-op. ECALL, EBREAK e C.EBREAK são ilegais no Pipeline5. O subconjunto
comprimido continua coberto pelos testes Chisel e pelo programa diferencial
do Spike. O perfil não declara C, M ou A.

Com as ferramentas do [README oficial](https://github.com/riscv/riscv-arch-test/blob/12f451d032197aeb747642f83c8d1f8b17064ad6/README.md)
no PATH, execute na raiz do projeto:

```bash
rtk bash scripts/pipeline5-act.sh
```

O script usa o checkout em ~/src/riscv-arch-test por padrão. Defina ACT4_DIR
para indicar outro local. O ACT4 gera os ELFs autoverificáveis com resultados
do Sail. O testbench carrega os segmentos ELF na RAM simulada, inicia no
endereço de entrada e observa escritas em tohost. O valor 1 indica sucesso.
Os ELFs e os logs são preservados em target/pipeline5-act4.*.
Um resultado de falha, uma instrução ilegal ou um timeout falha o teste.
