import CoreGraphics
import Foundation

final class ScreenshotService {
    func capture(rect: CGRect, displayID: CGDirectDisplayID) throws -> CGImage {
        // TODO: Capture and crop the selected display area in Phase 4.
        throw TextLensError.notImplemented("Screenshot capture is not implemented yet.")
    }
}
