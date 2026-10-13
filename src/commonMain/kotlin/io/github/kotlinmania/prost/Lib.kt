// port-lint: source lib.rs
package io.github.kotlinmania.prost

// Re-exports of public API symbols
const val RECURSION_LIMIT: UInt = io.github.kotlinmania.prost.encoding.RECURSION_LIMIT

fun ByteArray.asBuf(): io.github.kotlinmania.bytes.buf.Buf =
    io.github.kotlinmania.bytes.buf
        .ByteArrayBuf(this)

fun io.github.kotlinmania.bytes.Bytes.asBuf(): io.github.kotlinmania.bytes.buf.Buf =
    io.github.kotlinmania.bytes.buf
        .ByteArrayBuf(this.asSlice())
