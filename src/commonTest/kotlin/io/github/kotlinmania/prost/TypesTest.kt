// port-lint: tests prost/src/types.rs
package io.github.kotlinmania.prost

import kotlin.test.Test
import kotlin.test.assertEquals

class TypesTest {
    @Test
    fun testImplName() {
        val boolVal = BoolValue()
        assertEquals("BoolValue", boolVal.typeName)
        assertEquals("google.protobuf", boolVal.packageName)
        assertEquals("google.protobuf.BoolValue", boolVal.fullName())
        assertEquals("type.googleapis.com/google.protobuf.BoolValue", boolVal.typeUrl())

        val u32Val = UInt32Value()
        assertEquals("UInt32Value", u32Val.typeName)
        assertEquals("google.protobuf", u32Val.packageName)
        assertEquals("google.protobuf.UInt32Value", u32Val.fullName())
        assertEquals("type.googleapis.com/google.protobuf.UInt32Value", u32Val.typeUrl())

        val u64Val = UInt64Value()
        assertEquals("UInt64Value", u64Val.typeName)
        assertEquals("google.protobuf", u64Val.packageName)
        assertEquals("google.protobuf.UInt64Value", u64Val.fullName())
        assertEquals("type.googleapis.com/google.protobuf.UInt64Value", u64Val.typeUrl())

        val i32Val = Int32Value()
        assertEquals("Int32Value", i32Val.typeName)
        assertEquals("google.protobuf", i32Val.packageName)
        assertEquals("google.protobuf.Int32Value", i32Val.fullName())
        assertEquals("type.googleapis.com/google.protobuf.Int32Value", i32Val.typeUrl())

        val i64Val = Int64Value()
        assertEquals("Int64Value", i64Val.typeName)
        assertEquals("google.protobuf", i64Val.packageName)
        assertEquals("google.protobuf.Int64Value", i64Val.fullName())
        assertEquals("type.googleapis.com/google.protobuf.Int64Value", i64Val.typeUrl())

        val f32Val = FloatValue()
        assertEquals("FloatValue", f32Val.typeName)
        assertEquals("google.protobuf", f32Val.packageName)
        assertEquals("google.protobuf.FloatValue", f32Val.fullName())
        assertEquals("type.googleapis.com/google.protobuf.FloatValue", f32Val.typeUrl())

        val f64Val = DoubleValue()
        assertEquals("DoubleValue", f64Val.typeName)
        assertEquals("google.protobuf", f64Val.packageName)
        assertEquals("google.protobuf.DoubleValue", f64Val.fullName())
        assertEquals("type.googleapis.com/google.protobuf.DoubleValue", f64Val.typeUrl())

        val strVal = StringValue()
        assertEquals("StringValue", strVal.typeName)
        assertEquals("google.protobuf", strVal.packageName)
        assertEquals("google.protobuf.StringValue", strVal.fullName())
        assertEquals("type.googleapis.com/google.protobuf.StringValue", strVal.typeUrl())

        val bytesVal = BytesValue()
        assertEquals("BytesValue", bytesVal.typeName)
        assertEquals("google.protobuf", bytesVal.packageName)
        assertEquals("google.protobuf.BytesValue", bytesVal.fullName())
        assertEquals("type.googleapis.com/google.protobuf.BytesValue", bytesVal.typeUrl())

        val emptyVal = Empty
        assertEquals("Empty", emptyVal.typeName)
        assertEquals("google.protobuf", emptyVal.packageName)
        assertEquals("google.protobuf.Empty", emptyVal.fullName())
        assertEquals("type.googleapis.com/google.protobuf.Empty", emptyVal.typeUrl())
    }

    @Test
    fun testRoundtripWrapperTypes() {
        val b = BoolValue(true)
        val bBytes = b.encodeToByteArray()
        val bDecoded = BoolValue()
        bDecoded.merge(bBytes).getOrThrow()
        assertEquals(true, bDecoded.value)

        val s = StringValue("hello world")
        val sBytes = s.encodeToByteArray()
        val sDecoded = StringValue()
        sDecoded.merge(sBytes).getOrThrow()
        assertEquals("hello world", sDecoded.value)

        val i = Int32Value(42)
        val iBytes = i.encodeToByteArray()
        val iDecoded = Int32Value()
        iDecoded.merge(iBytes).getOrThrow()
        assertEquals(42, iDecoded.value)
    }
}
