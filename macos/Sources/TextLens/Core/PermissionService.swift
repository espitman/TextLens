import AppKit
import CoreGraphics
import Foundation

final class PermissionService {
    func hasScreenRecordingPermission() -> Bool {
        CGPreflightScreenCaptureAccess()
    }

    @discardableResult
    func requestScreenRecordingPermission() -> Bool {
        CGRequestScreenCaptureAccess()
    }

    func openScreenRecordingSettings() {
        let url = URL(string: "x-apple.systempreferences:com.apple.preference.security?Privacy_ScreenCapture")
        if let url {
            NSWorkspace.shared.open(url)
            bringSystemSettingsToFront()
        }
    }

    private func bringSystemSettingsToFront() {
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
            let bundleIdentifiers = [
                "com.apple.SystemSettings",
                "com.apple.systempreferences",
            ]

            for bundleIdentifier in bundleIdentifiers {
                guard let app = NSRunningApplication.runningApplications(withBundleIdentifier: bundleIdentifier).first else {
                    continue
                }

                app.unhide()
                app.activate(options: [.activateAllWindows, .activateIgnoringOtherApps])
                return
            }
        }
    }
}
