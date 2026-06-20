import AppKit

final class SelectionOverlayWindow: NSWindow {
    typealias Completion = (ScreenSelection?) -> Void

    private let completion: Completion
    private let screenDisplayID: CGDirectDisplayID

    init(screen: NSScreen = NSScreen.main ?? NSScreen.screens[0], completion: @escaping Completion) {
        self.completion = completion
        self.screenDisplayID = screen.displayID

        super.init(
            contentRect: screen.frame,
            styleMask: [.borderless],
            backing: .buffered,
            defer: false
        )

        let overlayView = SelectionOverlayView { [weak self] selectionRect in
            guard let self else {
                return
            }

            let result: ScreenSelection?
            if let selectionRect {
                result = ScreenSelection(
                    rect: self.convertToScreen(selectionRect),
                    displayID: self.screenDisplayID
                )
            } else {
                result = nil
            }

            let completion = self.completion
            self.orderOut(nil)
            completion(result)
        }

        contentView = overlayView
        backgroundColor = .clear
        isOpaque = false
        hasShadow = false
        isReleasedWhenClosed = false
        ignoresMouseEvents = false
        level = .screenSaver
        collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        acceptsMouseMovedEvents = true
    }

    override var canBecomeKey: Bool {
        true
    }

    override var canBecomeMain: Bool {
        true
    }

    func show() {
        makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }
}

private extension NSScreen {
    var displayID: CGDirectDisplayID {
        if let screenNumber = deviceDescription[NSDeviceDescriptionKey("NSScreenNumber")] as? NSNumber {
            return CGDirectDisplayID(screenNumber.uint32Value)
        }

        return CGMainDisplayID()
    }
}
