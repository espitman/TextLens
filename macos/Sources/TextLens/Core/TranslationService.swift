import Foundation

protocol TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String
    func translate(_ text: String, settings: TranslationSettings) async throws -> TranslationResult
}

final class TranslationService: TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String {
        try await translate(text, settings: .defaults).translatedText
    }

    func translate(_ text: String, settings: TranslationSettings) async throws -> TranslationResult {
        guard settings.hasAPIKey else {
            throw TextLensError.missingAPIKey
        }

        let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedText.isEmpty else {
            throw TextLensError.noTextFound
        }

        var request = URLRequest(url: chatCompletionsURL(from: settings.baseURL))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Bearer \(settings.apiKey)", forHTTPHeaderField: "Authorization")
        if settings.provider == .openRouter {
            request.setValue("TextLens", forHTTPHeaderField: "X-OpenRouter-Title")
        }
        request.timeoutInterval = 60
        request.httpBody = try JSONEncoder().encode(
            ChatCompletionRequest(
                model: settings.model,
                temperature: 0.2,
                messages: [
                    .init(role: "system", content: systemPrompt(targetLanguage: settings.targetLanguage)),
                    .init(role: "user", content: trimmedText),
                ]
            )
        )

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch {
            throw TextLensError.translationNetworkFailed
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw TextLensError.translationMalformedResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            let apiMessage = decodeAPIError(from: data)
            throw TextLensError.translationHTTPError(statusCode: httpResponse.statusCode, message: apiMessage)
        }

        do {
            let decoded = try JSONDecoder().decode(ChatCompletionResponse.self, from: data)
            guard let content = decoded.choices.first?.message.content else {
                throw TextLensError.translationMalformedResponse
            }

            let translation = cleanTranslation(content)
            guard !translation.isEmpty else {
                throw TextLensError.translationMalformedResponse
            }

            return TranslationResult(
                sourceText: trimmedText,
                translatedText: translation,
                costToman: decoded.costToman ?? extractCostToman(from: data)
            )
        } catch let error as TextLensError {
            throw error
        } catch {
            throw TextLensError.translationMalformedResponse
        }
    }

    private func chatCompletionsURL(from baseURL: URL) -> URL {
        let normalized = baseURL.absoluteString.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: #"/+$"#, with: "", options: .regularExpression)
        if normalized.hasSuffix("/chat/completions") {
            return URL(string: normalized) ?? baseURL
        }

        return URL(string: "\(normalized)/chat/completions") ?? baseURL
    }

    private func systemPrompt(targetLanguage: String) -> String {
        """
        Translate the following text to \(targetLanguage).
        Keep the meaning accurate and natural.
        Do not add explanations.
        If the text contains UI labels, keep the translation concise.
        Return only the translation.
        """
    }

    private func cleanTranslation(_ raw: String) -> String {
        raw
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: #"^```(?:text|txt|json)?\s*"#, with: "", options: .regularExpression)
            .replacingOccurrences(of: #"\s*```$"#, with: "", options: .regularExpression)
            .replacingOccurrences(of: #"^(translation|translated text|persian)\s*:\s*"#, with: "", options: [.regularExpression, .caseInsensitive])
            .trimmingCharacters(in: .whitespacesAndNewlines.union(CharacterSet(charactersIn: "\"'")))
    }

    private func decodeAPIError(from data: Data) -> String? {
        if let decoded = try? JSONDecoder().decode(APIErrorResponse.self, from: data) {
            return decoded.error.message
        }

        return String(data: data, encoding: .utf8)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .prefix(240)
            .description
    }

    private func extractCostToman(from data: Data) -> Int? {
        guard let object = try? JSONSerialization.jsonObject(with: data) else {
            return nil
        }

        let preferredKeys = [
            "total_cost_toman",
            "cost_toman",
            "total_price_toman",
            "price_toman",
            "total_cost",
            "cost",
            "total_price",
            "price",
        ]

        for key in preferredKeys {
            if let value = findNumericValue(for: key, in: object) {
                return Int(value.rounded())
            }
        }

        return nil
    }

    private func findNumericValue(for key: String, in value: Any) -> Double? {
        if let dictionary = value as? [String: Any] {
            for (candidateKey, candidateValue) in dictionary where candidateKey == key {
                if let number = candidateValue as? NSNumber {
                    return number.doubleValue
                }

                if let string = candidateValue as? String, let number = Double(string) {
                    return number
                }
            }

            for nestedValue in dictionary.values {
                if let found = findNumericValue(for: key, in: nestedValue) {
                    return found
                }
            }
        }

        if let array = value as? [Any] {
            for item in array {
                if let found = findNumericValue(for: key, in: item) {
                    return found
                }
            }
        }

        return nil
    }
}

private struct ChatCompletionRequest: Encodable {
    var model: String
    var temperature: Double
    var messages: [ChatMessage]
}

private struct ChatMessage: Codable {
    var role: String
    var content: String
}

private struct ChatCompletionResponse: Decodable {
    var choices: [Choice]
    var costToman: Int?

    private enum CodingKeys: String, CodingKey {
        case choices
        case costToman = "cost_toman"
        case totalCostToman = "total_cost_toman"
        case priceToman = "price_toman"
        case totalPriceToman = "total_price_toman"
        case cost
        case totalCost = "total_cost"
        case price
        case totalPrice = "total_price"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        choices = try container.decode([Choice].self, forKey: .choices)

        let directCost = try container.decodeIfPresent(Double.self, forKey: .costToman)
        let totalCostToman = try container.decodeIfPresent(Double.self, forKey: .totalCostToman)
        let priceToman = try container.decodeIfPresent(Double.self, forKey: .priceToman)
        let totalPriceToman = try container.decodeIfPresent(Double.self, forKey: .totalPriceToman)
        let cost = try container.decodeIfPresent(Double.self, forKey: .cost)
        let totalCost = try container.decodeIfPresent(Double.self, forKey: .totalCost)
        let price = try container.decodeIfPresent(Double.self, forKey: .price)
        let totalPrice = try container.decodeIfPresent(Double.self, forKey: .totalPrice)

        let firstCost = directCost ?? totalCostToman ?? priceToman ?? totalPriceToman ?? cost ?? totalCost ?? price ?? totalPrice
        costToman = firstCost.map { Int($0.rounded()) }
    }

    struct Choice: Decodable {
        var message: ChatMessage
    }
}

private struct APIErrorResponse: Decodable {
    var error: APIError

    struct APIError: Decodable {
        var message: String
    }
}
