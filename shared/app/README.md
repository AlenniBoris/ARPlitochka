# :shared:app

Compose Multiplatform app layer shared by Android and iOS.

Responsibilities:
- Common root composable.
- Common catalog UI shell.
- Navigation between catalog, transition screen and platform AR slot.
- Common bottom bar for routes where product navigation is visible.
- Common UI state that does not depend on Android-only APIs.

Platform AR implementations are injected into the shared app through an `arContent` slot.

Android and iOS entry points must both start from `ArPlitkaSharedApp`. Platform modules may provide permissions, ARCore/ARKit checks and rendering content, but they must not define a separate product navigation flow.
