# :shared:app

Compose Multiplatform app layer shared by Android and iOS.

Responsibilities:
- Common root composable.
- Common catalog UI shell.
- Navigation between catalog, transition screen and platform AR slot.
- Integration with `:shared:ui:navigation` for a unified Bottom Bar.
- Common UI state that does not depend on Android-only APIs.

Platform AR implementations and Catalog feature are injected into the shared app through `arContent` and `catalogContent` slots.

Android and iOS entry points must both start from `ArPlitkaSharedApp`. Platform modules provide specific feature implementations (using Hilt on Android or manual DI on iOS), permissions, ARCore/ARKit checks, but they must not define a separate product navigation flow.
