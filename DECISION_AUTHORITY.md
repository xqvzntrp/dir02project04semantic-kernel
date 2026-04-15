# Decision Authority

The semantic kernel defines possible actions.
Selection of an action happens at the request boundary.

That means the write path is always:

`history -> kernel -> possible actions -> request -> adapter -> event -> append`

Responsibilities stay split this way:

- The kernel defines what is possible next.
- The request names the intended action.
- The action adapter validates that request against available actions and materializes a domain event.
- The application service orchestrates loading and persistence, but does not choose actions on behalf of the caller.

This keeps decision authority explicit, rejects invalid requests at the semantic boundary, and keeps application code orchestration-only.
