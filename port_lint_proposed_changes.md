# port-lint Proposed Changes

**Generated:** 2026-08-25
**Source:** tmp/prost/src
**Target:** src/commonMain/kotlin/io/github/kotlinmania/prost

These are review proposals only. They are emitted when a Rust -> Kotlin pair matches only after fallback normalization, so the existing `port-lint` header is not an exact provenance match.

| Target file | Current header | Proposed header | Source path | Reason |
|-------------|----------------|-----------------|-------------|--------|
| `src/commonMain/kotlin/io/github/kotlinmania/prost/encoding/WireType.kt` | `// port-lint: source src/encoding/wire_type.rs` | `// port-lint: source encoding/wire_type.rs` | `encoding/wire_type.rs` | `port-lint provenance header matched only after fallback normalization: 'src/encoding/wire_type.rs' vs expected 'encoding/wire_type.rs'` |
| `src/commonMain/kotlin/io/github/kotlinmania/prost/Error.kt` | `// port-lint: source src/error.rs` | `// port-lint: source error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'src/error.rs' vs expected 'error.rs'` |
| `src/commonTest/kotlin/io/github/kotlinmania/prost/ErrorTest.kt` | `// port-lint: source src/error.rs` | `// port-lint: source error.rs` | `error.rs` | `port-lint provenance header matched only after fallback normalization: 'src/error.rs' vs expected 'error.rs'` |
