// port-lint: source prost/src/lib.rs
package io.github.kotlinmania.prost

// Re-exports of public API symbols
fun ByteArray.asBuf(): io.github.kotlinmania.bytes.buf.Buf =
    io.github.kotlinmania.bytes.buf
        .ByteArrayBuf(this)

fun io.github.kotlinmania.bytes.Bytes.asBuf(): io.github.kotlinmania.bytes.buf.Buf =
    io.github.kotlinmania.bytes.buf
        .ByteArrayBuf(this.asSlice())
