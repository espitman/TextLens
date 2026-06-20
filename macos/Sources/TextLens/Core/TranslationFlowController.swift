import AppKit

final class TranslationFlowController {
    private(set) var isRunning = false
    private var selectionOverlayWindow: SelectionOverlayWindow?
    private let settingsStore: SettingsStore
    private let openSettings: () -> Void
    private let permissionService = PermissionService()
    private let screenshotService = ScreenshotService()
    private let ocrService = OCRService()
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
            self?.isRunning = false

            guard let selection else {
                return
            }

            Task { [weak self] in
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
            await MainActor.run {
                errorPresenter.presentMessage(title: "Recognized Text", message: recognizedText)
            }
        } catch {
            await MainActor.run {
                errorPresenter.present(error)
            }
        }
    }
}
