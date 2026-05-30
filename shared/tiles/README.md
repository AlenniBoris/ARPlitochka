# :shared:tiles

KMP-ready shared business module for tile catalog data.

Responsibilities:
- Tile domain models.
- Tile DTO contracts.
- `TilesApi`, `TilesRepository`, `GetTilesUseCase`.
- DTO-to-domain mapping.

Rules:
- Keep reusable tile logic here only when it is shared between multiple features.
- Feature-specific tile APIs should stay inside the owning feature module.
- Common code must not depend on Android-only APIs.
