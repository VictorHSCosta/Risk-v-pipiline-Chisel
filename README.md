# Risk-v-pipiline-Chisel

## Documentacao HTML (Scaladoc)

Sim, neste projeto voce pode escrever comentarios de documentacao no estilo Scaladoc e gerar HTML, similar ao fluxo de Ruby com RDoc.

### Como comentar

Use comentarios `/** ... */` acima de classes, objetos e metodos:

```scala
/** Unidade Logica e Aritmetica de 32 bits. */
class ULA extends Module {
	/** Resultado da operacao selecionada por `op`. */
	val io = IO(new Bundle {
		val result = Output(UInt(32.W))
	})
}
```

### Como gerar a documentacao

```bash
sbt docs
```

Ou diretamente:

```bash
sbt "Compile / doc"
```

### Onde fica o HTML gerado

`target/scala-2.13/api/index.html`
