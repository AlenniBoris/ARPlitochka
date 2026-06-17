# Project Context Prompt for AI Assistants

Copy and paste the content below into a new chat to provide the AI with full context of the project.

---

# Context: Mobile AR Application "AR Plitka" (Kotlin Multiplatform)

**Role:** You are an expert AR developer (iOS/ARKit/SceneKit & Android/ARCore).

**Project Goal:** A cross-platform app for measuring floor areas and visualizing tile placement using AR.

**Current Tech Stack:**
- **Shared:** Kotlin Multiplatform (KMP), Compose Multiplatform.
- **iOS:** Kotlin/Native, ARKit, SceneKit, custom C++ bridge for geometry.
- **Android:** ARCore, SceneView.

**Current Project State (Overall Completion: ~65%)**

### 1. What is implemented:
- **Core Engine:** Shared logic for point placement, contour calculation, and tile UV-mapping.
- **Android:** Fully functional and stable (Reference implementation).
- **iOS Infrastructure:** 
    - Custom AR Session Coordinator and Renderers.
    - Native C++ bridge for high-performance geometry creation.
    - "Hybrid Stability Model": World-locked points during placement + Anchor-relative (Dead Grip) for finalized tiles.
    - One Euro Filter for smoothing ARKit noise.
    - Asynchronous geometry building (Stage 1) to offload Main Thread.
- **UI:** Shared TopBar, ActionButtons, and StatusPanels.

### 2. Current Status & Known Issues:
- **iOS Stability:** We just finished a deep optimization cycle. The app is stable for small/medium zones (< 20m²).
- **Large Zone Limitation:** On iOS, very large zones (> 20-30m²) or complex contours (> 15 points) still experience "jumps" due to ARKit world-map re-localization. We have documented this in `docs/ar/LIMITATIONS_LARGE_ZONES.md` and decided to move forward for now.
- **Performance:** Most heavy lifting (mesh generation) is moved to background threads, but UI freezes can still occur if actions are performed too rapidly.

### 3. Git State:
- The `feature/ios_ar_screen` branch has been squashed and merged into `main`.
- The last stable point is a single consolidated commit covering all iOS AR stability strategies.

### 4. Next Steps:
- We are moving past the "iOS Stability" phase.
- We need to focus on [INSERT NEXT TASK HERE].

**Instructions for the AI:**
- Use the existing architecture in `iosApp/src/iosMain/kotlin/com/example/arplitka/iosapp/platform/ar/`.
- Maintain the "Hybrid Stability" logic.
- Always prefer shared logic in `shared/ar/domain/` over platform-specific hacks.
- When working on iOS, remember that SceneKit calls must be on the Main Thread, but geometry calculation should be async.
