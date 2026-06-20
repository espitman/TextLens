import SwiftUI

struct SettingsView: View {
    @ObservedObject var settingsStore: SettingsStore

    @State private var apiKey: String
    @State private var baseURL: String
    @State private var model: String
    @State private var targetLanguage: String
    @State private var validationMessage: String?

    init(settingsStore: SettingsStore) {
        self.settingsStore = settingsStore
        let settings = settingsStore.settings
        _apiKey = State(initialValue: settings.apiKey)
        _baseURL = State(initialValue: settings.baseURL.absoluteString)
        _model = State(initialValue: settings.model)
        _targetLanguage = State(initialValue: settings.targetLanguage)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("TextLens Settings")
                .font(.title3.weight(.semibold))

            Grid(alignment: .leadingFirstTextBaseline, horizontalSpacing: 12, verticalSpacing: 10) {
                GridRow {
                    Text("API Key")
                    SecureField("sk-...", text: $apiKey)
                        .textFieldStyle(.roundedBorder)
                }

                GridRow {
                    Text("Base URL")
                    TextField("https://ai.liara.ir/api/.../v1", text: $baseURL)
                        .textFieldStyle(.roundedBorder)
                }

                GridRow {
                    Text("Model")
                    TextField("openai/gpt-4.1-mini", text: $model)
                        .textFieldStyle(.roundedBorder)
                }

                GridRow {
                    Text("Target")
                    TextField("Persian", text: $targetLanguage)
                        .textFieldStyle(.roundedBorder)
                }
            }

            if let validationMessage {
                Text(validationMessage)
                    .font(.caption)
                    .foregroundStyle(.red)
            }

            HStack {
                Spacer()
                Button("Save") {
                    save()
                }
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding()
        .frame(width: 520)
    }

    private func save() {
        guard let parsedBaseURL = URL(string: baseURL.trimmingCharacters(in: .whitespacesAndNewlines)),
              parsedBaseURL.scheme != nil,
              parsedBaseURL.host != nil else {
            validationMessage = "Enter a valid Base URL."
            return
        }

        guard !model.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            validationMessage = "Enter a model name."
            return
        }

        guard !targetLanguage.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            validationMessage = "Enter a target language."
            return
        }

        settingsStore.save(
            TranslationSettings(
                apiKey: apiKey,
                baseURL: parsedBaseURL,
                model: model,
                targetLanguage: targetLanguage
            )
        )
        validationMessage = "Saved."
    }
}
