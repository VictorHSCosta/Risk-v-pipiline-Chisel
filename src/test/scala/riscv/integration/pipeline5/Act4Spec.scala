package riscv.integration.pipeline5

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import riscv.core.pipeline.pipeline5.Pipeline5

import java.nio.{ByteBuffer, ByteOrder}
import java.nio.file.{Files, Paths}

class Act4Spec extends AnyFlatSpec with ChiselScalatestTester {
  for (manifest <- sys.env.get("PIPELINE5_ACT4_MANIFEST")) {
    for (line <- Files.readString(Paths.get(manifest)).linesIterator) {
      val Array(tohostHex, filename) = line.split("\\t", 2)
      val tohost = java.lang.Long.parseUnsignedLong(tohostHex, 16)
      it should s"pass ${Paths.get(filename).getFileName}" in {
        val bytes = Files.readAllBytes(Paths.get(filename))
        val elf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(
          elf.getInt(0) == 0x464c457f && bytes(4) == 1 && bytes(5) == 1 &&
            elf.getShort(16) == 2 && elf.getShort(18) == 243,
          s"expected a little-endian RISC-V ELF32 executable: $filename"
        )
        def uint(offset: Int): Long = Integer.toUnsignedLong(elf.getInt(offset))
        val ram = new Array[Byte](0x800000)
        val base = 0x80000000L
        val entry = uint(24)
        require(entry >= base && entry < base + ram.length)
        require(
          tohost >= base && tohost + 8 <= base + ram.length && tohost % 8 == 0
        )
        for (index <- 0 until elf.getShort(44).toInt) {
          val header =
            Math.toIntExact(uint(28) + index.toLong * elf.getShort(42))
          if (elf.getInt(header) == 1 && uint(header + 20) != 0) {
            val address = uint(header + 8)
            val offset = uint(header + 4)
            val fileSize = uint(header + 16)
            val memorySize = uint(header + 20)
            require(
              address == uint(header + 12),
              "ELF requires virtual addressing"
            )
            require(
              address >= base && address + memorySize <= base + ram.length
            )
            require(fileSize <= memorySize && offset + fileSize <= bytes.length)
            System.arraycopy(
              bytes,
              offset.toInt,
              ram,
              (address - base).toInt,
              fileSize.toInt
            )
          }
        }
        def read(address: Long, size: Int): Long = {
          require(
            address >= base && address + size <= base + ram.length,
            f"read at 0x$address%08x"
          )
          (0 until size).foldLeft(0L) { (value, index) =>
            value | ((ram(
              (address - base).toInt + index
            ).toLong & 255) << (8 * index))
          }
        }
        val limit = sys.env.getOrElse("PIPELINE5_ACT4_CYCLES", "1000000").toInt
        test(new Pipeline5(resetPc = entry, externalMemory = true)) { dut =>
          val fetch = dut.io.instructionMemory.get
          val data = dut.io.dataMemory.get
          var result = 0L
          var cycle = 0
          while (result == 0 && cycle < limit) {
            fetch.data.poke(read(fetch.address.peekInt().toLong, 4).U)
            if (data.load.peekBoolean() || data.store.peekBoolean()) {
              val address = data.address.peekInt().toLong
              val size = 1 << data.size.peekInt().toInt
              if (data.load.peekBoolean()) {
                val value = read(address, size)
                val shift = 64 - size * 8
                val extended =
                  if (data.unsigned.peekBoolean()) value
                  else (value << shift) >> shift
                data.readData.poke((extended & 0xffffffffL).U)
              } else {
                val value = data.writeData.peekInt().toLong
                require(
                  address >= base && address + size <= base + ram.length,
                  f"store at 0x$address%08x"
                )
                if (address == tohost && size == 4 && read(tohost + 4, 4) == 0)
                  result = value
                for (index <- 0 until size)
                  ram((address - base).toInt + index) =
                    (value >>> (8 * index)).toByte
              }
            }
            if (result == 0) {
              assert(
                !dut.io.illegal.peekBoolean(),
                s"illegal instruction at cycle $cycle"
              )
              dut.clock.step()
            }
            cycle += 1
          }
          assert(
            result == 1,
            s"$filename reached tohost=$result after $cycle cycles (limit $limit)"
          )
        }
      }
    }
  }
}
