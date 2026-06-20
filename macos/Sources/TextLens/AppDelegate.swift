import AppKit

final class AppDelegate: NSObject, NSApplicationDelegate {
    private var menuBarController: MenuBarController?
    private let settingsWindowController = SettingsWindowController()
    private let translationFlowController = TranslationFlowController()
    private lazy var hotKeyService = HotKeyService { [weak self] in
        self?.translationFlowController.startTranslateArea()
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
        menuBarController = MenuBarController(
            onTranslateArea: { [weak self] in
                self?.translationFlowController.startTranslateArea()
            },
            onOpenSettings: { [weak self] in
                self?.settingsWindowController.show()
            },
            onQuit: {
                NSApp.terminate(nil)
            }
        )
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
