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
        window?.invalidateCursorRects(for: self)
        SelectionOverlayCursor.cursor.set()
        DispatchQueue.main.async {
            SelectionOverlayCursor.cursor.set()
        }
    }

    override func draw(_ dirtyRect: NSRect) {
        super.draw(dirtyRect)

        guard let selectionRect else {
            return
        }

        NSColor.black.withAlphaComponent(0.16).setFill()
        selectionRect.fill()

        let outerBorder = NSBezierPath(rect: selectionRect.insetBy(dx: 0.5, dy: 0.5))
        outerBorder.lineWidth = 3
        NSColor.black.withAlphaComponent(0.45).setStroke()
        outerBorder.stroke()

        let innerBorder = NSBezierPath(rect: selectionRect.insetBy(dx: 1.5, dy: 1.5))
        innerBorder.lineWidth = 1.5
        NSColor.white.withAlphaComponent(0.96).setStroke()
        innerBorder.stroke()
    }

    override func mouseDown(with event: NSEvent) {
        SelectionOverlayCursor.cursor.set()
        let point = convert(event.locationInWindow, from: nil)
        startPoint = point
        currentPoint = point
        needsDisplay = true
    }

    override func mouseDragged(with event: NSEvent) {
        SelectionOverlayCursor.cursor.set()
        currentPoint = convert(event.locationInWindow, from: nil)
        needsDisplay = true
    }

    override func mouseMoved(with event: NSEvent) {
        SelectionOverlayCursor.cursor.set()
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
        addCursorRect(bounds, cursor: SelectionOverlayCursor.cursor)
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

private enum SelectionOverlayCursor {
    static let cursor: NSCursor = {
        let size = NSSize(width: 42, height: 28)
        let image = NSImage(size: size)

        image.lockFocus()
        defer { image.unlockFocus() }

        NSGraphicsContext.current?.imageInterpolation = .high

        let plusCenter = CGPoint(x: 9, y: 18)
        let plusPath = NSBezierPath()
        plusPath.lineWidth = 2.5
        plusPath.lineCapStyle = .round
        plusPath.move(to: CGPoint(x: plusCenter.x - 6, y: plusCenter.y))
        plusPath.line(to: CGPoint(x: plusCenter.x + 6, y: plusCenter.y))
        plusPath.move(to: CGPoint(x: plusCenter.x, y: plusCenter.y - 6))
        plusPath.line(to: CGPoint(x: plusCenter.x, y: plusCenter.y + 6))
        NSColor.black.withAlphaComponent(0.72).setStroke()
        plusPath.stroke()

        let plusHighlight = NSBezierPath()
        plusHighlight.lineWidth = 1
        plusHighlight.lineCapStyle = .round
        plusHighlight.move(to: CGPoint(x: plusCenter.x - 6, y: plusCenter.y + 1))
        plusHighlight.line(to: CGPoint(x: plusCenter.x + 6, y: plusCenter.y + 1))
        plusHighlight.move(to: CGPoint(x: plusCenter.x + 1, y: plusCenter.y - 6))
        plusHighlight.line(to: CGPoint(x: plusCenter.x + 1, y: plusCenter.y + 6))
        NSColor.white.withAlphaComponent(0.92).setStroke()
        plusHighlight.stroke()

        let badgeRect = NSRect(x: 18, y: 5, width: 20, height: 20)
        let badgePath = NSBezierPath(roundedRect: badgeRect, xRadius: 5, yRadius: 5)
        NSColor.black.withAlphaComponent(0.92).setFill()
        badgePath.fill()
        NSColor(red: 1.0, green: 0.78, blue: 0.25, alpha: 0.95).setStroke()
        badgePath.lineWidth = 1.4
        badgePath.stroke()

        let attributes: [NSAttributedString.Key: Any] = [
            .font: NSFont.systemFont(ofSize: 15, weight: .heavy),
            .foregroundColor: NSColor(red: 1.0, green: 0.82, blue: 0.28, alpha: 1.0),
        ]
        NSString(string: "T").draw(in: NSRect(x: 23.5, y: 4.8, width: 12, height: 18), withAttributes: attributes)

        return NSCursor(image: image, hotSpot: CGPoint(x: 9, y: 18))
    }()
}
