# Extending the System

This repository is designed so that domain behavior is defined in the domain
layer.

To extend the system, you do not modify the semantic kernel. You teach the
domain how to interpret a new kind of event.

## Core Rule

To add behavior, add an event and teach the domain what it means.

This always happens through the same sequence.

## Extension Steps

### 1. Add a Domain Event

Define a new event in your domain.

Example:
`src/task/domain/TaskReopened.java`

```java
public record TaskReopened(String taskId) implements TaskEvent {}
```

This answers:

- What happened?

### 2. Update the Projector

Teach the projector how the new event affects state and which histories are
invalid.

File:
`src/task/domain/TaskProjector.java`

```java
if (event instanceof TaskReopened) {
    if (state.status() != TaskStatus.COMPLETED) {
        throw new IllegalStateException("task can only reopen from COMPLETED");
    }
    state = new TaskState(state.id(), TaskStatus.IN_PROGRESS);
}
```

The projector:

- derives full state from history
- enforces validity of event sequences

The projector owns admissibility of history.

### 3. Add a Transition Rule

Define when the event is allowed and what it leads to.

File:
`src/task/domain/TaskDomainKernel.java`

```java
new TransitionRule<>(
    "reopen",
    Set.of(TaskStatus.COMPLETED),
    TaskStatus.IN_PROGRESS,
    List.of(),
    false,
    false)
```

This answers:

- When is this event allowed, and what state does it produce?

### 4. Map the Action

Expose the transition as an action and map it back to an event.

File:
`src/task/domain/TaskActionAdapter.java`

```java
case "reopen" -> new TaskReopened(taskId);
```

This answers:

- How does a request become an event?

### 5. Update the Decoder (If Needed)

Only required if the event comes from external history.

File:
`src/integration/eventchain/TaskEventChainDecoder.java`

```java
case "TaskReopened" -> new TaskReopened(taskId);
```

This answers:

- How do external records become domain events?

## What You Do Not Change

You should not modify:

- `src/semantic/kernel/*`
- `src/semantic/rules/*`
- `src/semantic/snapshot/*`

These packages are generic and stable.

If you feel the need to change them, it likely means the domain model is still
incomplete.

## Mental Model

All behavior follows this flow:

`events -> projector -> state -> rules -> actions -> events`

Or more simply:

`history -> meaning -> next possibilities`

## Boundary Reminder

- Files, CLI, APIs, and decoders are outside meaning
- Projectors, rules, and action adapters define meaning

Only the domain layer is allowed to interpret events.

## Summary

To extend the system:

1. Add a new event
2. Update the projector
3. Add a transition rule
4. Map the action
5. Optionally update the decoder

That is the path for adding new behavior.

## One-Line Rule

Record what happened. Teach the domain what it means.
