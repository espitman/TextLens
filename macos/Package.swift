// swift-tools-version: 5.9

import PackageDescription

let package = Package(
    name: "TextLens",
    platforms: [
        .macOS(.v13)
    ],
    products: [
        .executable(
            name: "TextLens",
            targets: ["TextLens"]
        )
    ],
    targets: [
        .executableTarget(
            name: "TextLens",
            path: "Sources/TextLens",
            resources: [
                .process("Resources")
            ]
        )
    ]
)
