import Foundation

struct TranslationSettings: Equatable {
    var apiKey: String
    var baseURL: URL
    var model: String
    var targetLanguage: String

    static let defaults = TranslationSettings(
        apiKey: "",
        baseURL: URL(string: "https://api.openai.com/v1")!,
        model: "gpt-4o-mini",
        targetLanguage: "Persian"
    )
}
