import SwiftUI

struct SettingsView: View {
    var body: some View {
        Form {
            Text("TextLens Settings")
                .font(.headline)
            Text("API settings will be implemented in Phase 6.")
                .foregroundStyle(.secondary)
        }
        .padding()
        .frame(width: 420, height: 180)
    }
}

#Preview {
    SettingsView()
}
