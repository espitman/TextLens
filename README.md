# TextLens

TextLens is a cross-platform project for translating text from selected screen areas into Persian.

The first implementation target is macOS. Android and other platforms are planned for later.

## Repository Structure

```text
TextLens/
  macos/         macOS app implementation
  docs/          product specs and implementation plans
```

## Current App

The macOS app is scaffolded in:

```text
macos
```

Build it with:

```sh
cd macos
swift build
```

Build, install, and run it from the repository root with:

```sh
./run
```

The script creates `/Applications/TextLens.app` and opens it. This gives macOS a stable app identity for Screen Recording permission.

## Documentation

- [Implementation spec](docs/TextLens_Codex_Spec.md)
- [MVP TODO](docs/TODO.md)
