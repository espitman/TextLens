import Carbon
import Foundation

final class HotKeyService {
    private static let defaultHotKeyID = EventHotKeyID(signature: fourCharacterCode("TLNS"), id: 1)

    private var hotKeyRef: EventHotKeyRef?
    private var eventHandlerRef: EventHandlerRef?
    private let onDefaultShortcut: () -> Void

    init(onDefaultShortcut: @escaping () -> Void) {
        self.onDefaultShortcut = onDefaultShortcut
    }

    deinit {
        unregisterDefaultShortcut()
    }

    func registerDefaultShortcut() throws {
        unregisterDefaultShortcut()

        var eventType = EventTypeSpec(eventClass: OSType(kEventClassKeyboard), eventKind: UInt32(kEventHotKeyPressed))
        let handlerStatus = InstallEventHandler(
            GetApplicationEventTarget(),
            HotKeyService.eventHandler,
            1,
            &eventType,
            Unmanaged.passUnretained(self).toOpaque(),
            &eventHandlerRef
        )

        guard handlerStatus == noErr else {
            eventHandlerRef = nil
            throw TextLensError.hotKeyRegistrationFailed(status: handlerStatus)
        }

        var registeredHotKeyRef: EventHotKeyRef?
        let hotKeyStatus = RegisterEventHotKey(
            UInt32(kVK_ANSI_T),
            UInt32(cmdKey | shiftKey),
            HotKeyService.defaultHotKeyID,
            GetApplicationEventTarget(),
            0,
            &registeredHotKeyRef
        )

        guard hotKeyStatus == noErr else {
            unregisterDefaultShortcut()
            throw TextLensError.hotKeyRegistrationFailed(status: hotKeyStatus)
        }

        hotKeyRef = registeredHotKeyRef
    }

    func unregisterDefaultShortcut() {
        if let hotKeyRef {
            UnregisterEventHotKey(hotKeyRef)
            self.hotKeyRef = nil
        }

        if let eventHandlerRef {
            RemoveEventHandler(eventHandlerRef)
            self.eventHandlerRef = nil
        }
    }

    private func handleHotKey(event: EventRef?) -> OSStatus {
        guard let event else {
            return OSStatus(eventNotHandledErr)
        }

        var hotKeyID = EventHotKeyID()
        let status = GetEventParameter(
            event,
            EventParamName(kEventParamDirectObject),
            EventParamType(typeEventHotKeyID),
            nil,
            MemoryLayout<EventHotKeyID>.size,
            nil,
            &hotKeyID
        )

        guard status == noErr else {
            return status
        }

        guard hotKeyID.signature == HotKeyService.defaultHotKeyID.signature,
              hotKeyID.id == HotKeyService.defaultHotKeyID.id else {
            return OSStatus(eventNotHandledErr)
        }

        DispatchQueue.main.async { [onDefaultShortcut] in
            onDefaultShortcut()
        }

        return noErr
    }
}

private extension HotKeyService {
    static let eventHandler: EventHandlerUPP = { _, event, userData in
        guard let userData else {
            return OSStatus(eventNotHandledErr)
        }

        let service = Unmanaged<HotKeyService>.fromOpaque(userData).takeUnretainedValue()
        return service.handleHotKey(event: event)
    }
}

private func fourCharacterCode(_ string: String) -> OSType {
    assert(string.utf8.count == 4)

    return string.utf8.reduce(0) { result, character in
        (result << 8) + OSType(character)
    }
}
