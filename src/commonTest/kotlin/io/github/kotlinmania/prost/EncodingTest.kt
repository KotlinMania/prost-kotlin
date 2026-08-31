// port-lint: tests prost/src/encoding.rs
package io.github.kotlinmania.prost

import io.github.kotlinmania.bytes.BytesMut
import io.github.kotlinmania.bytes.buf.ByteArrayBuf
import io.github.kotlinmania.prost.encoding.DecodeContext
import io.github.kotlinmania.prost.encoding.DoubleEncoding
import io.github.kotlinmania.prost.encoding.Fixed32Encoding
import io.github.kotlinmania.prost.encoding.Fixed64Encoding
import io.github.kotlinmania.prost.encoding.FloatEncoding
import io.github.kotlinmania.prost.encoding.Int32Encoding
import io.github.kotlinmania.prost.encoding.Int64Encoding
import io.github.kotlinmania.prost.encoding.MapEncoding
import io.github.kotlinmania.prost.encoding.SFixed32Encoding
import io.github.kotlinmania.prost.encoding.SFixed64Encoding
import io.github.kotlinmania.prost.encoding.SInt32Encoding
import io.github.kotlinmania.prost.encoding.SInt64Encoding
import io.github.kotlinmania.prost.encoding.StringEncoding
import io.github.kotlinmania.prost.encoding.UInt32Encoding
import io.github.kotlinmania.prost.encoding.UInt64Encoding
import io.github.kotlinmania.prost.encoding.WireType
import io.github.kotlinmania.prost.encoding.decodeKey
import io.github.kotlinmania.prost.encoding.decodeVarint
import io.github.kotlinmania.prost.encoding.encodeKey
import io.github.kotlinmania.prost.encoding.encodeVarint
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EncodingTest {
    @Test
    fun testKeyEncodeDecode() {
        for (tag in listOf(1u, 2u, 15u, 16u, 2047u, 2048u, 0x1FFFFFFFu)) {
            for (wt in listOf(
                WireType.Varint,
                WireType.SixtyFourBit,
                WireType.LengthDelimited,
                WireType.StartGroup,
                WireType.EndGroup,
                WireType.ThirtyTwoBit,
            )) {
                val buf = BytesMut.withCapacity(10)
                encodeKey(tag, wt, buf)
                val readBuf = buf.freeze().asSlice().asBuf()
                val decoded = decodeKey(readBuf).getOrThrow()
                assertEquals(tag, decoded.first)
                assertEquals(wt, decoded.second)
            }
        }
    }

    @Test
    fun testStringMergeInvalidUtf8() {
        val invalidBytes = byteArrayOf(0x02, 0x80.toByte(), 0x80.toByte())
        val buf = invalidBytes.asBuf()
        val res = StringEncoding.merge(WireType.LengthDelimited, buf, DecodeContext())
        assertTrue(res.isFailure, "must be an error for invalid UTF-8")
    }

    @Test
    fun testSplitVarintDecoding() {
        val testValues = mutableListOf<ULong>()
        testValues.add(128uL)
        for (i in 2..8) {
            testValues.add((1uL shl (7 * i)) - 1uL)
            testValues.add(1uL shl (7 * i))
        }

        for (v in testValues) {
            val buf = BytesMut.withCapacity(10)
            encodeVarint(v, buf)
            val frozen = buf.freeze()
            val slice = frozen.asSlice()
            val halfLen = slice.size / 2

            val b1 = ByteArrayBuf(slice.copyOfRange(0, halfLen))
            val b2 = ByteArrayBuf(slice.copyOfRange(halfLen, slice.size))
            val chained = b1.chain(b2)

            assertEquals(slice.size, chained.remaining())
            val decoded = decodeVarint(chained).getOrThrow()
            assertEquals(v, decoded)
        }
    }

    @Test
    fun testAllNumericTypesRoundtrip() {
        // Int32
        val i32Buf = BytesMut.withCapacity(32)
        Int32Encoding.encode(1u, -12345, i32Buf)
        val i32Decoded =
            Int32Encoding
                .merge(
                    WireType.Varint,
                    i32Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(-12345, i32Decoded)

        // Int64
        val i64Buf = BytesMut.withCapacity(32)
        Int64Encoding.encode(2u, -9876543210L, i64Buf)
        val i64Decoded =
            Int64Encoding
                .merge(
                    WireType.Varint,
                    i64Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(-9876543210L, i64Decoded)

        // UInt32
        val u32Buf = BytesMut.withCapacity(32)
        UInt32Encoding.encode(3u, 12345u, u32Buf)
        val u32Decoded =
            UInt32Encoding
                .merge(
                    WireType.Varint,
                    u32Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(12345u, u32Decoded)

        // UInt64
        val u64Buf = BytesMut.withCapacity(32)
        UInt64Encoding.encode(4u, 9876543210uL, u64Buf)
        val u64Decoded =
            UInt64Encoding
                .merge(
                    WireType.Varint,
                    u64Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(9876543210uL, u64Decoded)

        // SInt32
        val s32Buf = BytesMut.withCapacity(32)
        SInt32Encoding.encode(5u, -12345, s32Buf)
        val s32Decoded =
            SInt32Encoding
                .merge(
                    WireType.Varint,
                    s32Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(-12345, s32Decoded)

        // SInt64
        val s64Buf = BytesMut.withCapacity(32)
        SInt64Encoding.encode(6u, -9876543210L, s64Buf)
        val s64Decoded =
            SInt64Encoding
                .merge(
                    WireType.Varint,
                    s64Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(-9876543210L, s64Decoded)

        // Fixed32
        val f32Buf = BytesMut.withCapacity(32)
        Fixed32Encoding.encode(7u, 0x12345678u, f32Buf)
        val f32Decoded =
            Fixed32Encoding
                .merge(
                    WireType.ThirtyTwoBit,
                    f32Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(0x12345678u, f32Decoded)

        // Fixed64
        val f64Buf = BytesMut.withCapacity(32)
        Fixed64Encoding.encode(8u, 0x123456789ABCDEF0uL, f64Buf)
        val f64Decoded =
            Fixed64Encoding
                .merge(
                    WireType.SixtyFourBit,
                    f64Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(0x123456789ABCDEF0uL, f64Decoded)

        // SFixed32
        val sf32Buf = BytesMut.withCapacity(32)
        SFixed32Encoding.encode(9u, -42, sf32Buf)
        val sf32Decoded =
            SFixed32Encoding
                .merge(
                    WireType.ThirtyTwoBit,
                    sf32Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(-42, sf32Decoded)

        // SFixed64
        val sf64Buf = BytesMut.withCapacity(32)
        SFixed64Encoding.encode(10u, -99999999L, sf64Buf)
        val sf64Decoded =
            SFixed64Encoding
                .merge(
                    WireType.SixtyFourBit,
                    sf64Buf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(-99999999L, sf64Decoded)

        // Float
        val fltBuf = BytesMut.withCapacity(32)
        FloatEncoding.encode(11u, 3.14159f, fltBuf)
        val fltDecoded =
            FloatEncoding
                .merge(
                    WireType.ThirtyTwoBit,
                    fltBuf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(3.14159f, fltDecoded, 1e-5f)

        // Double
        val dblBuf = BytesMut.withCapacity(32)
        DoubleEncoding.encode(12u, 2.718281828459, dblBuf)
        val dblDecoded =
            DoubleEncoding
                .merge(
                    WireType.SixtyFourBit,
                    dblBuf
                        .freeze()
                        .asSlice()
                        .asBuf()
                        .apply { decodeKey(this) },
                    DecodeContext(),
                ).getOrThrow()
        assertEquals(2.718281828459, dblDecoded)
    }

    @Test
    fun testPackedNumericEncoding() {
        val values = listOf(1, 2, 3, 1000, -500)
        val buf = BytesMut.withCapacity(64)
        Int32Encoding.encodePacked(1u, values, buf)

        val readBuf = buf.freeze().asSlice().asBuf()
        val (tag, wireType) = decodeKey(readBuf).getOrThrow()
        assertEquals(1u, tag)
        assertEquals(WireType.LengthDelimited, wireType)

        val merged = mutableListOf<Int>()
        Int32Encoding.mergeRepeated(WireType.LengthDelimited, merged, readBuf, DecodeContext()).getOrThrow()
        assertContentEquals(values, merged)
    }

    @Test
    fun testMapEncodingRoundtrip() {
        val map = mapOf("key1" to 100, "key2" to 200)
        val buf = BytesMut.withCapacity(128)
        MapEncoding.encode(
            keyEncode = { tag, k, b -> StringEncoding.encode(tag, k, b) },
            keyEncodedLen = { tag, k -> StringEncoding.encodedLen(tag, k) },
            valEncode = { tag, v, b -> Int32Encoding.encode(tag, v, b) },
            valEncodedLen = { tag, v -> Int32Encoding.encodedLen(tag, v) },
            valDefault = 0,
            tag = 5u,
            values = map,
            buf = buf,
        )

        val readBuf = buf.freeze().asSlice().asBuf()
        val decodedMap = mutableMapOf<String, Int>()
        while (readBuf.hasRemaining()) {
            val (tag, _) = decodeKey(readBuf).getOrThrow()
            assertEquals(5u, tag)
            MapEncoding
                .merge(
                    keyMerge = { wt, b, c -> StringEncoding.merge(wt, b, c) },
                    valMerge = { wt, b, c -> Int32Encoding.merge(wt, b, c) },
                    defaultKey = "",
                    defaultVal = 0,
                    values = decodedMap,
                    buf = readBuf,
                    ctx = DecodeContext(),
                ).getOrThrow()
        }

        assertEquals(map, decodedMap)
    }
}
