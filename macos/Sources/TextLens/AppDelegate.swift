import AppKit

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private var menuBarController: MenuBarController?
    private var settingsPopoverController: SettingsPopoverController?
    private var translationFlowController: TranslationFlowController?
    private let settingsStore = SettingsStore()
    private lazy var hotKeyService = HotKeyService { [weak self] in
        self?.translationFlowController?.startTranslateArea()
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        FontRegistrar.registerBundledFonts()
        NSApp.setActivationPolicy(.accessory)

        let translationFlowController = TranslationFlowController(
            settingsStore: settingsStore,
            openSettings: { [weak self] in
                self?.settingsPopoverController?.show()
            }
        )
        let settingsPopoverController = SettingsPopoverController(
            settingsStore: settingsStore,
            onTranslateArea: {
                translationFlowController.startTranslateArea()
            },
            onQuit: {
                NSApp.terminate(nil)
            }
        )
        self.translationFlowController = translationFlowController
        self.settingsPopoverController = settingsPopoverController

        menuBarController = MenuBarController(
            onTranslateArea: { [weak self] in
                self?.translationFlowController?.startTranslateArea()
            },
            onOpenSettings: { [weak self] in
                self?.settingsPopoverController?.toggle()
            },
            onQuit: {
                NSApp.terminate(nil)
            }
        )
        if let menuBarController {
            menuBarController.attachSettingsPopover(settingsPopoverController)
        }
        registerGlobalHotKey()
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        false
    }

    func applicationWillTerminate(_ notification: Notification) {
        hotKeyService.unregisterDefaultShortcut()
    }

    private func registerGlobalHotKey() {
        do {
            try hotKeyService.registerDefaultShortcut()
        } catch {
            ErrorPresenter().present(error)
        }
    }
}
