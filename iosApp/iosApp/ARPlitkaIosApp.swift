import SwiftUI
import ARPlitkaIos

@main
struct ARPlitkaIosApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeRootView()
                .ignoresSafeArea(.all)
        }
    }
}
