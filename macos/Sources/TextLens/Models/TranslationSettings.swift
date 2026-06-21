import Foundation

enum TranslationProvider: String, CaseIterable, Identifiable {
    case liara
    case openRouter

    var id: String {
        rawValue
    }

    var title: String {
        switch self {
        case .liara:
            "Liara"
        case .openRouter:
            "OpenRouter"
        }
    }

    var apiKeyPlaceholder: String {
        "\(title) API key"
    }

    var defaultBaseURL: URL {
        switch self {
        case .liara:
            URL(string: "https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1")!
        case .openRouter:
            URL(string: "https://openrouter.ai/api/v1")!
        }
    }

    var defaultModel: String {
        switch self {
        case .liara:
            "openai/gpt-4.1-mini"
        case .openRouter:
            "~openai/gpt-latest"
        }
    }
}

struct TranslationSettings: Equatable {
    var provider: TranslationProvider
    var apiKey: String
    var baseURL: URL
    var model: String
    var targetLanguage: String

    var hasAPIKey: Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    static let defaults = defaults(for: .liara)

    static func defaults(for provider: TranslationProvider) -> TranslationSettings {
        TranslationSettings(
            provider: provider,
            apiKey: "",
            baseURL: provider.defaultBaseURL,
            model: provider.defaultModel,
            targetLanguage: "Persian"
        )
    }
}
