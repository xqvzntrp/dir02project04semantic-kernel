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
