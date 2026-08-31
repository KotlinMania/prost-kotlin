# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 9/10 (90.0%)
- **Function parity:** 44/70 matched (target 266) — 62.9%
- **Class/type parity:** 8/12 matched (target 55) — 66.7%
- **Combined symbol parity:** 52/82 matched (target 321) — 63.4%
- **Average inline-code cosine:** 0.53 (function body across 8 matched files)
- **Average documentation cosine:** 0.87 (doc text across 8 matched files)
- **Cheat-zeroed Files:** 1
- **Critical Issues:** 7 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. prost.message

- **Target:** `prost.Message`
- **Similarity:** 0.45
- **Dependents:** 4
- **Priority Score:** 4041305.5
- **Functions:** 8/12 matched (target 18)
- **Missing functions:** `encode_to_vec`, `encode_length_delimited_to_vec`, `decode`, `decode_length_delimited`
- **Types:** 1/1 matched (target 3)
- **Missing types:** _none_

### 2. encoding.wire_type

- **Target:** `encoding.WireType`
- **Similarity:** 0.75
- **Dependents:** 3
- **Priority Score:** 3010402.5
- **Functions:** 2/2 matched
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Error`

### 3. prost.name

- **Target:** `prost.Name`
- **Similarity:** 0.47
- **Dependents:** 2
- **Priority Score:** 2000305.2
- **Functions:** 2/2 matched
- **Missing functions:** _none_
- **Types:** 1/1 matched
- **Missing types:** _none_

### 4. prost.encoding

- **Target:** `encoding.Encoding`
- **Similarity:** 0.50
- **Dependents:** 0
- **Priority Score:** 132705.0
- **Functions:** 13/24 matched (target 152)
- **Missing functions:** `default`, `drop`, `is_empty`, `len`, `replace_with`, `append_to`, `merge_one_copy`, `check_type`, `check_collection_type`, `string_merge_invalid_utf8`, `split_varint_decoding`
- **Types:** 1/3 matched (target 20)
- **Missing types:** `DropGuard`, `BytesAdapter`
- **Tests:** 0/4 matched

### 5. prost.error

- **Target:** `prost.Error`
- **Similarity:** 0.27
- **Dependents:** 0
- **Priority Score:** 51407.3
- **Functions:** 5/9 matched (target 24)
- **Missing functions:** `new`, `fmt`, `from`, `test_into_std_io_error`
- **Types:** 4/5 matched (target 18)
- **Missing types:** `Inner`
- **Tests:** 1/2 matched

### 6. encoding.varint

- **Target:** `encoding.Varint`
- **Similarity:** 0.49
- **Dependents:** 0
- **Priority Score:** 40905.1
- **Functions:** 5/9 matched (target 8)
- **Missing functions:** `varint`, `check`, `varint_overflow`, `variant_slow_overflow`
- **Types:** 0/0 matched (target 1)
- **Missing types:** _none_
- **Tests:** 0/4 matched

### 7. prost.types

- **Target:** `prost.Types`
- **Similarity:** 0.47
- **Dependents:** 0
- **Priority Score:** 10705.3
- **Functions:** 6/7 matched (target 55)
- **Missing functions:** `googleapis_type_url_for`
- **Types:** 0/0 matched (target 11)
- **Missing types:** _none_
- **Tests:** 1/1 matched

### 8. encoding.length_delimiter

- **Target:** `encoding.LengthDelimiter`
- **Similarity:** 0.81
- **Dependents:** 0
- **Priority Score:** 301.9
- **Functions:** 3/3 matched
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

### 9. prost.lib

- **Target:** `prost.Lib [STUB]`
- **Similarity:** 0.00
- **Dependents:** 0
- **Priority Score:** 10.0
- **Functions:** 0/0 matched (target 2)
- **Missing functions:** _none_
- **Types:** 0/0 matched
- **Missing types:** _none_

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

