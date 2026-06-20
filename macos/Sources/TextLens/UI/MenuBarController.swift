import AppKit

final class MenuBarController: NSObject {
    private let statusItem: NSStatusItem
    private let onTranslateArea: () -> Void
    private let onOpenSettings: () -> Void
    private let onQuit: () -> Void

    init(
        onTranslateArea: @escaping () -> Void,
        onOpenSettings: @escaping () -> Void,
        onQuit: @escaping () -> Void
    ) {
        statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
        self.onTranslateArea = onTranslateArea
        self.onOpenSettings = onOpenSettings
        self.onQuit = onQuit
        super.init()
        configureStatusItem()
    }

    private func configureStatusItem() {
        if let button = statusItem.button {
            button.image = NSImage(systemSymbolName: "text.viewfinder", accessibilityDescription: "TextLens")
            button.imagePosition = .imageLeading
            button.title = "TextLens"
            button.toolTip = "TextLens"
        }

        let menu = NSMenu()
        menu.addItem(menuItem(title: "Translate Area", action: #selector(translateArea), keyEquivalent: "t", modifiers: [.command, .shift]))
        menu.addItem(menuItem(title: "Settings", action: #selector(openSettings), keyEquivalent: ",", modifiers: [.command]))
        menu.addItem(.separator())
        menu.addItem(menuItem(title: "Quit", action: #selector(quit), keyEquivalent: "q", modifiers: [.command]))

        statusItem.menu = menu
    }

    private func menuItem(
        title: String,
        action: Selector,
        keyEquivalent: String,
        modifiers: NSEvent.ModifierFlags
    ) -> NSMenuItem {
        let item = NSMenuItem(title: title, action: action, keyEquivalent: keyEquivalent)
        item.keyEquivalentModifierMask = modifiers
        item.target = self
        return item
    }

    @objc private func translateArea() {
        onTranslateArea()
    }

    @objc private func openSettings() {
        onOpenSettings()
    }

    @objc private func quit() {
        onQuit()
    }
}
