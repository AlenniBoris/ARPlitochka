import SwiftUI
import ARPlitkaIos

struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
