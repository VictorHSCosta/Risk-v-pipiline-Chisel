# Programa de integração RV32C

`integer-program.hex` contém palavras de 32 bits da memória de instruções,
com PC inicial zero. Cada linha tem oito dígitos hexadecimais. Uma instrução
comprimida ocupa dois bytes e pode dividir uma palavra com outra instrução.

| Grupo | Instruções exercitadas |
| --- | --- |
| Imediatos e pilha | C.LI, C.LUI, C.ADDI, C.ADDI4SPN, C.ADDI16SP |
| Memória | C.LW, C.SW, C.LWSP, C.SWSP |
| Aritmética e lógica | C.SLLI, C.SRLI, C.SRAI, C.ANDI, C.SUB, C.XOR, C.OR, C.AND, C.MV, C.ADD |
| Fluxo | C.BEQZ, C.BNEZ, C.J, C.JAL, C.JR, C.JALR |
| Sem efeito arquitetural | C.NOP e um HINT |

O programa usa x31 = 1 como marcador de término. Instruções nos caminhos que
devem ser descartados escrevem x30. O teste verifica os resultados em
registradores específicos por meio da interface pública de writeback. Os
endereços usados na memória de dados são inicializados pelo próprio programa.

C.EBREAK, codificações reservadas e outras variantes de operandos fazem parte
do escopo do teste unitário de descompressão, que percorre as 65.536 entradas.
A fixture de integração usa casos dirigidos e pode ser carregada por outro
núcleo com o mesmo contrato de inicialização e observação.
