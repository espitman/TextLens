import SwiftUI

struct TranslationPopupView: View {
    let translatedText: String

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(translatedText)
                .textSelection(.enabled)

            HStack {
                Button("Copy") {
                    // TODO: Copy translation in Phase 8.
                }
                Button("Close") {
                    // TODO: Close popup window in Phase 8.
                }
            }
        }
        .padding()
        .frame(minWidth: 280, idealWidth: 360)
    }
}

#Preview {
    TranslationPopupView(translatedText: "ترجمه اینجا نمایش داده می‌شود.")
}
