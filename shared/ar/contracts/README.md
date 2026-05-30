# :shared:ar:contracts

KMP-ready contracts for the AR floor selection scenario.

This module contains shared state, events, commands and geometry normalization used by Android ARCore, iOS ARKit and future MR implementations.

Rules:
- No ARCore, ARKit or platform rendering dependencies in `commonMain`.
- Platform modules implement detection, anchors and rendering.
- Shared contracts describe behavior and data flow, not engine-specific APIs.
