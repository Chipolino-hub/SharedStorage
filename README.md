# SharedStorage

An educational **Java / JavaFX** desktop application for splitting a text secret into multiple shares using a **Shamir threshold-sharing scheme** and reconstructing it from enough shares.

**Selected source version:** the supplied *SharedStorage v3* archive, whose interface identifies the application as v2.0 and share files as v2.1.

## Features

- Split UTF-8 `.txt` files into **N shares**, requiring **K** shares for recovery.
- Save and load share files (`.txt`).
- Check a share's data integrity against its stored SHA-256 checksum in the v2.1 format.
- Check that selected shares belong to the same original file and reject duplicate indices.
- Reconstruct a text secret from sufficient shares and compare the original file hash.

## Requirements

- JDK 22 (the supplied Eclipse configuration targets Java 22).
- JavaFX 21 SDK.
- Eclipse IDE, or another Java IDE configured for JavaFX.

## Run in Eclipse

1. Import the directory using **File → Import → Existing Projects into Workspace**.
2. Configure a Java 22 JDK.
3. Configure the Eclipse User Library `JavaFX21` with JARs from the JavaFX SDK `lib` directory.
4. If needed, specify VM arguments: `--module-path "PATH_TO_JAVAFX_SDK/lib" --add-modules javafx.controls,javafx.fxml`.
5. Run `application.Main`.

## Validation

The share-splitting and recovery logic passed **225 programmatic round-trip cases** with different K/N combinations and text inputs, plus a share-file checksum modification test. The JavaFX user interface has not been launched in this environment.

## Security and limitations

**Educational prototype only — do not use it to protect real passwords, production keys or sensitive secrets.** It has not undergone a cryptographic security review. Share files contain sensitive recovery material and must never be committed or published. This repository contains only source code, UI resources and project configuration, **not** generated share files or user secrets.

## Project structure

- `src/application/` — Java source, FXML screens and CSS.
- `src/module-info.java` — Java module configuration.
- `.project`, `.classpath`, `.settings/` — Eclipse project files.

## Version notes

This repository combines the v3 share-file format (2.1) and controller with the classic UI stylesheet. The share-file loader validates the data checksum for both 1.0 and 2.1, and rejects unsupported version headers. The Shamir implementation is educational and has not undergone a professional cryptographic audit; do not use it for real credentials or sensitive secrets. The JavaFX desktop GUI has not been exercised in an automated end-to-end test.
