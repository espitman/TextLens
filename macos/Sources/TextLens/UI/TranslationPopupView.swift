import AppKit
import SwiftUI

@MainActor
final class TranslationPopupViewModel: ObservableObject {
    enum State: Equatable {
        case loading
        case result(text: String, costToman: Int?)
        case failed(message: String, action: FailureAction?)
    }

    enum FailureAction: Equatable {
        case openScreenRecordingSettings
    }

    @Published var state: State = .loading
}

struct TranslationPopupView: View {
    static let popupWidth: CGFloat = 760
    static let maxPopupHeight: CGFloat = 520
    static let loadingHeight: CGFloat = 300

    @ObservedObject var viewModel: TranslationPopupViewModel
    let popupHeight: CGFloat
    let onCopy: () -> Void
    let onCancel: () -> Void
    let onClose: () -> Void
    let onFailureAction: (TranslationPopupViewModel.FailureAction) -> Void

    var body: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, 18)
                .padding(.top, 16)
                .padding(.bottom, 12)

            Divider()
                .padding(.horizontal, 18)

            content
                .frame(maxWidth: .infinity)
                .frame(height: contentHeight)
                .padding(.horizontal, 20)
                .padding(.vertical, 18)

            Divider()

            footer
                .padding(.horizontal, 20)
                .padding(.vertical, 14)
        }
        .frame(width: Self.popupWidth, height: popupHeight)
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
            HStack(spacing: 14) {
                AppMark()
                VStack(alignment: .leading, spacing: 3) {
                    Text("TextLens")
                        .font(.system(size: 19, weight: .semibold))
                        .foregroundStyle(.black.opacity(0.82))
                    Text(headerSubtitle)
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.black.opacity(0.42))
                }
            }

            Spacer()

            HStack(spacing: 10) {
                if case .result = viewModel.state {
                    Image(systemName: "checkmark.circle")
                        .font(.system(size: 22, weight: .medium))
                        .foregroundStyle(.green)
                }

                Text("ترجمه")
                    .font(.custom("Vazirmatn", size: 19).weight(.semibold))
                    .foregroundStyle(.black.opacity(0.84))
                    .environment(\.layoutDirection, .rightToLeft)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            LoadingGraphic()
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)

        case .result(let text, _):
            RtlScrollTextView(text: text)
                .padding(.horizontal, 22)
                .padding(.vertical, 20)
            .background(contentCardBackground)
            .environment(\.layoutDirection, .rightToLeft)

        case .failed(let message, _):
            VStack(spacing: 14) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .font(.system(size: 30, weight: .semibold))
                    .foregroundStyle(.red)
                Text(message)
                    .font(.custom("Vazirmatn", size: 16).weight(.medium))
                    .foregroundStyle(.red.opacity(0.88))
                    .frame(maxWidth: .infinity, alignment: .center)
                    .multilineTextAlignment(.center)
            }
            .padding(28)
            .background(contentCardBackground)
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
            Button {
                onCancel()
            } label: {
                Label("لغو", systemImage: "xmark")
                    .font(.custom("Vazirmatn", size: 13).weight(.medium))
            }
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

        case .failed(_, let action):
            if let action {
                Button {
                    onFailureAction(action)
                } label: {
                    Label("Open Screen Recording Settings", systemImage: "gear")
                        .font(.system(size: 13, weight: .semibold))
                }
                .buttonStyle(.bordered)
                .controlSize(.regular)
            }

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
            .overlay(
                LinearGradient(
                    colors: [
                        Color.white.opacity(0.72),
                        Color(red: 0.95, green: 0.97, blue: 1.0).opacity(0.64),
                        Color(red: 1.0, green: 0.96, blue: 0.92).opacity(0.34),
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
    }

    private var contentCardBackground: some View {
        RoundedRectangle(cornerRadius: 18, style: .continuous)
            .fill(Color.white.opacity(0.46))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(Color.black.opacity(0.055), lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.045), radius: 16, y: 8)
    }

    private var contentHeight: CGFloat {
        max(110, popupHeight - 152)
    }

    private var headerSubtitle: String {
        switch viewModel.state {
        case .loading:
            "Reading selected area"
        case .result:
            "Translation ready"
        case .failed:
            "Could not translate"
        }
    }

}

private struct RtlScrollTextView: NSViewRepresentable {
    let text: String

    func makeNSView(context: Context) -> NSScrollView {
        let scrollView = NSScrollView()
        scrollView.drawsBackground = false
        scrollView.hasVerticalScroller = true
        scrollView.hasHorizontalScroller = false
        scrollView.autohidesScrollers = true
        scrollView.borderType = .noBorder

        let textView = NSTextView()
        textView.isEditable = false
        textView.isSelectable = true
        textView.drawsBackground = false
        textView.textContainerInset = .zero
        textView.textContainer?.lineFragmentPadding = 0
        textView.textContainer?.widthTracksTextView = true
        textView.textContainer?.heightTracksTextView = false
        textView.isVerticallyResizable = true
        textView.isHorizontallyResizable = false
        textView.autoresizingMask = [.width]
        textView.minSize = NSSize(width: 0, height: 0)
        textView.maxSize = NSSize(width: CGFloat.greatestFiniteMagnitude, height: CGFloat.greatestFiniteMagnitude)
        textView.alignment = .right
        textView.baseWritingDirection = .rightToLeft
        textView.textContainer?.containerSize = NSSize(
            width: scrollView.contentSize.width,
            height: CGFloat.greatestFiniteMagnitude
        )

        scrollView.documentView = textView
        return scrollView
    }

    func updateNSView(_ scrollView: NSScrollView, context: Context) {
        guard let textView = scrollView.documentView as? NSTextView else {
            return
        }

        let paragraph = NSMutableParagraphStyle()
        paragraph.alignment = .right
        paragraph.baseWritingDirection = .rightToLeft
        paragraph.lineSpacing = 6

        textView.textStorage?.setAttributedString(
            NSAttributedString(
                string: text,
                attributes: [
                    .font: NSFont(name: "Vazirmatn", size: 14) ?? .systemFont(ofSize: 14),
                    .foregroundColor: NSColor.black.withAlphaComponent(0.84),
                    .paragraphStyle: paragraph,
                ]
            )
        )

        textView.textContainer?.containerSize = NSSize(
            width: scrollView.contentSize.width,
            height: CGFloat.greatestFiniteMagnitude
        )
        textView.layoutManager?.ensureLayout(for: textView.textContainer!)
        textView.sizeToFit()
    }
}

private struct LoadingGraphic: View {
    @State private var rotation: Double = 0
    @State private var pulse = false

    var body: some View {
        ZStack {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [Color.blue.opacity(0.12), Color.clear],
                        center: .center,
                        startRadius: 8,
                        endRadius: 96
                    )
                )
                .frame(width: 210, height: 210)
                .scaleEffect(pulse ? 1.08 : 0.94)

            Circle()
                .stroke(Color.black.opacity(0.06), lineWidth: 10)
                .frame(width: 98, height: 98)

            Circle()
                .trim(from: 0.08, to: 0.74)
                .stroke(
                    AngularGradient(
                        colors: [
                            Color.blue.opacity(0.12),
                            Color.blue.opacity(0.95),
                            Color.cyan.opacity(0.76),
                            Color.blue.opacity(0.12),
                        ],
                        center: .center
                    ),
                    style: StrokeStyle(lineWidth: 10, lineCap: .round)
                )
                .frame(width: 98, height: 98)
                .rotationEffect(.degrees(rotation))

            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Color.white.opacity(0.64))
                .frame(width: 64, height: 64)
                .overlay(
                    Image(systemName: "text.viewfinder")
                        .font(.system(size: 26, weight: .semibold))
                        .foregroundStyle(Color.blue.opacity(0.9))
                )
                .shadow(color: .black.opacity(0.08), radius: 14, y: 6)
        }
        .frame(width: 240, height: 180)
        .onAppear {
            withAnimation(.linear(duration: 1.05).repeatForever(autoreverses: false)) {
                rotation = 360
            }
            withAnimation(.easeInOut(duration: 1.25).repeatForever(autoreverses: true)) {
                pulse = true
            }
        }
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
        popupHeight: 420,
        onCopy: {},
        onCancel: {},
        onClose: {},
        onFailureAction: { _ in }
    )
}
