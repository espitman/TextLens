import AppKit

enum TextLensError: LocalizedError {
    case notImplemented(String)

    var errorDescription: String? {
        switch self {
        case .notImplemented(let message):
            return message
        }
    }
}

final class ErrorPresenter {
    func present(_ error: Error) {
        let alert = NSAlert(error: error)
        alert.runModal()
    }
}
