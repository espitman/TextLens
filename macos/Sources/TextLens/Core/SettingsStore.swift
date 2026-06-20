import Foundation

@MainActor
final class SettingsStore: ObservableObject {
    @Published private(set) var settings: TranslationSettings

    private let userDefaults: UserDefaults

    private enum Key {
        static let apiKey = "translation.apiKey"
        static let baseURL = "translation.baseURL"
        static let model = "translation.model"
        static let targetLanguage = "translation.targetLanguage"
    }

    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
        settings = SettingsStore.load(from: userDefaults)
    }

    func save(_ settings: TranslationSettings) {
        let sanitizedSettings = TranslationSettings(
            apiKey: settings.apiKey.trimmingCharacters(in: .whitespacesAndNewlines),
            baseURL: settings.baseURL,
            model: settings.model.trimmingCharacters(in: .whitespacesAndNewlines),
            targetLanguage: settings.targetLanguage.trimmingCharacters(in: .whitespacesAndNewlines)
        )

        // TODO: Move API Key storage to Keychain before shipping beyond MVP.
        userDefaults.set(sanitizedSettings.apiKey, forKey: Key.apiKey)
        userDefaults.set(sanitizedSettings.baseURL.absoluteString, forKey: Key.baseURL)
        userDefaults.set(sanitizedSettings.model, forKey: Key.model)
        userDefaults.set(sanitizedSettings.targetLanguage, forKey: Key.targetLanguage)

        self.settings = sanitizedSettings
    }

    private static func load(from userDefaults: UserDefaults) -> TranslationSettings {
        let defaults = TranslationSettings.defaults

        let apiKey = userDefaults.string(forKey: Key.apiKey) ?? defaults.apiKey
        let baseURLString = userDefaults.string(forKey: Key.baseURL) ?? defaults.baseURL.absoluteString
        let baseURL = URL(string: baseURLString) ?? defaults.baseURL
        let model = userDefaults.string(forKey: Key.model) ?? defaults.model
        let targetLanguage = userDefaults.string(forKey: Key.targetLanguage) ?? defaults.targetLanguage

        return TranslationSettings(
            apiKey: apiKey,
            baseURL: baseURL,
            model: model,
            targetLanguage: targetLanguage
        )
    }
}
