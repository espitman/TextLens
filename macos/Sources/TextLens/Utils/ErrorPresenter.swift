import AppKit

enum TextLensError: LocalizedError {
    case hotKeyRegistrationFailed(status: OSStatus)
    case invalidSelection
    case missingAPIKey
    case noTextFound
    case notImplemented(String)
    case screenRecordingPermissionMissing
    case screenshotCaptureFailed
    case screenshotCropFailed
    case translationHTTPError(statusCode: Int, message: String?)
    case translationMalformedResponse
    case translationNetworkFailed

    var errorDescription: String? {
        switch self {
        case .hotKeyRegistrationFailed(let status):
            return "Could not register the Command + Shift + 0 shortcut. It may already be used by another app. OSStatus: \(status)"
        case .invalidSelection:
            return "The selected area is too small or outside the current display."
        case .missingAPIKey:
            return "Add the Liara/OpenAI-compatible API key in Settings."
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
        case .translationHTTPError(let statusCode, let message):
            if let message, !message.isEmpty {
                return "Translation API returned \(statusCode): \(message)"
            }

            return "Translation API returned \(statusCode)."
        case .translationMalformedResponse:
            return "TextLens could not read the translation response."
        case .translationNetworkFailed:
            return "TextLens could not reach the translation API."
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
