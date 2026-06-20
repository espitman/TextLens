import AppKit
import SwiftUI

final class TranslationPopupWindow: NSPanel {
    init(translatedText: String, near selectionRect: CGRect) {
        let hostingController = NSHostingController(
            rootView: TranslationPopupView(
                translatedText: translatedText,
                onCopy: {
                    NSPasteboard.general.clearContents()
                    NSPasteboard.general.setString(translatedText, forType: .string)
                },
                onClose: {}
            )
        )

        super.init(
            contentRect: CGRect(x: 0, y: 0, width: 420, height: 220),
            styleMask: [.titled, .closable, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )

        contentViewController = hostingController
        titleVisibility = .hidden
        titlebarAppearsTransparent = true
        isReleasedWhenClosed = false
        backgroundColor = .clear
        isOpaque = false
        hasShadow = true
        level = .floating
        collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .transient]
        setFrameOrigin(Self.origin(for: frame.size, near: selectionRect))

        hostingController.rootView = TranslationPopupView(
            translatedText: translatedText,
            onCopy: {
                NSPasteboard.general.clearContents()
                NSPasteboard.general.setString(translatedText, forType: .string)
            },
            onClose: { [weak self] in
                self?.close()
            }
        )
    }

    override var canBecomeKey: Bool {
        true
    }

    override func keyDown(with event: NSEvent) {
        if event.keyCode == 53 {
            close()
        } else {
            super.keyDown(with: event)
        }
    }

    override func resignKey() {
        super.resignKey()
        close()
    }

    func show() {
        makeKeyAndOrderFront(nil)
    }

    private static func origin(for popupSize: CGSize, near selectionRect: CGRect) -> CGPoint {
        let visibleFrame = NSScreen.screens
            .first { $0.frame.intersects(selectionRect) }?
            .visibleFrame ?? NSScreen.main?.visibleFrame ?? .zero
        let padding: CGFloat = 12
        var x = selectionRect.minX
        var y = selectionRect.minY - popupSize.height - padding

        if y < visibleFrame.minY {
            y = selectionRect.maxY + padding
        }

        x = min(max(x, visibleFrame.minX + padding), visibleFrame.maxX - popupSize.width - padding)
        y = min(max(y, visibleFrame.minY + padding), visibleFrame.maxY - popupSize.height - padding)

        return CGPoint(x: x, y: y)
    }
}
