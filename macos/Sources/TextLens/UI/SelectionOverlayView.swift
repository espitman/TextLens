import AppKit

final class SelectionOverlayView: NSView {
    typealias Completion = (CGRect?) -> Void

    private let completion: Completion
    private var startPoint: CGPoint?
    private var currentPoint: CGPoint?

    init(completion: @escaping Completion) {
        self.completion = completion
        super.init(frame: .zero)
        wantsLayer = true
        layer?.backgroundColor = NSColor.clear.cgColor
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        nil
    }

    override var acceptsFirstResponder: Bool {
        true
    }

    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        window?.makeFirstResponder(self)
        NSCursor.crosshair.set()
    }

    override func draw(_ dirtyRect: NSRect) {
        super.draw(dirtyRect)

        NSColor.black.withAlphaComponent(0.42).setFill()
        bounds.fill()

        guard let selectionRect else {
            return
        }

        NSGraphicsContext.saveGraphicsState()
        NSBezierPath(rect: selectionRect).setClip()
        NSColor.clear.setFill()
        selectionRect.fill(using: .clear)
        NSGraphicsContext.restoreGraphicsState()

        NSColor.white.withAlphaComponent(0.95).setStroke()
        let border = NSBezierPath(rect: selectionRect.insetBy(dx: 0.5, dy: 0.5))
        border.lineWidth = 2
        border.stroke()

        NSColor.controlAccentColor.withAlphaComponent(0.22).setFill()
        selectionRect.fill()
    }

    override func mouseDown(with event: NSEvent) {
        let point = convert(event.locationInWindow, from: nil)
        startPoint = point
        currentPoint = point
        needsDisplay = true
    }

    override func mouseDragged(with event: NSEvent) {
        currentPoint = convert(event.locationInWindow, from: nil)
        needsDisplay = true
    }

    override func mouseUp(with event: NSEvent) {
        currentPoint = convert(event.locationInWindow, from: nil)

        guard let selectionRect, selectionRect.isMeaningfulSelection else {
            completion(nil)
            return
        }

        completion(selectionRect)
    }

    override func keyDown(with event: NSEvent) {
        if event.keyCode == 53 {
            completion(nil)
        } else {
            super.keyDown(with: event)
        }
    }

    override func resetCursorRects() {
        addCursorRect(bounds, cursor: .crosshair)
    }

    private var selectionRect: CGRect? {
        guard let startPoint, let currentPoint else {
            return nil
        }

        return CGRect(
            x: min(startPoint.x, currentPoint.x),
            y: min(startPoint.y, currentPoint.y),
            width: abs(currentPoint.x - startPoint.x),
            height: abs(currentPoint.y - startPoint.y)
        )
    }
}
