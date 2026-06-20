import AppKit

final class TranslationFlowController {
    private(set) var isRunning = false
    private var selectionOverlayWindow: SelectionOverlayWindow?
    private var translationPopupWindow: TranslationPopupWindow?
    private var currentTask: Task<Void, Never>?
    private let settingsStore: SettingsStore
    private let openSettings: () -> Void
    private let permissionService = PermissionService()
    private let screenshotService = ScreenshotService()
    private let ocrService = OCRService()
    private let translationService: TranslationServiceProtocol = TranslationService()
    private let errorPresenter = ErrorPresenter()

    init(settingsStore: SettingsStore, openSettings: @escaping () -> Void) {
        self.settingsStore = settingsStore
        self.openSettings = openSettings
    }

    @MainActor
    func startTranslateArea() {
        guard settingsStore.settings.hasAPIKey else {
            openSettings()
            return
        }

        guard !isRunning else {
            return
        }

        isRunning = true
        runTranslateAreaShell()
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
                permissionService.openScreenRecordingSettings()
                throw TextLensError.screenRecordingPermissionMissing
            }

            let image = try screenshotService.capture(rect: selection.rect, displayID: selection.displayID)
            let recognizedText = try await ocrService.recognizeText(from: image)
            let settings = await MainActor.run {
                settingsStore.settings
            }
            let translationResult = try await translationService.translate(recognizedText, settings: settings)
            await MainActor.run {
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
        let popupWindow = TranslationPopupWindow(near: selectionRect) { [weak self] in
            self?.currentTask?.cancel()
            self?.currentTask = nil
            self?.isRunning = false
        }
        translationPopupWindow = popupWindow
        popupWindow.show()
    }
}
