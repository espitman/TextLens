import Foundation

@MainActor
final class SettingsStore: ObservableObject {
    @Published private(set) var settings: TranslationSettings

    private let userDefaults: UserDefaults

    private enum Key {
        static let provider = "translation.provider"
        static let apiKey = "translation.apiKey"
        static let baseURL = "translation.baseURL"
        static let model = "translation.model"
        static let targetLanguage = "translation.targetLanguage"

        static func apiKey(for provider: TranslationProvider) -> String {
            "translation.\(provider.rawValue).apiKey"
        }

        static func baseURL(for provider: TranslationProvider) -> String {
            "translation.\(provider.rawValue).baseURL"
        }

        static func model(for provider: TranslationProvider) -> String {
            "translation.\(provider.rawValue).model"
        }

        static func targetLanguage(for provider: TranslationProvider) -> String {
            "translation.\(provider.rawValue).targetLanguage"
        }
    }

    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
        settings = SettingsStore.load(from: userDefaults)
    }

    func settings(for provider: TranslationProvider) -> TranslationSettings {
        SettingsStore.load(provider: provider, from: userDefaults)
    }

    func save(_ settings: TranslationSettings) {
        let sanitizedSettings = TranslationSettings(
            provider: settings.provider,
            apiKey: settings.apiKey.trimmingCharacters(in: .whitespacesAndNewlines),
            baseURL: settings.baseURL,
            model: settings.model.trimmingCharacters(in: .whitespacesAndNewlines),
            targetLanguage: settings.targetLanguage.trimmingCharacters(in: .whitespacesAndNewlines)
        )

        // TODO: Move API Key storage to Keychain before shipping beyond MVP.
        userDefaults.set(sanitizedSettings.provider.rawValue, forKey: Key.provider)
        userDefaults.set(sanitizedSettings.apiKey, forKey: Key.apiKey(for: sanitizedSettings.provider))
        userDefaults.set(sanitizedSettings.baseURL.absoluteString, forKey: Key.baseURL(for: sanitizedSettings.provider))
        userDefaults.set(sanitizedSettings.model, forKey: Key.model(for: sanitizedSettings.provider))
        userDefaults.set(sanitizedSettings.targetLanguage, forKey: Key.targetLanguage(for: sanitizedSettings.provider))

        // Keep legacy active keys in sync for older builds and simple local inspection.
        userDefaults.set(sanitizedSettings.apiKey, forKey: Key.apiKey)
        userDefaults.set(sanitizedSettings.baseURL.absoluteString, forKey: Key.baseURL)
        userDefaults.set(sanitizedSettings.model, forKey: Key.model)
        userDefaults.set(sanitizedSettings.targetLanguage, forKey: Key.targetLanguage)

        self.settings = sanitizedSettings
    }

    private static func load(from userDefaults: UserDefaults) -> TranslationSettings {
        let providerRawValue = userDefaults.string(forKey: Key.provider)
        let provider = providerRawValue.flatMap(TranslationProvider.init(rawValue:)) ?? .liara
        return load(provider: provider, from: userDefaults)
    }

    private static func load(provider: TranslationProvider, from userDefaults: UserDefaults) -> TranslationSettings {
        let defaults = TranslationSettings.defaults(for: provider)
        let canReadLegacySettings = provider == .liara

        let apiKey = userDefaults.string(forKey: Key.apiKey(for: provider))
            ?? (canReadLegacySettings ? userDefaults.string(forKey: Key.apiKey) : nil)
            ?? defaults.apiKey
        let baseURLString = userDefaults.string(forKey: Key.baseURL(for: provider))
            ?? (canReadLegacySettings ? userDefaults.string(forKey: Key.baseURL) : nil)
            ?? defaults.baseURL.absoluteString
        let baseURL = URL(string: baseURLString) ?? defaults.baseURL
        let model = userDefaults.string(forKey: Key.model(for: provider))
            ?? (canReadLegacySettings ? userDefaults.string(forKey: Key.model) : nil)
            ?? defaults.model
        let targetLanguage = userDefaults.string(forKey: Key.targetLanguage(for: provider))
            ?? (canReadLegacySettings ? userDefaults.string(forKey: Key.targetLanguage) : nil)
            ?? defaults.targetLanguage

        return TranslationSettings(
            provider: provider,
            apiKey: apiKey,
            baseURL: baseURL,
            model: model,
            targetLanguage: targetLanguage
        )
    }
}
