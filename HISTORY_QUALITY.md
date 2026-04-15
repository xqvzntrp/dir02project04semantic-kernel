# History Quality

## Purpose

This document defines history quality as a dimension separate from admissibility
and semantic meaning.

The system distinguishes three independent concerns:

| Dimension | Question | Owned By |
| --- | --- | --- |
| Admissibility | May this history exist? | Projector |
| Semantics | What does this history mean? | Kernel |
| History Quality | How disciplined was the path taken? | Analysis Layer |

History quality does not affect semantic interpretation or equivalence.

History quality is evaluated along two independent axes:

1. Structural Quality (reducer-based)
   - minimality under admissible, order-preserving deletion
   - metrics: `reducedLength`, `redundancy`, `locallyMinimal`
2. Flow Quality (pattern-based)
   - measures historical discipline of the original path
   - metrics: `repairCount`, `flowStable`

These axes are independent.

A history may be:

- structurally minimal but flow-unstable
- structurally reducible but flow-stable

Neither axis affects semantic meaning.

## Core Principle

Semantic equivalence does not imply path-quality equivalence.

Two histories may:

- produce the same state
- expose the same actions

and still differ in:

- stability
- minimality
- redundancy

## Key Law

Repeated successful repairs are semantically invisible but historically observable.

If a sequence of repairs:

- restores valid state
- does not change final state or actions

then:

- it does not affect semantic equivalence
- but it does affect history quality

## Definitions

### Stable Flow

A history is stable if it does not contain unnecessary detours or repair cycles.

Informally:

No step is later undone or re-derived without necessity.

### Minimality

A history is minimal if no order-preserving, admissible subsequence preserves both:

- final semantic state
- available actions

### Redundancy

A history contains redundancy if it includes steps that:

- do not contribute to final semantic state
- and are later negated or superseded

### Repair Count

The number of times a history:

- leaves a desired state
- and returns to it via repair actions

### Detour Depth

A measure of how far a history diverges from a stable path before returning.

## Relationship to Equivalence

From `EQUIVALENCE.md`:

- state is the primary semantic carrier
- actions are a derived projection of state

History quality operates outside this model.

Two histories may satisfy:

- `STATE_AND_ACTIONS_EQUAL`

while differing in:

- repair count
- redundancy
- stability

## Non-Goals

History quality does not:

- affect admissibility
- affect semantic state
- affect action derivation
- participate in equivalence classification

## Use Cases

History quality enables:

### Analysis

- detect unstable workflows
- identify redundant transitions

### Optimization

- compress histories to minimal form
- suggest canonical paths

### Policy

- enforce limits on repair cycles
- require stable flows for acceptance

### Tooling

- highlight clean vs repair-heavy histories
- visualize divergence and convergence

## Summary

History quality is a third axis of evaluation:

`validity != meaning != path quality`

The system preserves a strict separation:

- meaning is derived from state
- path quality is derived from structure

Both are observable.
Only one affects semantics.
