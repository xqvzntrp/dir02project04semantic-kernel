# Semantic Kernel

A minimal deterministic engine for interpreting event histories.

## Core Idea

Given a list of events:

- derive current state
- derive valid next moves
- derive available actions
- convert actions into new events

## Non-Goals

- no storage
- no hashing
- no parsing
- no CLI
- no domain logic

Those are handled outside this kernel.

## Stability Note

The kernel has been validated against:

- a linear lifecycle domain (task)
- a branching lifecycle domain (approval)
- a composite account-conditioned work domain (accounttask)

The repository also includes verified sample histories and law suites that
exercise those domains through the event-chain boundary, not only through
direct in-memory proofs.

At this stage, the kernel is intentionally "boring" and stable.
Changes should only be made in response to new domain proofs that cannot be expressed cleanly.

## Extending the System

To add new behavior, follow the extension guide:

See `EXTENDING.md`

For how histories are compared once meaning has been derived, see `EQUIVALENCE.md`.
For structural quality of histories apart from semantics, see `HISTORY_QUALITY.md`.

It walks through the exact steps and points to the relevant domain files,
including the projector, domain kernel, action adapter, and decoder boundary.

For the generated whole-project review bundle, see `../BUNDLE_INDEX.md`
and `../all-sources.txt`.
