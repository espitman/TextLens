import AppKit
import SwiftUI

final class SettingsPopoverController {
    private let popover = NSPopover()
    private weak var statusItem: NSStatusItem?

    init(
        settingsStore: SettingsStore,
        historyStore: TranslationHistoryStore,
        onTranslateArea: @escaping () -> Void,
        onOpenHistoryItem: @escaping (TranslationHistoryItem) -> Void,
        onQuit: @escaping () -> Void
    ) {
        popover.behavior = .transient
        popover.animates = true
        popover.contentSize = NSSize(width: 420, height: 720)
        popover.contentViewController = NSHostingController(
            rootView: SettingsView(
                settingsStore: settingsStore,
                historyStore: historyStore,
                onTranslateArea: onTranslateArea,
                onOpenHistoryItem: { [popover] item in
                    popover.performClose(nil)
                    onOpenHistoryItem(item)
                },
                onCloseSettings: { [popover] in
                    popover.performClose(nil)
                },
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
