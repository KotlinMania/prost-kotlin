package io.github.kotlinmania.prost

fun ByteArray.asBuf(): io.github.kotlinmania.bytes.buf.Buf =
    io.github.kotlinmania.bytes.buf
        .ByteArrayBuf(this)

fun io.github.kotlinmania.bytes.Bytes.asBuf(): io.github.kotlinmania.bytes.buf.Buf =
    io.github.kotlinmania.bytes.buf
        .ByteArrayBuf(this.asSlice())
