//
//  ComposeRootView.swift
//  ARPlitkaIos
//
//  Created by Аленников Борис Сергеевич on 28.05.26.
//

import Foundation
import SwiftUI
import ARPlitkaIos
struct ComposeRootView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
