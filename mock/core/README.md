# :mock:core

KMP-ready mock foundation for debug development and UI tests.

Responsibilities:
- Root `JsonAsset` marker.
- Mock route and response models.
- `MockResponseRegistry`.
- Core DSL for registering mock responses.

Domain-specific mock routes and assets must live in modules such as `:mock:tiles`.
