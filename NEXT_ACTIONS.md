# Immediate Actions - High-Value Files

Based on AST analysis, here are the concrete next steps.

## Summary

- **Files Present:** 2/9 (22.2%)
- **Function parity:** 7/133 matched (target 26) — 5.3%
- **Class/type parity:** 5/12 matched (target 19) — 41.7%
- **Combined symbol parity:** 12/145 matched (target 45) — 8.3%
- **Average inline-code cosine:** 0.51 (function body across 2 matched files)
- **Average documentation cosine:** 0.99 (doc text across 2 matched files)
- **Cheat-zeroed Files:** 0
- **Critical Issues:** 1 files with <0.60 function similarity

## Priority 1: Fix Incomplete High-Dependency Files

No incomplete high-dependency files detected.

## Priority 2: Port Missing High-Value Files

Critical missing files (>10 dependencies):

No missing high-value files detected.

## Detailed Work Items

Every matched file is listed below with function and type symbol parity.

### 1. encoding.wire_type

- **Target:** `encoding.WireType`
- **Similarity:** 0.75
- **Dependents:** 3
- **Priority Score:** 3010402.5
- **Functions:** 2/2 matched
- **Missing functions:** _none_
- **Types:** 1/2 matched (target 1)
- **Missing types:** `Error`

### 2. error

- **Target:** `prost.Error`
- **Similarity:** 0.27
- **Dependents:** 0
- **Priority Score:** 51407.3
- **Functions:** 5/9 matched (target 24)
- **Missing functions:** `new`, `fmt`, `from`, `test_into_std_io_error`
- **Types:** 4/5 matched (target 18)
- **Missing types:** `Inner`
- **Tests:** 1/2 matched

## Success Criteria

For each file to be considered "complete":
- **Similarity ≥ 0.85** (Excellent threshold)
- All public APIs ported
- All tests ported
- Documentation ported
- port-lint header present

## Reexport / Wiring Modules

These files match `reexport_modules` patterns in `.ast_distance_config.json`. They are filtered out of
normal priority and missing-file ladders because they are wiring
modules, not direct logic ports. Consult them for call-site routing;
do not treat them as the next implementation target by default.

### Missing

| Source | Expected target | Deps | Source path | Expected path |
|--------|-----------------|------|-------------|---------------|
| `lib` | `Lib` | 0 | `lib.rs` | `Lib.kt` |

