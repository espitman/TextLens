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
            button.image = Self.menuBarIcon()
            button.imagePosition = .imageOnly
            button.title = ""
            button.toolTip = "TextLens"
            button.action = #selector(openSettings)
            button.target = self
        }
    }

    func attachSettingsPopover(_ popoverController: SettingsPopoverController) {
        popoverController.attach(to: statusItem)
    }

    private static func menuBarIcon() -> NSImage {
        let image = NSImage(size: NSSize(width: 18, height: 18))
        image.lockFocus()

        NSColor.labelColor.set()
        let paragraphStyle = NSMutableParagraphStyle()
        paragraphStyle.alignment = .center
        let attributes: [NSAttributedString.Key: Any] = [
            .font: NSFont.systemFont(ofSize: 15, weight: .bold),
            .foregroundColor: NSColor.labelColor,
            .paragraphStyle: paragraphStyle,
        ]
        NSString(string: "T").draw(
            in: NSRect(x: 0, y: 0, width: 18, height: 17),
            withAttributes: attributes
        )

        image.unlockFocus()
        image.isTemplate = true
        image.accessibilityDescription = "TextLens"
        return image
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
