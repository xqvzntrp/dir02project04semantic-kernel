# Architecture

## Purpose

This repository defines a minimal layered architecture for turning event history into meaning.

The central composition is:

verified history -> domain decoder -> domain events -> semantic kernel -> state, next moves, actions

## Layers

### Event Chain

Responsible for external truth of history.

May know:

- files
- canonical encoding
- hashes
- verification
- ordering

Must not know:

- domain semantics
- state interpretation
- workflow meaning

### Domain Decoder

Responsible for converting verified field records into domain events.

May know:

- field names
- event type names
- domain event constructors

Must not know:

- kernel internals
- storage policy beyond its input contract
- rule evaluation

### Semantic Kernel

Responsible for interpreting domain events.

Full state and rule state are intentionally distinct.
The projector derives full domain truth, while rule evaluation operates on a smaller decision surface extracted from that truth.
The system also distinguishes semantic meaning from path quality; quality analysis operates on history structure and does not affect kernel interpretation.

May know:

- domain event values
- projectors
- transition rules
- actions

Must not know:

- files
- hashes
- parsers
- transport formats
- persistence

## Data Flow

1. Event Chain verifies history and ordering.
2. An adapter exposes verified records as structured field events.
3. A domain decoder maps verified field events into domain events.
4. A projector replays domain events into full state.
Projectors own admissibility of domain history. The kernel does not decide whether a history is valid; it only composes projector, rules, and actions around already-decoded domain events.
5. The kernel extracts rule state from full state.
6. A transition table derives valid next moves.
7. An action adapter derives available actions and maps requests back to domain events.

## Core Rule

The kernel is intentionally boring.

Changes to the kernel should happen only when a new domain proof cannot be expressed cleanly through:

- a projector
- a rule-state extractor
- a transition table
- an action adapter

## Validation Status

The architecture has been validated with:

- Task domain proof: linear lifecycle
- Approval domain proof: branching lifecycle
- Account-task domain proof: composite state where account status constrains task actions
- Semantic kernel invariants: determinism, terminal emptiness, projector-owned invalid history
- Event Chain integration proof: verified field events decoded into task events and analyzed by the kernel
- Account-task integration proof: verified field events decoded into composite account-task events and analyzed by the kernel

## Practical Boundaries

`src/semantic/`
: generic kernel, rules, snapshot, invariants

`src/task/domain/`
: minimal linear proof domain

`src/approval/domain/`
: minimal branching proof domain

`src/accounttask/domain/`
: composite proof domain for account-conditioned task flow

`src/integration/eventchain/`
: adapter and decoder boundary for verified history

`samples/eventchain/`
: sample verified inputs for boundary proofs

`samples/eventchain/accounttask/`
: sample verified inputs for composite account-conditioned work proofs
