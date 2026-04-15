# Equivalence Model

Histories are compared along independent dimensions:

- Structural: identical event history
- State: identical derived state
- Actions: identical available action surface

Equivalence is a family of cases defined by capabilities, not a total ordering.

## Capabilities

- `preservesState()`
- `preservesActions()`
- structural equality via `EXACT`

Named cases such as `EXACT`, `STATE_AND_ACTIONS_EQUAL`, `ACTIONS_EQUAL_ONLY`,
`STATE_EQUAL_ONLY`, and `NONE` are combinations of those properties.

## Semantics

- State is the primary semantic carrier.
- Actions are a derived projection of state.

Observed in current domains:

- `preservesState()` implies `preservesActions()`
- `preservesActions()` does not imply `preservesState()`
- terminal states may differ in state while sharing an identical empty action surface

## Convergence

Semantic convergence requires both:

`preservesState() && preservesActions()`

This corresponds to `EXACT` or `STATE_AND_ACTIONS_EQUAL`.

## State Completeness (Current Domains)

In current domains, projectors encode all decision-relevant history into derived
state.

Therefore:

`preservesState() => preservesActions()`

Implications:

- Histories that preserve state also preserve the available action surface.
- `STATE_EQUAL_ONLY` does not occur under a complete projector.

This is an observed invariant of the current architecture, not a property of
equivalence itself.

It holds as long as:

- rules operate only on derived state
- no external context (time, permissions, environment) influences action availability
- projectors fully capture all information required for rule evaluation

In domains with terminal states, distinct states may also share an identical
empty action surface because no outgoing transitions remain. This yields
`ACTIONS_EQUAL_ONLY`, not semantic convergence: terminal states can be
operationally indistinguishable while remaining semantically distinct in state.

## Usage

Policies should depend on capabilities, not enum ordering or fragile name checks.
