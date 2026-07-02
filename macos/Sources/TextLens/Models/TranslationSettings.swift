import Foundation

enum TranslationProvider: String, CaseIterable, Identifiable {
    case liara
    case openRouter
    case local

    var id: String {
        rawValue
    }

    var title: String {
        switch self {
        case .liara:
            "Liara"
        case .openRouter:
            "OpenRouter"
        case .local:
            "Local"
        }
    }

    var apiKeyPlaceholder: String {
        requiresAPIKey ? "\(title) API key" : "Optional API key"
    }

    var requiresAPIKey: Bool {
        switch self {
        case .liara, .openRouter:
            true
        case .local:
            false
        }
    }

    var defaultBaseURL: URL {
        switch self {
        case .liara:
            URL(string: "https://ai.liara.ir/api/6a0ccd2d298429714a4b3e25/v1")!
        case .openRouter:
            URL(string: "https://openrouter.ai/api/v1")!
        case .local:
            URL(string: "http://127.0.0.1:8088/v1")!
        }
    }

    var defaultModel: String {
        modelCatalog.defaultModel
    }

    var modelCatalog: TranslationModelCatalog {
        switch self {
        case .liara:
            TranslationModelCatalog(
                defaultModel: "openai/gpt-5-nano",
                placeholder: "Choose or type a Liara model",
                options: [
                    "openai/gpt-5-nano",
                    "openai/gpt-4.1-mini",
                    "google/gemma-3-27b-it",
                    "google/gemini-2.0-flash-lite-001",
                    "google/gemini-2.5-flash-lite",
                    "google/gemini-3.1-flash-lite",
                    "google/gemini-2.5-flash",
                ]
            )
        case .openRouter:
            TranslationModelCatalog(
                defaultModel: "google/gemma-4-31b-it:free",
                placeholder: "Choose an OpenRouter model",
                options: [
                    "google/gemma-4-31b-it:free",
                    "openai/gpt-4.1-mini",
                    "google/gemini-2.0-flash-lite-001",
                    "anthropic/claude-3.5-sonnet",
                ]
            )
        case .local:
            TranslationModelCatalog(
                defaultModel: "local-model",
                placeholder: "Type a local model id",
                options: [
                    "local-model",
                    "llama3.2",
                    "qwen2.5",
                    "mistral",
                ]
            )
        }
    }

    static let settingsDisplayOrder: [TranslationProvider] = [.openRouter, .liara, .local]
}

struct TranslationModelCatalog: Equatable {
    var defaultModel: String
    var placeholder: String
    var options: [String]
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

    var isReadyToTranslate: Bool {
        provider.requiresAPIKey ? hasAPIKey : true
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
