import CoreGraphics
import Foundation

final class ScreenshotService {
    func capture(rect: CGRect, displayID: CGDirectDisplayID) throws -> CGImage {
        guard rect.isMeaningfulSelection else {
            throw TextLensError.invalidSelection
        }

        guard let displayImage = CGDisplayCreateImage(displayID) else {
            throw TextLensError.screenshotCaptureFailed
        }

        let displayBounds = CGDisplayBounds(displayID)
        let pixelWidth = CGFloat(displayImage.width)
        let pixelHeight = CGFloat(displayImage.height)
        let scaleX = pixelWidth / displayBounds.width
        let scaleY = pixelHeight / displayBounds.height
        let rectInDisplayPoints = rect.offsetBy(dx: -displayBounds.minX, dy: -displayBounds.minY)
        let cropRect = CGRect(
            x: rectInDisplayPoints.minX * scaleX,
            y: (displayBounds.height - rectInDisplayPoints.maxY) * scaleY,
            width: rectInDisplayPoints.width * scaleX,
            height: rectInDisplayPoints.height * scaleY
        )
        .integral
        .intersection(CGRect(x: 0, y: 0, width: pixelWidth, height: pixelHeight))

        guard cropRect.width > 0, cropRect.height > 0 else {
            throw TextLensError.invalidSelection
        }

        guard let croppedImage = displayImage.cropping(to: cropRect) else {
            throw TextLensError.screenshotCropFailed
        }

        return croppedImage
    }
}
