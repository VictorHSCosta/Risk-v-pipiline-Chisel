name := "aprendendo-tcc"
version := "0.1.0"
scalaVersion := "2.13.14"

scalacOptions ++= Seq(
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:reflectiveCalls",
)

// Configuracao do Scaladoc (gera documentacao HTML via sbt)
Compile / doc / scalacOptions ++= Seq(
  "-doc-title",
  "Documentacao do Projeto RISC-V em Chisel",
  "-doc-version",
  version.value,
)

// Atalho: `sbt docs` para gerar o HTML da documentacao
addCommandAlias("docs", "Compile / doc")

val chiselVersion = "3.6.1"

addCompilerPlugin("edu.berkeley.cs" % "chisel3-plugin" % chiselVersion cross CrossVersion.full)

libraryDependencies += "edu.berkeley.cs" %% "chisel3" % chiselVersion
libraryDependencies += "edu.berkeley.cs" %% "chiseltest" % "0.6.1"