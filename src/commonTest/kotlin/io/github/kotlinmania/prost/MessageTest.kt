// port-lint: tests prost/src/message.rs
package io.github.kotlinmania.prost

import io.github.kotlinmania.bytes.Bytes
import io.github.kotlinmania.bytes.buf.Buf
import io.github.kotlinmania.bytes.buf.BufMut
import io.github.kotlinmania.prost.encoding.DecodeContext
import io.github.kotlinmania.prost.encoding.Int32Encoding
import io.github.kotlinmania.prost.encoding.StringEncoding
import io.github.kotlinmania.prost.encoding.WireType
import io.github.kotlinmania.prost.encoding.skipField
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class TestPerson(
    var id: Int = 0,
    var name: String = "",
) : Message {
    override fun encodeRaw(buf: BufMut) {
        if (id != 0) {
            Int32Encoding.encode(1u, id, buf)
        }
        if (name.isNotEmpty()) {
            StringEncoding.encode(2u, name, buf)
        }
    }

    override fun mergeField(
        tag: UInt,
        wireType: WireType,
        buf: Buf,
        ctx: DecodeContext,
    ): Result<Unit> {
        return when (tag) {
            1u -> {
                val res = Int32Encoding.merge(wireType, buf, ctx)
                if (res.isSuccess) {
                    id = res.getOrThrow()
                    Result.success(Unit)
                } else Result.failure(res.exceptionOrNull()!!)
            }
            2u -> {
                val res = StringEncoding.merge(wireType, buf, ctx)
                if (res.isSuccess) {
                    name = res.getOrThrow()
                    Result.success(Unit)
                } else Result.failure(res.exceptionOrNull()!!)
            }
            else -> skipField(wireType, tag, buf, ctx)
        }
    }

    override fun encodedLen(): Int {
        var len = 0
        if (id != 0) {
            len += Int32Encoding.encodedLen(1u, id)
        }
        if (name.isNotEmpty()) {
            len += StringEncoding.encodedLen(2u, name)
        }
        return len
    }

    override fun clear() {
        id = 0
        name = ""
    }

    override fun equals(other: Any?): Boolean =
        other is TestPerson && id == other.id && name == other.name

    override fun hashCode(): Int = 31 * id + name.hashCode()
}

class MessageTest {
    @Test
    fun testMessageEncodeDecode() {
        val person = TestPerson(id = 123, name = "Alice")
        val bytes = person.encodeToByteArray()

        val decoded = TestPerson()
        decoded.merge(bytes).getOrThrow()
        assertEquals(person, decoded)
    }

    @Test
    fun testLengthDelimited() {
        val person = TestPerson(id = 456, name = "Bob")
        val bytes = person.encodeLengthDelimitedToByteArray()

        val decoded = TestPerson()
        decoded.mergeLengthDelimited(bytes).getOrThrow()
        assertEquals(person, decoded)
    }
}
