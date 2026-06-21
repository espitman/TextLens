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
        request.setValue(authorizationHeaderValue(for: settings), forHTTPHeaderField: "Authorization")
        if settings.provider == .openRouter {
            request.setValue("TextLens", forHTTPHeaderField: "X-OpenRouter-Title")
        }
        request.timeoutInterval = 180
        let maxOutputTokens = maxOutputTokens(for: trimmedText, provider: settings.provider)
        request.httpBody = try JSONEncoder().encode(
            ChatCompletionRequest(
                model: settings.model,
                maxTokens: maxOutputTokens,
                maxCompletionTokens: settings.provider == .openRouter ? maxOutputTokens : nil,
                temperature: 0.2,
                messages: [
                    .init(role: "system", content: systemPrompt(targetLanguage: settings.targetLanguage)),
                    .init(role: "user", content: trimmedText),
                ]
            )
        )

        let data: Data
        let statusCode: Int
        do {
            (data, statusCode) = try await perform(request)
        } catch {
            throw TextLensError.translationNetworkFailed(networkErrorMessage(from: error))
        }

        guard (200..<300).contains(statusCode) else {
            let apiMessage = decodeAPIError(from: data)
            throw TextLensError.translationHTTPError(statusCode: statusCode, message: apiMessage)
        }

        do {
            let decoded = try JSONDecoder().decode(ChatCompletionResponse.self, from: data)
            let firstChoice = decoded.choices.first
            guard let content = firstChoice?.message.content else {
                if firstChoice?.finishReason == "length" || firstChoice?.nativeFinishReason?.contains("max") == true {
                    throw TextLensError.translationHTTPError(
                        statusCode: 200,
                        message: "The model hit its output limit before returning a final translation."
                    )
                }
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

    private func maxOutputTokens(for text: String, provider: TranslationProvider) -> Int {
        let estimatedTokens = max(1600, Int(Double(text.count) * 1.4))
        let cappedTokens = min(estimatedTokens, provider == .openRouter ? 8000 : 12000)
        return cappedTokens
    }

    private static let session: URLSession = {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 180
        configuration.timeoutIntervalForResource = 240
        configuration.waitsForConnectivity = true
        return URLSession(configuration: configuration)
    }()

    private func perform(_ request: URLRequest) async throws -> (Data, Int) {
        do {
            let (data, response) = try await Self.session.data(for: request)
            guard let httpResponse = response as? HTTPURLResponse else {
                throw TextLensError.translationMalformedResponse
            }
            return (data, httpResponse.statusCode)
        } catch {
            return try await performWithCurl(request)
        }
    }

    private func performWithCurl(_ request: URLRequest) async throws -> (Data, Int) {
        guard let url = request.url,
              let body = request.httpBody else {
            throw TextLensError.translationMalformedResponse
        }

        return try await withCheckedThrowingContinuation { continuation in
            let process = Process()
            let configPipe = Pipe()
            let outputPipe = Pipe()
            let errorPipe = Pipe()
            let bodyURL = FileManager.default.temporaryDirectory
                .appendingPathComponent("textlens-request-\(UUID().uuidString).json")

            do {
                try body.write(to: bodyURL, options: .atomic)
            } catch {
                continuation.resume(throwing: error)
                return
            }

            process.executableURL = URL(fileURLWithPath: "/usr/bin/curl")
            process.arguments = ["-sS", "-w", "\n%{http_code}", "-K", "-"]
            process.standardInput = configPipe
            process.standardOutput = outputPipe
            process.standardError = errorPipe

            let headers = request.allHTTPHeaderFields ?? [:]
            var configLines = [
                "url = \"\(url.absoluteString)\"",
                "request = \"\(request.httpMethod ?? "POST")\"",
                "data = @\(bodyURL.path)",
                "connect-timeout = 30",
                "max-time = 240",
            ]
            for (key, value) in headers {
                configLines.append("header = \"\(key): \(value)\"")
            }
            configLines.append("")

            process.terminationHandler = { process in
                defer {
                    try? FileManager.default.removeItem(at: bodyURL)
                }

                let output = outputPipe.fileHandleForReading.readDataToEndOfFile()
                let errorOutput = errorPipe.fileHandleForReading.readDataToEndOfFile()

                guard process.terminationStatus == 0 else {
                    let message = String(data: errorOutput, encoding: .utf8)?
                        .trimmingCharacters(in: .whitespacesAndNewlines)
                    continuation.resume(throwing: TextLensError.translationNetworkFailed(message))
                    return
                }

                guard let separatorRange = output.range(of: Data("\n".utf8), options: .backwards) else {
                    continuation.resume(throwing: TextLensError.translationMalformedResponse)
                    return
                }

                let responseData = output.subdata(in: output.startIndex..<separatorRange.lowerBound)
                let statusData = output.subdata(in: separatorRange.upperBound..<output.endIndex)
                let statusString = String(data: statusData, encoding: .utf8)?
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                guard let statusCode = statusString.flatMap(Int.init) else {
                    continuation.resume(throwing: TextLensError.translationMalformedResponse)
                    return
                }

                continuation.resume(returning: (responseData, statusCode))
            }

            do {
                try process.run()
                configPipe.fileHandleForWriting.write(Data(configLines.joined(separator: "\n").utf8))
                try configPipe.fileHandleForWriting.close()
            } catch {
                try? FileManager.default.removeItem(at: bodyURL)
                continuation.resume(throwing: error)
            }
        }
    }

    private func networkErrorMessage(from error: Error) -> String {
        let nsError = error as NSError
        if nsError.domain == NSURLErrorDomain {
            switch nsError.code {
            case NSURLErrorTimedOut:
                return "The request timed out."
            case NSURLErrorNotConnectedToInternet:
                return "No internet connection."
            case NSURLErrorCannotFindHost:
                return "Could not find the API host."
            case NSURLErrorCannotConnectToHost:
                return "Could not connect to the API host."
            case NSURLErrorNetworkConnectionLost:
                return "The network connection was lost."
            default:
                break
            }
        }

        return error.localizedDescription
    }

    private func authorizationHeaderValue(for settings: TranslationSettings) -> String {
        let apiKey = settings.apiKey.trimmingCharacters(in: .whitespacesAndNewlines)

        switch settings.provider {
        case .liara, .openRouter:
            if apiKey.range(of: "Bearer ", options: [.anchored, .caseInsensitive]) != nil {
                return apiKey
            }

            return "Bearer \(apiKey)"
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
        Translate the entire text completely and preserve every paragraph.
        Keep the meaning accurate and natural.
        Do not summarize, shorten, omit, or add commentary.
        Do not add explanations.
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
    var maxTokens: Int
    var maxCompletionTokens: Int?
    var temperature: Double
    var messages: [ChatMessage]

    private enum CodingKeys: String, CodingKey {
        case model
        case maxTokens = "max_tokens"
        case maxCompletionTokens = "max_completion_tokens"
        case temperature
        case messages
    }
}

private struct ChatMessage: Codable {
    var role: String
    var content: String?
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
        var finishReason: String?
        var nativeFinishReason: String?

        private enum CodingKeys: String, CodingKey {
            case message
            case finishReason = "finish_reason"
            case nativeFinishReason = "native_finish_reason"
        }
    }
}

private struct APIErrorResponse: Decodable {
    var error: APIError

    struct APIError: Decodable {
        var message: String
    }
}
