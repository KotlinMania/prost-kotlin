// port-lint: tests encoding/varint.rs
package io.github.kotlinmania.prost

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.bytes.BytesMut
import io.github.kotlinmania.prost.encoding.decodeVarint
import io.github.kotlinmania.prost.encoding.encodeVarint
import io.github.kotlinmania.prost.encoding.encodedLenVarint
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VarintTest {
    private fun check(value: ULong, expected: ByteArray) {
        // Small buffer
        val bufSmall = BytesMut.withCapacity(1)
        encodeVarint(value, bufSmall)
        assertContentEquals(expected, bufSmall.freeze().asSlice())

        // Large buffer
        val bufLarge = BytesMut.withCapacity(100)
        encodeVarint(value, bufLarge)
        assertContentEquals(expected, bufLarge.freeze().asSlice())

        assertEquals(expected.size, encodedLenVarint(value))

        val readBuf = expected.asBuf()
        val roundtripResult = decodeVarint(readBuf)
        assertTrue(roundtripResult.isSuccess, "decoding failed for $value")
        assertEquals(value, roundtripResult.getOrThrow())
    }

    @Test
    fun testVarint() {
        check(0uL, byteArrayOf(0x00))
        check(1uL, byteArrayOf(0x01))

        check(127uL, byteArrayOf(0x7F))
        check(128uL, byteArrayOf(0x80.toByte(), 0x01))
        check(300uL, byteArrayOf(0xAC.toByte(), 0x02))

        check(16383uL, byteArrayOf(0xFF.toByte(), 0x7F))
        check(16384uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x01))

        check(2097151uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(2097152uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(268435455uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(268435456uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(34359738367uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(34359738368uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(4398046511103uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(4398046511104uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(562949953421311uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(562949953421312uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(72057594037927935uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(72057594037927936uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(9223372036854775807uL, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F))
        check(9223372036854775808uL, byteArrayOf(0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x80.toByte(), 0x01))

        check(ULong.MAX_VALUE, byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x01))
    }

    @Test
    fun testVarintOverflow() {
        val u64MaxPlusOne = byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x02,
        )
        val buf = u64MaxPlusOne.asBuf()
        val res = decodeVarint(buf)
        assertTrue(res.isFailure, "decoding u64::MAX + 1 should fail")
    }
}
