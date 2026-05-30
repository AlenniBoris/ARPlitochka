# :network:core

KMP-ready technical network foundation.

This module contains only common network primitives: result wrappers, errors, configuration and future Ktor/Ktorfit factories.

Rules:
- No business endpoints.
- No Android-only dependencies in `commonMain`.
- Domain-specific APIs live in feature modules or shared business modules such as `:shared:tiles`.
