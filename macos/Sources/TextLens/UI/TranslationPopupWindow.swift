import AppKit
import SwiftUI

@MainActor
final class TranslationPopupWindow: NSPanel {
    let viewModel = TranslationPopupViewModel()

    private var translatedTextForCopy = ""
    private let onCancel: () -> Void

    init(near selectionRect: CGRect, onCancel: @escaping () -> Void) {
        self.onCancel = onCancel

        super.init(
            contentRect: CGRect(origin: .zero, size: TranslationPopupView.popupSize),
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )

        contentViewController = NSHostingController(
            rootView: TranslationPopupView(
                viewModel: viewModel,
                onCopy: { [weak self] in
                    self?.copyTranslation()
                },
                onCancel: { [weak self] in
                    self?.cancel()
                },
                onClose: { [weak self] in
                    self?.close()
                }
            )
        )

        isReleasedWhenClosed = false
        backgroundColor = .clear
        isOpaque = false
        hasShadow = false
        setContentSize(TranslationPopupView.popupSize)
        level = .floating
        collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .transient]
        setFrameOrigin(Self.origin(for: TranslationPopupView.popupSize, near: selectionRect))
    }

    override var canBecomeKey: Bool {
        true
    }

    override func keyDown(with event: NSEvent) {
        if event.keyCode == 53 {
            if case .loading = viewModel.state {
                cancel()
            } else {
                close()
            }
        } else {
            super.keyDown(with: event)
        }
    }

    override func resignKey() {
        super.resignKey()
        if case .loading = viewModel.state {
            return
        }
        close()
    }

    func show() {
        makeKeyAndOrderFront(nil)
    }

    func showResult(_ result: TranslationResult) {
        translatedTextForCopy = result.translatedText
        viewModel.state = .result(text: result.translatedText, costToman: result.costToman)
    }

    func showError(_ error: Error) {
        translatedTextForCopy = ""
        viewModel.state = .failed(error.localizedDescription)
    }

    private func copyTranslation() {
        guard !translatedTextForCopy.isEmpty else {
            return
        }

        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(translatedTextForCopy, forType: .string)
    }

    private func cancel() {
        onCancel()
        close()
    }

    private static func origin(for popupSize: CGSize, near selectionRect: CGRect) -> CGPoint {
        let visibleFrame = NSScreen.screens
            .first { $0.frame.intersects(selectionRect) }?
            .visibleFrame ?? NSScreen.main?.visibleFrame ?? .zero
        let padding: CGFloat = 14
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
