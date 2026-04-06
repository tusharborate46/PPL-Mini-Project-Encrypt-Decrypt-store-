# Java TUI Encrypted Notes

A simple Java terminal UI (TUI) app for writing and reading a `.txt` file where content is always stored encrypted.

## Features

- Beautiful terminal menu with colors and boxed sections.
- Write text to `notes.txt` from the menu.
- Content is encrypted before saving.
- Read and decrypt the same file directly from the menu.
- Simple code and data flow using plain Java classes.

## Project Structure

- `src/Main.java` – app entry point and TUI flow.
- `src/FileService.java` – file read/write logic.
- `src/CryptoService.java` – AES encryption/decryption.
- `notes.txt` – encrypted output file created/updated by the app.

## Requirements

- Java 17+ (or Java 11+ with minor compatibility in some environments).

## Run

```bash
javac src/*.java
java -cp src Main
```

## How It Works

1. App asks for a passphrase once.
2. You choose options from the menu:
   - Write encrypted text
   - Read and decrypt text
   - View raw encrypted file
   - Exit
3. For writes:
   - Plain text is encrypted with AES/GCM.
   - Encrypted payload is saved into `notes.txt`.
4. For reads:
   - App reads `notes.txt` and decrypts with your passphrase.

> If you open `notes.txt` in any editor, you will only see encrypted text.

## Security Note

This is a learning-friendly implementation with solid primitives (AES/GCM + PBKDF2).
For production use, add stronger key management and secret handling policies.
