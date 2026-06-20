import AppKit

final class TranslationFlowController {
    private(set) var isRunning = false
    private var selectionOverlayWindow: SelectionOverlayWindow?
    private let permissionService = PermissionService()
    private let screenshotService = ScreenshotService()
    private let errorPresenter = ErrorPresenter()

    func startTranslateArea() {
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

            self?.handleSelectedArea(selection)
        }

        selectionOverlayWindow = overlayWindow
        overlayWindow.show()
    }

    private func handleSelectedArea(_ selection: ScreenSelection) {
        do {
            guard permissionService.hasScreenRecordingPermission() || permissionService.requestScreenRecordingPermission() else {
                permissionService.openScreenRecordingSettings()
                throw TextLensError.screenRecordingPermissionMissing
            }

            _ = try screenshotService.capture(rect: selection.rect, displayID: selection.displayID)
            // TODO: Pass the captured image to OCR in Phase 5.
        } catch {
            errorPresenter.present(error)
        }
    }
}
