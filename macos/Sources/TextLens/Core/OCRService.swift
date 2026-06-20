import CoreGraphics
import Foundation
import Vision

final class OCRService {
    func recognizeText(from image: CGImage) async throws -> String {
        try await withCheckedThrowingContinuation { continuation in
            let request = VNRecognizeTextRequest { request, error in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }

                let recognizedText = (request.results as? [VNRecognizedTextObservation] ?? [])
                    .compactMap { observation in
                        observation.topCandidates(1).first?.string
                    }
                    .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                    .filter { !$0.isEmpty }
                    .joined(separator: "\n")

                guard !recognizedText.isEmpty else {
                    continuation.resume(throwing: TextLensError.noTextFound)
                    return
                }

                continuation.resume(returning: recognizedText)
            }

            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = true
            request.recognitionLanguages = ["en-US"]

            let handler = VNImageRequestHandler(cgImage: image, options: [:])

            do {
                try handler.perform([request])
            } catch {
                continuation.resume(throwing: error)
            }
        }
    }
}
