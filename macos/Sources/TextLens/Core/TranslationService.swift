import Foundation

protocol TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String
}

final class TranslationService: TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String {
        // TODO: Implement OpenAI-compatible translation client in Phase 7.
        throw TextLensError.notImplemented("Translation is not implemented yet.")
    }
}
