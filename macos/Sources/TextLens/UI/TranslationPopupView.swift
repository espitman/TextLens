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

struct TranslationRetryOption: Equatable, Identifiable {
    var id: String { model }
    var model: String
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
    let retryOptions: [TranslationRetryOption]
    let selectedRetryModel: String?
    let onSelectRetryModel: (String) -> Void
    let onRetry: () -> Void

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
            PopupFooterButton(title: "لغو", systemImage: "xmark", style: .secondary) {
                onCancel()
            }
                .keyboardShortcut(.cancelAction)

        case .result:
            PopupFooterButton(title: "کپی", systemImage: "doc.on.doc", style: .secondary) {
                onCopy()
            }

            PopupFooterButton(title: "بستن", systemImage: "xmark", style: .primary) {
                onClose()
            }
            .keyboardShortcut(.cancelAction)

        case .failed(_, let action):
            if !retryOptions.isEmpty {
                RetryModelPicker(
                    options: retryOptions,
                    selectedModel: selectedRetryModel,
                    onSelect: onSelectRetryModel
                )

                PopupFooterButton(title: "Retry", systemImage: "arrow.clockwise", style: .gold) {
                    onRetry()
                }
            }

            if let action {
                PopupFooterButton(title: "Settings", systemImage: "gearshape", style: .danger) {
                    onFailureAction(action)
                }
            }

            PopupFooterButton(title: "بستن", systemImage: "xmark", style: .primary) {
                onClose()
            }
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
                        Color(red: 1.0, green: 0.96, blue: 0.84).opacity(0.60),
                        Color(red: 0.98, green: 0.86, blue: 0.48).opacity(0.22),
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

private struct RetryModelPicker: View {
    let options: [TranslationRetryOption]
    let selectedModel: String?
    let onSelect: (String) -> Void

    var body: some View {
        Menu {
            ForEach(options) { option in
                Button {
                    onSelect(option.model)
                } label: {
                    if option.model == selectedModel {
                        Label(option.model, systemImage: "checkmark")
                    } else {
                        Text(option.model)
                    }
                }
            }
        } label: {
            HStack(spacing: 6) {
                Text(shortModelName)
                    .font(.system(size: 12, weight: .semibold))
                    .lineLimit(1)
                    .truncationMode(.middle)
                Image(systemName: "chevron.down")
                    .font(.system(size: 10, weight: .bold))
            }
            .foregroundStyle(Color.black.opacity(0.78))
            .padding(.horizontal, 10)
            .frame(height: 30)
            .background(
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(Color.white.opacity(0.58))
                    .overlay(
                        RoundedRectangle(cornerRadius: 9, style: .continuous)
                            .stroke(Color.black.opacity(0.08), lineWidth: 1)
                    )
            )
        }
        .menuStyle(.borderlessButton)
        .frame(maxWidth: 190)
    }

    private var shortModelName: String {
        selectedModel ?? options.first?.model ?? "Model"
    }
}

private struct PopupFooterButton: View {
    enum Style {
        case primary
        case secondary
        case gold
        case danger
    }

    let title: String
    let systemImage: String
    let style: Style
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                Image(systemName: systemImage)
                    .font(.system(size: 13, weight: .semibold))
                Text(title)
                    .font(textFont)
                    .lineLimit(1)
            }
            .foregroundStyle(foregroundColor)
            .padding(.horizontal, 14)
            .frame(height: 34)
            .background(
                Capsule(style: .continuous)
                    .fill(backgroundColor)
            )
            .overlay(
                Capsule(style: .continuous)
                    .stroke(borderColor, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }

    private var textFont: Font {
        if title.range(of: #"[\u{0600}-\u{06FF}]"#, options: .regularExpression) != nil {
            return .custom("Vazirmatn", size: 13).weight(.medium)
        }
        return .system(size: 13, weight: .semibold)
    }

    private var foregroundColor: Color {
        switch style {
        case .primary:
            Color.white
        case .secondary:
            Color.black.opacity(0.78)
        case .gold:
            Color.black.opacity(0.86)
        case .danger:
            Color(red: 0.74, green: 0.12, blue: 0.12)
        }
    }

    private var backgroundColor: Color {
        switch style {
        case .primary:
            Color.black.opacity(0.88)
        case .secondary:
            Color.white.opacity(0.58)
        case .gold:
            Color(red: 1.0, green: 0.78, blue: 0.25).opacity(0.86)
        case .danger:
            Color(red: 1.0, green: 0.24, blue: 0.24).opacity(0.11)
        }
    }

    private var borderColor: Color {
        switch style {
        case .primary:
            Color(red: 1.0, green: 0.78, blue: 0.25).opacity(0.38)
        case .secondary:
            Color.black.opacity(0.08)
        case .gold:
            Color(red: 0.72, green: 0.48, blue: 0.10).opacity(0.22)
        case .danger:
            Color(red: 1.0, green: 0.24, blue: 0.24).opacity(0.24)
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
        textView.scrollRangeToVisible(NSRange(location: 0, length: 0))
        scrollView.contentView.scroll(to: .zero)
        scrollView.reflectScrolledClipView(scrollView.contentView)
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
                        colors: [Color(red: 1.0, green: 0.78, blue: 0.25).opacity(0.14), Color.clear],
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
                            Color(red: 1.0, green: 0.78, blue: 0.25).opacity(0.12),
                            Color(red: 1.0, green: 0.78, blue: 0.25).opacity(0.95),
                            Color(red: 0.72, green: 0.48, blue: 0.10).opacity(0.76),
                            Color(red: 1.0, green: 0.78, blue: 0.25).opacity(0.12),
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
                        .foregroundStyle(Color(red: 0.72, green: 0.48, blue: 0.10).opacity(0.95))
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
                        colors: [
                            Color.black,
                            Color(red: 0.16, green: 0.12, blue: 0.05),
                        ],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 36, height: 36)
                .overlay(
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .stroke(Color(red: 0.95, green: 0.70, blue: 0.20).opacity(0.42), lineWidth: 1)
                )

            Text("T")
                .font(.system(size: 24, weight: .heavy))
                .foregroundStyle(Color(red: 1.0, green: 0.82, blue: 0.28))
                .offset(x: -5, y: -4)

            Circle()
                .fill(Color.black.opacity(0.92))
                .frame(width: 16, height: 16)
                .overlay(
                    Circle()
                        .stroke(Color(red: 1.0, green: 0.78, blue: 0.25), lineWidth: 3)
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
        onFailureAction: { _ in },
        retryOptions: [
            TranslationRetryOption(model: "google/gemma-4-31b-it:free"),
            TranslationRetryOption(model: "openai/gpt-5-nano"),
        ],
        selectedRetryModel: "google/gemma-4-31b-it:free",
        onSelectRetryModel: { _ in },
        onRetry: {}
    )
}
