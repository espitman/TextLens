import Foundation

struct TranslationSettings: Equatable {
    var apiKey: String
    var baseURL: URL
    var model: String
    var targetLanguage: String

    var hasAPIKey: Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    static let defaults = TranslationSettings(
        apiKey: "",
        baseURL: URL(string: "https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1")!,
        model: "openai/gpt-4.1-mini",
        targetLanguage: "Persian"
    )
}
