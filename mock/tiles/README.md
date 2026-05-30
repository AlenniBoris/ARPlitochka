# :mock:tiles

KMP-ready mock module for tile catalog responses and assets.

Responsibilities:
- Tile mock routes, for example `get.tiles.all`.
- Tile mock JSON assets.
- Tile local image assets used by mock responses.

Note: Kotlin extension properties cannot have backing fields, so extension-style access such as `JsonAsset.tiles` is implemented with `get() = TileAssets`.
