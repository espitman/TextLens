import SwiftUI

struct TranslationPopupView: View {
    let translatedText: String
    let onCopy: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            ScrollView {
                Text(translatedText)
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .multilineTextAlignment(.trailing)
                    .textSelection(.enabled)
                    .environment(\.layoutDirection, .rightToLeft)
            }
            .frame(maxHeight: 150)

            HStack {
                Button("Copy", action: onCopy)
                Spacer()
                Button("Close", action: onClose)
                    .keyboardShortcut(.cancelAction)
            }
        }
        .padding(16)
        .frame(width: 420)
        .frame(minHeight: 160)
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 8))
    }
}

#Preview {
    TranslationPopupView(
        translatedText: "ترجمه اینجا نمایش داده می‌شود.",
        onCopy: {},
        onClose: {}
    )
}
