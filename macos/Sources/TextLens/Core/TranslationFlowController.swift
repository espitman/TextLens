import AppKit

final class TranslationFlowController {
    private(set) var isRunning = false
    private var selectionOverlayWindow: SelectionOverlayWindow?
    private var translationPopupWindow: TranslationPopupWindow?
    private var currentTask: Task<Void, Never>?
    private var lastRecognizedText: String?
    private let settingsStore: SettingsStore
    private let historyStore: TranslationHistoryStore
    private let openSettings: () -> Void
    private let permissionService = PermissionService()
    private let screenshotService = ScreenshotService()
    private let ocrService = OCRService()
    private let translationService: TranslationServiceProtocol = TranslationService()
    private let errorPresenter = ErrorPresenter()

    init(
        settingsStore: SettingsStore,
        historyStore: TranslationHistoryStore,
        openSettings: @escaping () -> Void
    ) {
        self.settingsStore = settingsStore
        self.historyStore = historyStore
        self.openSettings = openSettings
    }

    @MainActor
    func startTranslateArea() {
        guard settingsStore.settings.isReadyToTranslate else {
            openSettings()
            return
        }

        guard !isRunning else {
            return
        }

        isRunning = true
        runTranslateAreaShell()
    }

    @MainActor
    func showHistoryItem(_ item: TranslationHistoryItem) {
        translationPopupWindow?.close()
        let popupWindow = makePopupWindow(near: Self.defaultHistoryPopupRect())
        translationPopupWindow = popupWindow
        popupWindow.show()
        popupWindow.showHistoryItem(item)
    }

    private func runTranslateAreaShell() {
        // TODO: Add multi-display overlay support after the primary-display MVP path is stable.
        let overlayWindow = SelectionOverlayWindow { [weak self] selection in
            self?.selectionOverlayWindow = nil

            guard let selection else {
                self?.isRunning = false
                return
            }

            Task { @MainActor [weak self] in
                self?.showLoadingPopup(near: selection.rect)
            }
            self?.currentTask = Task { [weak self] in
                await self?.handleSelectedArea(selection)
            }
        }

        selectionOverlayWindow = overlayWindow
        overlayWindow.show()
    }

    private func handleSelectedArea(_ selection: ScreenSelection) async {
        do {
            guard permissionService.hasScreenRecordingPermission() || permissionService.requestScreenRecordingPermission() else {
                throw TextLensError.screenRecordingPermissionMissing
            }

            let image = try screenshotService.capture(rect: selection.rect, displayID: selection.displayID)
            let recognizedText = try await ocrService.recognizeText(from: image)
            await MainActor.run {
                lastRecognizedText = recognizedText
            }
            let settings = await MainActor.run {
                settingsStore.settings
            }
            let translationResult = try await translationService.translate(recognizedText, settings: settings)
            await MainActor.run {
                historyStore.add(translationResult)
                translationPopupWindow?.showResult(translationResult)
                currentTask = nil
                isRunning = false
            }
        } catch is CancellationError {
            await MainActor.run {
                currentTask = nil
                isRunning = false
            }
        } catch {
            await MainActor.run {
                translationPopupWindow?.showError(error)
                currentTask = nil
                isRunning = false
            }
        }
    }

    @MainActor
    private func showLoadingPopup(near selectionRect: CGRect) {
        translationPopupWindow?.close()
        let popupWindow = makePopupWindow(near: selectionRect)
        translationPopupWindow = popupWindow
        popupWindow.show()
    }

    @MainActor
    private func makePopupWindow(near selectionRect: CGRect) -> TranslationPopupWindow {
        TranslationPopupWindow(near: selectionRect) { [weak self] in
            self?.currentTask?.cancel()
            self?.currentTask = nil
            self?.isRunning = false
        } onOpenScreenRecordingSettings: { [weak self] in
            guard let self else {
                return
            }
            if !permissionService.requestScreenRecordingPermission() {
                permissionService.openScreenRecordingSettings()
            }
        } retryOptions: { [weak self] in
            self?.retryOptions() ?? []
        } selectedRetryModel: { [weak self] in
            self?.settingsStore.settings.model
        } onSelectRetryModel: { [weak self] model in
            self?.selectRetryModel(model)
        } onRetry: { [weak self] in
            self?.retryLastTranslation()
        }
    }

    @MainActor
    private func retryOptions() -> [TranslationRetryOption] {
        settingsStore.settings.provider.modelCatalog.options.map {
            TranslationRetryOption(model: $0)
        }
    }

    @MainActor
    private func selectRetryModel(_ model: String) {
        let currentSettings = settingsStore.settings
        settingsStore.save(
            TranslationSettings(
                provider: currentSettings.provider,
                apiKey: currentSettings.apiKey,
                baseURL: currentSettings.baseURL,
                model: model,
                targetLanguage: currentSettings.targetLanguage
            )
        )
    }

    @MainActor
    private func retryLastTranslation() {
        guard let text = lastRecognizedText?.trimmingCharacters(in: .whitespacesAndNewlines),
              !text.isEmpty else {
            translationPopupWindow?.showError(TextLensError.noTextFound)
            return
        }

        currentTask?.cancel()
        translationPopupWindow?.viewModel.state = .loading
        isRunning = true
        currentTask = Task { [weak self] in
            await self?.translateRecognizedText(text)
        }
    }

    private func translateRecognizedText(_ recognizedText: String) async {
        do {
            let settings = await MainActor.run {
                settingsStore.settings
            }
            let translationResult = try await translationService.translate(recognizedText, settings: settings)
            await MainActor.run {
                historyStore.add(translationResult)
                translationPopupWindow?.showResult(translationResult)
                currentTask = nil
                isRunning = false
            }
        } catch is CancellationError {
            await MainActor.run {
                currentTask = nil
                isRunning = false
            }
        } catch {
            await MainActor.run {
                translationPopupWindow?.showError(error)
                currentTask = nil
                isRunning = false
            }
        }
    }

    private static func defaultHistoryPopupRect() -> CGRect {
        let visibleFrame = NSScreen.main?.visibleFrame ?? .zero
        return CGRect(
            x: visibleFrame.midX - 1,
            y: visibleFrame.midY + 1,
            width: 2,
            height: 2
        )
    }
}
