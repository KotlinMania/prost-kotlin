# port-lint Proposed Changes

**Generated:** 2026-08-31
**Source:** tmp/prost/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/prost

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/prost/Message.kt` | `// port-lint: source prost/src/message.rs` | `// port-lint: source message.rs` | `message.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/message.rs' vs expected 'message.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/prost/MessageTest.kt` | `// port-lint: tests prost/src/message.rs` | `// port-lint: tests message.rs` | `message.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:prost/src/message.rs' vs expected 'message.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/encoding/WireType.kt` | `// port-lint: source prost/src/encoding/wire_type.rs` | `// port-lint: source encoding/wire_type.rs` | `encoding/wire_type.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/encoding/wire_type.rs' vs expected 'encoding/wire_type.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/Name.kt` | `// port-lint: source prost/src/name.rs` | `// port-lint: source name.rs` | `name.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/name.rs' vs expected 'name.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/encoding/Encoding.kt` | `// port-lint: source prost/src/encoding.rs` | `// port-lint: source encoding.rs` | `encoding.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/encoding.rs' vs expected 'encoding.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/prost/EncodingTest.kt` | `// port-lint: tests prost/src/encoding.rs` | `// port-lint: tests encoding.rs` | `encoding.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:prost/src/encoding.rs' vs expected 'encoding.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/Error.kt` | `// port-lint: source prost/src/error.rs` | `// port-lint: source error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/error.rs' vs expected 'error.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/prost/ErrorTest.kt` | `// port-lint: tests prost/src/error.rs` | `// port-lint: tests error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:prost/src/error.rs' vs expected 'error.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/encoding/Varint.kt` | `// port-lint: source prost/src/encoding/varint.rs` | `// port-lint: source encoding/varint.rs` | `encoding/varint.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/encoding/varint.rs' vs expected 'encoding/varint.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/prost/VarintTest.kt` | `// port-lint: tests prost/src/encoding/varint.rs` | `// port-lint: tests encoding/varint.rs` | `encoding/varint.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:prost/src/encoding/varint.rs' vs expected 'encoding/varint.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/Types.kt` | `// port-lint: source prost/src/types.rs` | `// port-lint: source types.rs` | `types.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/types.rs' vs expected 'types.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/prost/TypesTest.kt` | `// port-lint: tests prost/src/types.rs` | `// port-lint: tests types.rs` | `types.rs` | `port-lint provenance header matched only after fallback normalization: 'tests:prost/src/types.rs' vs expected 'types.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/encoding/LengthDelimiter.kt` | `// port-lint: source prost/src/encoding/length_delimiter.rs` | `// port-lint: source encoding/length_delimiter.rs` | `encoding/length_delimiter.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/encoding/length_delimiter.rs' vs expected 'encoding/length_delimiter.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/Lib.kt` | `// port-lint: source prost/src/lib.rs` | `// port-lint: source lib.rs` | `lib.rs` | `port-lint provenance header matched only after fallback normalization: 'prost/src/lib.rs' vs expected 'lib.rs'` |
