import AppKit
import SwiftUI

final class SettingsWindowController: NSWindowController {
    init(settingsStore: SettingsStore) {
        let hostingController = NSHostingController(rootView: SettingsView(settingsStore: settingsStore))
        let window = NSWindow(contentViewController: hostingController)
        window.title = "TextLens Settings"
        window.styleMask = [.titled, .closable, .miniaturizable]
        window.isReleasedWhenClosed = false
        window.setContentSize(NSSize(width: 520, height: 260))
        window.center()

        super.init(window: window)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        nil
    }

    func show() {
        window?.center()
        showWindow(nil)
        NSApp.activate(ignoringOtherApps: true)
        window?.makeKeyAndOrderFront(nil)
    }
}
