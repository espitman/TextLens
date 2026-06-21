import SwiftUI

struct SettingsView: View {
    @ObservedObject var settingsStore: SettingsStore

    let onTranslateArea: () -> Void
    let onQuit: () -> Void

    @State private var provider: TranslationProvider
    @State private var apiKey: String
    @State private var baseURL: String
    @State private var model: String
    @State private var targetLanguage: String
    @State private var validationMessage: String?

    init(
        settingsStore: SettingsStore,
        onTranslateArea: @escaping () -> Void = {},
        onQuit: @escaping () -> Void = {}
    ) {
        self.settingsStore = settingsStore
        self.onTranslateArea = onTranslateArea
        self.onQuit = onQuit
        let settings = settingsStore.settings
        _provider = State(initialValue: settings.provider)
        _apiKey = State(initialValue: settings.apiKey)
        _baseURL = State(initialValue: settings.baseURL.absoluteString)
        _model = State(initialValue: settings.model)
        _targetLanguage = State(initialValue: settings.targetLanguage)
    }

    var body: some View {
        VStack(spacing: 18) {
            header
            shortcutRow
            settingsCard
            actionCard
            statusCard
            Divider()
                .background(.white.opacity(0.16))
            quitRow
        }
        .padding(22)
        .frame(width: 420)
        .background(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .fill(Color.black.opacity(0.94))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 28, style: .continuous)
                .stroke(.white.opacity(0.14), lineWidth: 1)
        )
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        HStack(spacing: 16) {
            AppIcon()
            VStack(alignment: .leading, spacing: 5) {
                Text("TextLens")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(.white)
                Text("Screen OCR translation companion")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.48))
            }
            Spacer()
        }
    }

    private var shortcutRow: some View {
        HStack {
            Text("Global Shortcut")
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(.white.opacity(0.52))
            Spacer()
            Text("⌘⇧0")
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(.white.opacity(0.85))
        }
        .padding(.horizontal, 18)
        .frame(height: 48)
        .background(cardBackground)
    }

    private var settingsCard: some View {
        VStack(spacing: 12) {
            providerPicker

            FieldRow(title: "API Key") {
                SecureField(provider.apiKeyPlaceholder, text: $apiKey)
                    .textFieldStyle(.plain)
            }

            FieldRow(title: "Base URL") {
                TextField(provider.defaultBaseURL.absoluteString, text: $baseURL)
                    .textFieldStyle(.plain)
            }

            FieldRow(title: "Model") {
                TextField(provider.defaultModel, text: $model)
                    .textFieldStyle(.plain)
            }

            FieldRow(title: "Target") {
                TextField("Persian", text: $targetLanguage)
                    .textFieldStyle(.plain)
            }

            if let validationMessage {
                Text(validationMessage)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(validationMessage == "Saved." ? .green : .red)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            Button {
                save()
            } label: {
                Text("Save Settings")
                    .font(.system(size: 15, weight: .bold))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.white.opacity(0.055))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(.blue.opacity(0.34), lineWidth: 1.2)
                )
        )
    }

    private var actionCard: some View {
        Button(action: onTranslateArea) {
            SettingsCardRow(
                icon: "text.viewfinder",
                iconColor: .blue,
                title: "Translate Area",
                subtitle: "Select an area and translate",
                trailing: "⌘⇧0"
            )
        }
        .buttonStyle(.plain)
    }

    private var statusCard: some View {
        SettingsCardRow(
            icon: hasDraftAPIKey ? "checkmark" : "exclamationmark",
            iconColor: hasDraftAPIKey ? .green : .orange,
            title: "\(provider.title) API",
            subtitle: model,
            trailing: hasDraftAPIKey ? "Ready" : "Missing"
        )
    }

    private var hasDraftAPIKey: Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var providerPicker: some View {
        HStack(spacing: 8) {
            ForEach(TranslationProvider.allCases) { candidate in
                Button {
                    selectProvider(candidate)
                } label: {
                    Text(candidate.title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(provider == candidate ? .white : .white.opacity(0.58))
                        .frame(maxWidth: .infinity)
                        .frame(height: 36)
                        .background(
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .fill(provider == candidate ? Color.blue.opacity(0.92) : Color.white.opacity(0.06))
                        )
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(
            RoundedRectangle(cornerRadius: 15, style: .continuous)
                .fill(.black.opacity(0.22))
                .overlay(
                    RoundedRectangle(cornerRadius: 15, style: .continuous)
                        .stroke(.white.opacity(0.08), lineWidth: 1)
                )
        )
    }

    private var quitRow: some View {
        Button(action: onQuit) {
            HStack {
                Image(systemName: "power")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(.white.opacity(0.44))
                Text("Quit")
                    .font(.system(size: 17, weight: .bold))
                    .foregroundStyle(.white)
                Spacer()
                Text("⌘Q")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundStyle(.white.opacity(0.42))
            }
        }
        .buttonStyle(.plain)
    }

    private var cardBackground: some View {
        RoundedRectangle(cornerRadius: 20, style: .continuous)
            .fill(.white.opacity(0.055))
            .overlay(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(.white.opacity(0.08), lineWidth: 1)
            )
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
                provider: provider,
                apiKey: apiKey,
                baseURL: parsedBaseURL,
                model: model,
                targetLanguage: targetLanguage
            )
        )
        validationMessage = "Saved."
    }

    private func selectProvider(_ selectedProvider: TranslationProvider) {
        guard provider != selectedProvider else {
            return
        }

        provider = selectedProvider
        let selectedSettings = settingsStore.settings(for: selectedProvider)
        apiKey = selectedSettings.apiKey
        baseURL = selectedSettings.baseURL.absoluteString
        model = selectedSettings.model
        targetLanguage = selectedSettings.targetLanguage
        validationMessage = selectedSettings.hasAPIKey ? nil : "Add your \(selectedProvider.title) API key, then save."
    }
}

private struct FieldRow<Content: View>: View {
    let title: String
    @ViewBuilder var content: Content

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white.opacity(0.5))
            content
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .frame(height: 34)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(.black.opacity(0.18))
                )
        }
    }
}

private struct SettingsCardRow: View {
    let icon: String
    let iconColor: Color
    let title: String
    let subtitle: String
    let trailing: String

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(iconColor.opacity(0.18))
                Image(systemName: icon)
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(iconColor)
            }
            .frame(width: 50, height: 50)

            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundStyle(.white)
                Text(subtitle)
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.white.opacity(0.45))
                    .lineLimit(1)
            }

            Spacer()

            Text(trailing)
                .font(.system(size: 16, weight: .bold))
                .foregroundStyle(iconColor)
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.white.opacity(0.055))
                .overlay(
                    RoundedRectangle(cornerRadius: 20, style: .continuous)
                        .stroke(.white.opacity(0.08), lineWidth: 1)
                )
        )
    }
}

private struct AppIcon: View {
    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            RoundedRectangle(cornerRadius: 15, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color.blue, Color.cyan.opacity(0.78)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 58, height: 58)

            Text("T")
                .font(.system(size: 38, weight: .heavy))
                .foregroundStyle(.white)
                .offset(x: -9, y: -7)

            Circle()
                .fill(.white)
                .frame(width: 24, height: 24)
                .overlay(Circle().stroke(Color.blue.opacity(0.8), lineWidth: 4))
                .offset(x: 5, y: 5)
        }
        .frame(width: 66, height: 66)
    }
}
