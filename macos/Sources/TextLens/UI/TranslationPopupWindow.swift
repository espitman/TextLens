import AppKit
import SwiftUI

@MainActor
final class TranslationPopupWindow: NSPanel {
    let viewModel = TranslationPopupViewModel()

    private var translatedTextForCopy = ""
    private let onCancel: () -> Void
    private let selectionRect: CGRect

    init(near selectionRect: CGRect, onCancel: @escaping () -> Void) {
        self.onCancel = onCancel
        self.selectionRect = selectionRect

        super.init(
            contentRect: CGRect(
                origin: .zero,
                size: CGSize(width: TranslationPopupView.popupWidth, height: TranslationPopupView.loadingHeight)
            ),
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )

        contentViewController = NSHostingController(
            rootView: TranslationPopupView(
                viewModel: viewModel,
                popupHeight: TranslationPopupView.loadingHeight,
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
        setContentSize(CGSize(width: TranslationPopupView.popupWidth, height: TranslationPopupView.loadingHeight))
        level = .floating
        collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary, .transient]
        setFrameOrigin(Self.origin(for: frame.size, near: selectionRect))
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
        updatePopupHeight(for: result.translatedText)
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

    private func updatePopupHeight(for text: String) {
        let targetHeight = Self.height(for: text)
        setContentSize(CGSize(width: TranslationPopupView.popupWidth, height: targetHeight))
        contentViewController = NSHostingController(
            rootView: TranslationPopupView(
                viewModel: viewModel,
                popupHeight: targetHeight,
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
        setFrameOrigin(Self.origin(for: CGSize(width: TranslationPopupView.popupWidth, height: targetHeight), near: selectionRect))
    }

    private static func height(for text: String) -> CGFloat {
        let explicitLines = CGFloat(max(1, text.split(separator: "\n", omittingEmptySubsequences: false).count))
        let estimatedWrappedLines = CGFloat(max(1, Int(ceil(Double(text.count) / 92.0))))
        let lines = max(explicitLines, estimatedWrappedLines)
        let estimatedHeight = 210 + (lines * 30)
        return min(TranslationPopupView.maxPopupHeight, max(TranslationPopupView.loadingHeight, estimatedHeight))
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
