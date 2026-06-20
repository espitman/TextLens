import CoreGraphics

extension CGRect {
    var isMeaningfulSelection: Bool {
        width >= 8 && height >= 8
    }
}
