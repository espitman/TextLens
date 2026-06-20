import Foundation

protocol TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String
    func translate(_ text: String, settings: TranslationSettings) async throws -> String
}

final class TranslationService: TranslationServiceProtocol {
    func translateToPersian(_ text: String) async throws -> String {
        try await translate(text, settings: .defaults)
    }

    func translate(_ text: String, settings: TranslationSettings) async throws -> String {
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

            return translation
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
