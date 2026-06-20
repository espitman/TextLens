import AppKit

enum TextLensError: LocalizedError {
    case hotKeyRegistrationFailed(status: OSStatus)
    case invalidSelection
    case noTextFound
    case notImplemented(String)
    case screenRecordingPermissionMissing
    case screenshotCaptureFailed
    case screenshotCropFailed

    var errorDescription: String? {
        switch self {
        case .hotKeyRegistrationFailed(let status):
            return "Could not register the Command + Shift + 0 shortcut. It may already be used by another app. OSStatus: \(status)"
        case .invalidSelection:
            return "The selected area is too small or outside the current display."
        case .noTextFound:
            return "No text found in selected area"
        case .notImplemented(let message):
            return message
        case .screenRecordingPermissionMissing:
            return "TextLens needs Screen Recording permission to read text from the selected area of your screen."
        case .screenshotCaptureFailed:
            return "TextLens could not capture the selected screen area."
        case .screenshotCropFailed:
            return "TextLens could not crop the selected screen area."
        }
    }
}

final class ErrorPresenter {
    func present(_ error: Error) {
        let alert = NSAlert(error: error)
        alert.runModal()
    }

    func presentMessage(title: String, message: String) {
        let alert = NSAlert()
        alert.messageText = title
        alert.informativeText = message
        alert.alertStyle = .informational
        alert.addButton(withTitle: "OK")
        alert.runModal()
    }
}
