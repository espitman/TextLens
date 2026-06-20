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
            defer: false,
            screen: screen
        )

        let overlayView = SelectionOverlayView { [weak self] selectionRect in
            guard let self else {
                return
            }

            if let selectionRect {
                self.completion(
                    ScreenSelection(
                        rect: self.convertToScreen(selectionRect),
                        displayID: self.screenDisplayID
                    )
                )
            } else {
                self.completion(nil)
            }

            self.close()
        }

        contentView = overlayView
        backgroundColor = .clear
        isOpaque = false
        hasShadow = false
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
