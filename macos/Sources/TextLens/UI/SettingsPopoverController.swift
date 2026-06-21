import AppKit
import SwiftUI

final class SettingsPopoverController {
    private let popover = NSPopover()
    private weak var statusItem: NSStatusItem?

    init(
        settingsStore: SettingsStore,
        onTranslateArea: @escaping () -> Void,
        onQuit: @escaping () -> Void
    ) {
        popover.behavior = .transient
        popover.animates = true
        popover.contentSize = NSSize(width: 420, height: 640)
        popover.contentViewController = NSHostingController(
            rootView: SettingsView(
                settingsStore: settingsStore,
                onTranslateArea: onTranslateArea,
                onQuit: onQuit
            )
        )
    }

    func attach(to statusItem: NSStatusItem) {
        self.statusItem = statusItem
    }

    func toggle() {
        if popover.isShown {
            popover.performClose(nil)
        } else {
            show()
        }
    }

    func show() {
        guard let button = statusItem?.button else {
            return
        }

        popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        popover.contentViewController?.view.window?.makeKey()
    }
}
