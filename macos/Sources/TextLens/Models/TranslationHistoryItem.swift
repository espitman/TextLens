import Foundation

struct TranslationHistoryItem: Codable, Equatable, Identifiable {
    var id: UUID
    var sourceText: String
    var translatedText: String
    var costToman: Int?
    var createdAt: Date

    init(
        id: UUID = UUID(),
        sourceText: String,
        translatedText: String,
        costToman: Int?,
        createdAt: Date = Date()
    ) {
        self.id = id
        self.sourceText = sourceText
        self.translatedText = translatedText
        self.costToman = costToman
        self.createdAt = createdAt
    }
}
