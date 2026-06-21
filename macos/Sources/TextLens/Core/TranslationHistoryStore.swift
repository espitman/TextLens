import Foundation

@MainActor
final class TranslationHistoryStore: ObservableObject {
    @Published private(set) var items: [TranslationHistoryItem]

    private let userDefaults: UserDefaults
    private let limit = 5

    private enum Key {
        static let items = "translation.history.items"
    }

    init(userDefaults: UserDefaults = .standard) {
        self.userDefaults = userDefaults
        items = Self.load(from: userDefaults)
    }

    func add(_ result: TranslationResult) {
        let item = TranslationHistoryItem(
            sourceText: result.sourceText,
            translatedText: result.translatedText,
            costToman: result.costToman
        )

        items.removeAll { $0.translatedText == item.translatedText && $0.sourceText == item.sourceText }
        items.insert(item, at: 0)
        items = Array(items.prefix(limit))
        save()
    }

    private func save() {
        guard let data = try? JSONEncoder().encode(items) else {
            return
        }

        userDefaults.set(data, forKey: Key.items)
    }

    private static func load(from userDefaults: UserDefaults) -> [TranslationHistoryItem] {
        guard let data = userDefaults.data(forKey: Key.items),
              let decoded = try? JSONDecoder().decode([TranslationHistoryItem].self, from: data) else {
            return []
        }

        return Array(decoded.prefix(5))
    }
}
