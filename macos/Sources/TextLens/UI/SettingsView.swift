import SwiftUI

struct SettingsView: View {
    @ObservedObject var settingsStore: SettingsStore
    @ObservedObject var historyStore: TranslationHistoryStore

    let onTranslateArea: () -> Void
    let onOpenHistoryItem: (TranslationHistoryItem) -> Void
    let onCloseSettings: () -> Void
    let onQuit: () -> Void

    private let permissionService = PermissionService()

    @State private var provider: TranslationProvider
    @State private var apiKey: String
    @State private var baseURL: String
    @State private var model: String
    @State private var targetLanguage: String
    @State private var validationMessage: String?
    @State private var hasScreenRecordingPermission: Bool
    @State private var isModelMenuOpen = false
    @State private var isCustomModel = false
    @State private var isEditingCustomModel = false

    init(
        settingsStore: SettingsStore,
        historyStore: TranslationHistoryStore,
        onTranslateArea: @escaping () -> Void = {},
        onOpenHistoryItem: @escaping (TranslationHistoryItem) -> Void = { _ in },
        onCloseSettings: @escaping () -> Void = {},
        onQuit: @escaping () -> Void = {}
    ) {
        self.settingsStore = settingsStore
        self.historyStore = historyStore
        self.onTranslateArea = onTranslateArea
        self.onOpenHistoryItem = onOpenHistoryItem
        self.onCloseSettings = onCloseSettings
        self.onQuit = onQuit
        let settings = settingsStore.settings
        _provider = State(initialValue: settings.provider)
        _apiKey = State(initialValue: settings.apiKey)
        _baseURL = State(initialValue: settings.baseURL.absoluteString)
        _model = State(initialValue: settings.model)
        _targetLanguage = State(initialValue: settings.targetLanguage)
        _hasScreenRecordingPermission = State(initialValue: PermissionService().hasScreenRecordingPermission())
        _isCustomModel = State(initialValue: !settings.provider.modelCatalog.options.contains(settings.model))
    }

    var body: some View {
        VStack(spacing: 18) {
            header
            if hasScreenRecordingPermission {
                settingsCard
                actionCard
                statusCard
                historyCarousel
            } else {
                permissionErrorCard
            }
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
        .onAppear {
            refreshScreenRecordingPermission()
        }
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

            EditableModelPicker(
                text: $model,
                isOpen: $isModelMenuOpen,
                isCustom: $isCustomModel,
                isEditingCustom: $isEditingCustomModel,
                catalog: provider.modelCatalog
            )

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
        Button {
            guard refreshScreenRecordingPermission() else {
                return
            }
            onTranslateArea()
        } label: {
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

    @ViewBuilder
    private var permissionErrorCard: some View {
        if !hasScreenRecordingPermission {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .fill(Color.red.opacity(0.18))
                    Image(systemName: "exclamationmark.triangle.fill")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundStyle(.red)
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 5) {
                    Text("Screen Recording Required")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(.red.opacity(0.95))
                    Text("TextLens needs permission to read the selected screen area.")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.52))
                        .lineLimit(2)
                }

                Spacer()

                Button {
                    openScreenRecordingPermission()
                } label: {
                    Text("Settings")
                        .font(.system(size: 13, weight: .bold))
                        .padding(.horizontal, 10)
                        .frame(height: 30)
                }
                .buttonStyle(.borderedProminent)
                .tint(.red)
            }
            .padding(16)
            .background(
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .fill(Color.red.opacity(0.08))
                    .overlay(
                        RoundedRectangle(cornerRadius: 20, style: .continuous)
                            .stroke(Color.red.opacity(0.34), lineWidth: 1)
                    )
            )
        }
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

    @ViewBuilder
    private var historyCarousel: some View {
        if !historyStore.items.isEmpty {
            VStack(alignment: .leading, spacing: 10) {
                HStack {
                    Text("History")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white.opacity(0.72))
                    Spacer()
                    Text("Last \(historyStore.items.count)")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundStyle(.white.opacity(0.38))
                }

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(historyStore.items) { item in
                            Button {
                                onOpenHistoryItem(item)
                            } label: {
                                HistoryCard(item: item)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.vertical, 1)
                }
            }
        }
    }

    private var hasDraftAPIKey: Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    private var providerPicker: some View {
        HStack(spacing: 8) {
            ForEach(TranslationProvider.settingsDisplayOrder) { candidate in
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
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.8) {
            if validationMessage == "Saved." {
                validationMessage = nil
            }
        }
    }

    @discardableResult
    private func refreshScreenRecordingPermission() -> Bool {
        let hasPermission = permissionService.hasScreenRecordingPermission()
        hasScreenRecordingPermission = hasPermission
        return hasPermission
    }

    private func openScreenRecordingPermission() {
        if permissionService.requestScreenRecordingPermission() {
            hasScreenRecordingPermission = true
        } else {
            hasScreenRecordingPermission = false
            permissionService.openScreenRecordingSettings()
            onCloseSettings()
        }
    }

    private func selectProvider(_ selectedProvider: TranslationProvider) {
        guard provider != selectedProvider else {
            return
        }

        provider = selectedProvider
        isModelMenuOpen = false
        isEditingCustomModel = false
        let selectedSettings = settingsStore.settings(for: selectedProvider)
        apiKey = selectedSettings.apiKey
        baseURL = selectedSettings.baseURL.absoluteString
        model = selectedSettings.model
        isCustomModel = !selectedProvider.modelCatalog.options.contains(selectedSettings.model)
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

private struct EditableModelPicker: View {
    @Binding var text: String
    @Binding var isOpen: Bool
    @Binding var isCustom: Bool
    @Binding var isEditingCustom: Bool
    let catalog: TranslationModelCatalog

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Model")
                .font(.system(size: 12, weight: .bold))
                .foregroundStyle(.white.opacity(0.5))

            if isEditingCustom {
                HStack(spacing: 8) {
                    TextField("Type custom model id", text: $text)
                        .textFieldStyle(.plain)
                        .font(.system(size: 13, weight: .medium))
                        .foregroundStyle(.white)
                        .onSubmit {
                            confirmCustomModel()
                        }

                    Button {
                        confirmCustomModel()
                    } label: {
                        Image(systemName: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(.green)
                            .frame(width: 26, height: 24)
                    }
                    .buttonStyle(.plain)
                }
                .padding(.leading, 12)
                .padding(.trailing, 6)
                .frame(height: 34)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(.black.opacity(0.18))
                )
            } else {
                Button {
                    withAnimation(.easeOut(duration: 0.12)) {
                        isOpen.toggle()
                    }
                } label: {
                    HStack(spacing: 8) {
                        Text(selectedLabel)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundStyle(text.isEmpty ? .white.opacity(0.38) : .white.opacity(0.92))
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .frame(maxWidth: .infinity, alignment: .leading)

                        Image(systemName: "chevron.down")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundStyle(.white.opacity(0.58))
                            .frame(width: 26, height: 24)
                            .rotationEffect(.degrees(isOpen ? 180 : 0))
                            .animation(.easeOut(duration: 0.12), value: isOpen)
                    }
                    .padding(.leading, 12)
                    .padding(.trailing, 6)
                    .frame(height: 34)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .background(
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(.black.opacity(0.18))
                )
            }

            if isOpen {
                VStack(alignment: .leading, spacing: 2) {
                    ForEach(catalog.options, id: \.self) { option in
                        Button {
                            text = option
                            isCustom = false
                            isEditingCustom = false
                            withAnimation(.easeOut(duration: 0.1)) {
                                isOpen = false
                            }
                        } label: {
                            HStack(spacing: 8) {
                                Text(option)
                                    .font(.system(size: 13, weight: .semibold))
                                    .foregroundStyle(.white.opacity(0.88))
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                                Spacer()
                                if !isCustom && option == text {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundStyle(.blue)
                                }
                            }
                            .padding(.horizontal, 12)
                            .frame(height: 30)
                            .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                    }

                    Divider()
                        .background(.white.opacity(0.08))
                        .padding(.vertical, 4)

                    Button {
                        isCustom = true
                        isEditingCustom = true
                        if catalog.options.contains(text) {
                            text = ""
                        }
                        withAnimation(.easeOut(duration: 0.1)) {
                            isOpen = false
                        }
                    } label: {
                        HStack(spacing: 8) {
                            Text("Custom model...")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.88))
                            Spacer()
                            if isCustom {
                                Image(systemName: "checkmark")
                                    .font(.system(size: 11, weight: .bold))
                                    .foregroundStyle(.blue)
                            }
                        }
                        .padding(.horizontal, 12)
                        .frame(height: 30)
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)
                }
                .padding(.vertical, 8)
                .frame(maxWidth: .infinity)
                .background(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .fill(Color(red: 0.12, green: 0.12, blue: 0.13))
                        .shadow(color: .black.opacity(0.34), radius: 18, y: 10)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(.white.opacity(0.08), lineWidth: 1)
                )
            }
        }
    }

    private var selectedLabel: String {
        if isCustom {
            return text.isEmpty ? "Custom model" : text
        }

        return text.isEmpty ? catalog.placeholder : text
    }

    private func confirmCustomModel() {
        let trimmedModel = text.trimmingCharacters(in: .whitespacesAndNewlines)
        text = trimmedModel
        isCustom = true
        isEditingCustom = false
        isOpen = false
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

private struct HistoryCard: View {
    let item: TranslationHistoryItem

    var body: some View {
        VStack(alignment: .trailing, spacing: 8) {
            Text(rtlPreview(item.translatedText))
                .font(.custom("Vazirmatn", size: 12).weight(.medium))
                .foregroundStyle(.white.opacity(0.88))
                .lineLimit(3)
                .multilineTextAlignment(.trailing)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .environment(\.layoutDirection, .rightToLeft)

            HStack {
                if let costToman = item.costToman {
                    Text("\(costToman.formatted()) تومان")
                        .font(.custom("Vazirmatn", size: 10).weight(.medium))
                        .foregroundStyle(.green.opacity(0.86))
                        .environment(\.layoutDirection, .rightToLeft)
                }
                Spacer()
                Image(systemName: "arrow.up.left.and.arrow.down.right")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(.blue.opacity(0.9))
            }
        }
        .padding(12)
        .frame(width: 168, height: 104)
        .background(
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .fill(.white.opacity(0.055))
                .overlay(
                    RoundedRectangle(cornerRadius: 16, style: .continuous)
                        .stroke(.white.opacity(0.08), lineWidth: 1)
                )
        )
    }

    private func rtlPreview(_ text: String) -> String {
        "\u{202B}\(text)\u{202C}"
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
