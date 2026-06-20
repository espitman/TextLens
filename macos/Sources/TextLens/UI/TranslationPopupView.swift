import SwiftUI

@MainActor
final class TranslationPopupViewModel: ObservableObject {
    enum State: Equatable {
        case loading
        case result(text: String, costToman: Int?)
        case failed(String)
    }

    @Published var state: State = .loading
}

struct TranslationPopupView: View {
    static let popupSize = CGSize(width: 1170, height: 750)

    @ObservedObject var viewModel: TranslationPopupViewModel
    let onCopy: () -> Void
    let onCancel: () -> Void
    let onClose: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, 18)
                .padding(.top, 18)
                .padding(.bottom, 14)

            Divider()
                .padding(.horizontal, 18)

            content
                .frame(maxWidth: .infinity)
                .frame(height: 560)
                .padding(.horizontal, 28)
                .padding(.vertical, 24)

            Divider()

            footer
                .padding(.horizontal, 22)
                .padding(.top, 12)
                .padding(.bottom, 22)
        }
        .frame(width: Self.popupSize.width, height: Self.popupSize.height)
        .background(panelBackground)
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Color.black.opacity(0.08), lineWidth: 1)
        )
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
        .shadow(color: .black.opacity(0.22), radius: 26, y: 14)
        .preferredColorScheme(.light)
    }

    private var header: some View {
        HStack(alignment: .center) {
            HStack(spacing: 12) {
                AppMark()
                Text("TextLens")
                    .font(.system(size: 18, weight: .medium))
                    .foregroundStyle(.black.opacity(0.78))
            }

            Spacer()

            HStack(spacing: 10) {
                if case .result = viewModel.state {
                    Image(systemName: "checkmark.circle")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundStyle(.green)
                }

                Text("ترجمه")
                    .font(.custom("Vazirmatn", size: 24).weight(.bold))
                    .foregroundStyle(.black.opacity(0.86))
                    .environment(\.layoutDirection, .rightToLeft)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            HStack {
                Spacer()

                ProgressView()
                    .controlSize(.large)
                    .scaleEffect(1.12)

                Spacer()

                Text("در حال خواندن و ترجمه...")
                    .font(.custom("Vazirmatn", size: 21).weight(.medium))
                    .foregroundStyle(.black.opacity(0.84))
                    .multilineTextAlignment(.trailing)
                    .environment(\.layoutDirection, .rightToLeft)
            }

        case .result(let text, _):
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Text(rtlText(text))
                        .font(.custom("Vazirmatn", size: 15).weight(.regular))
                        .foregroundStyle(.black.opacity(0.86))
                        .lineSpacing(5)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .multilineTextAlignment(.leading)
                        .textSelection(.enabled)
                        .environment(\.layoutDirection, .rightToLeft)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .environment(\.layoutDirection, .rightToLeft)

        case .failed(let message):
            Text(message)
                .font(.custom("Vazirmatn", size: 18).weight(.medium))
                .foregroundStyle(.red)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .multilineTextAlignment(.trailing)
                .environment(\.layoutDirection, .rightToLeft)
        }
    }

    private var footer: some View {
        HStack(spacing: 14) {
            footerLeading
            Spacer()
            footerActions
        }
    }

    @ViewBuilder
    private var footerLeading: some View {
        switch viewModel.state {
        case .result(_, let costToman):
            CostBadge(costToman: costToman)
        default:
            EmptyView()
        }
    }

    @ViewBuilder
    private var footerActions: some View {
        switch viewModel.state {
        case .loading:
            Button("لغو", action: onCancel)
                .font(.custom("Vazirmatn", size: 13).weight(.medium))
                .buttonStyle(.bordered)
                .controlSize(.regular)
                .keyboardShortcut(.cancelAction)

        case .result:
            Button {
                onCopy()
            } label: {
                Label("کپی", systemImage: "doc.on.doc")
                    .font(.custom("Vazirmatn", size: 13).weight(.medium))
            }
            .buttonStyle(.bordered)
            .controlSize(.regular)

            Button {
                onClose()
            } label: {
                Label("بستن", systemImage: "xmark")
                    .font(.custom("Vazirmatn", size: 13).weight(.medium))
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.regular)
            .keyboardShortcut(.cancelAction)

        case .failed:
            Button("بستن", action: onClose)
                .font(.custom("Vazirmatn", size: 13).weight(.medium))
                .buttonStyle(.borderedProminent)
                .controlSize(.regular)
                .keyboardShortcut(.cancelAction)
        }
    }

    private var panelBackground: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(.ultraThinMaterial)
            .overlay(Color.white.opacity(0.46))
    }

    private func rtlText(_ text: String) -> String {
        text
            .split(separator: "\n", omittingEmptySubsequences: false)
            .map { "\u{202B}\($0)\u{202C}" }
            .joined(separator: "\n")
    }
}

private struct AppMark: View {
    var body: some View {
        ZStack(alignment: .bottomTrailing) {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Color.blue.opacity(0.92), Color.cyan.opacity(0.86)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 36, height: 36)

            Text("T")
                .font(.system(size: 24, weight: .heavy))
                .foregroundStyle(.white)
                .offset(x: -5, y: -4)

            Circle()
                .fill(.white)
                .frame(width: 16, height: 16)
                .overlay(
                    Circle()
                        .stroke(Color.blue.opacity(0.8), lineWidth: 3)
                )
                .shadow(color: .black.opacity(0.2), radius: 2, y: 1)
                .offset(x: 3, y: 3)
        }
        .frame(width: 40, height: 40)
    }
}

private struct CostBadge: View {
    let costToman: Int?

    var body: some View {
        HStack(spacing: 8) {
            Text(label)
            Image(systemName: "tag")
                .font(.system(size: 13, weight: .medium))
        }
        .font(.custom("Vazirmatn", size: 13).weight(.medium))
        .foregroundStyle(Color.green.opacity(0.9))
        .padding(.horizontal, 11)
        .padding(.vertical, 7)
        .background(
            RoundedRectangle(cornerRadius: 11, style: .continuous)
                .fill(Color.green.opacity(0.1))
        )
        .overlay(
            RoundedRectangle(cornerRadius: 11, style: .continuous)
                .stroke(Color.green.opacity(0.22), lineWidth: 1)
        )
    }

    private var label: String {
        guard let costToman else {
            return "هزینه: نامشخص"
        }

        return "هزینه: \(costToman.formatted()) تومان"
    }
}

#Preview {
    TranslationPopupView(
        viewModel: {
            let model = TranslationPopupViewModel()
            model.state = .result(
                text: "برای استفاده از قابلیت TextLens، اجازه دسترسی به ناحیه انتخاب‌شده از صفحه نمایش مورد نیاز است. این دسترسی تنها برای خواندن متن از تصویر استفاده می‌شود.",
                costToman: 12
            )
            return model
        }(),
        onCopy: {},
        onCancel: {},
        onClose: {}
    )
}
