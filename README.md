# Secure Notes Portal

A simple Java-powered web application for writing and reading a `.txt` file where content is securely encrypted and decrypted right in your browser, powered by a Java backend.

## Features

- **Beautiful Modern Web Interface**: Clean, dark-mode glassmorphism styling.
- **End-to-End Encrypted Storage**: Plain text is encrypted with AES/GCM before being stored by the Java backend. 
- **Read & Decrypt**: App decrypts text from the file securely.
- **Zero Frameworks**: Powered purely by Java's built-in `com.sun.net.httpserver.HttpServer`.

## Project Structure

- `src/WebServer.java` – The Java HTTP Backend.
- `src/Main.java` – The legacy TUI if you prefer the terminal.
- `src/FileService.java` – File read/write operations.
- `src/CryptoService.java` – AES-GCM encryption/decryption logic.
- `public/` – Web assets (`index.html`, `style.css`, `script.js`).
- `notes.txt` – The encrypted output file created automatically.

## Requirements

- Java 11 or higher.

## Run

To run the web portal, compile the code and start the Java server:

```bash
# Compile all Java files
javac src/*.java

# Start the web server
java src.WebServer
```

Once running, open your web browser and go to **http://localhost:8080**.

## How It Works

1. Visit **http://localhost:8080** in your browser.
2. In the **Write** tab, insert your strong passphrase and the secret message. The message goes to the Java backend.
3. The Java backend encrypts the message securely and writes it to `notes.txt`.
4. Use the **Read** tab with the same passphrase to decrypt and view your text.
5. You can also view the raw encrypted data directly from the **Raw Data** tab.

## Security Note

This uses robust standards (AES/GCM + PBKDF2), but is meant for learning purposes! Ensure good key management for heavy production use.

## Deployment

The application runs seamlessly in the cloud by splitting the frontend (Vercel) and the Java backend (Render).

### 1. Deploy the Backend (Render)
Make sure your project is pushed to a Git repository, then:
- Create a new "Web Service" on [Render.com](https://render.com/).
- Choose **Docker** as the Runtime / Environment.
- Render will automatically use the `Dockerfile` to compile and start the Java server.
- Copy your live backend URL from Render (e.g., `https://java-backend-api.onrender.com`).

### 2. Connect the Frontend
In `public/script.js`, replace the `HOST` variable at the top of the file to point to your new Render URL:
```javascript
const HOST = 'https://java-backend-api.onrender.com';
```

### 3. Deploy the Frontend (Vercel)
Ensure you have the Vercel CLI installed via npm:
```bash
npm i -g vercel
```
Navigate into the `public` directory and run the deployment:
```bash
cd public
vercel --prod
```