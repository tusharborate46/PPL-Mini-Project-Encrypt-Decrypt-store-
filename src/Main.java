import java.util.Scanner;

public class Main {
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String RED = "\u001B[31m";
    private static final String PURPLE = "\u001B[35m";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        FileService fileService = new FileService("notes.txt");
        CryptoService cryptoService = new CryptoService();

        printBanner();
        System.out.print(CYAN + "Enter passphrase: " + RESET);
        String passphrase = scanner.nextLine();

        if (passphrase.isBlank()) {
            System.out.println(RED + "Passphrase cannot be empty. Exiting." + RESET);
            return;
        }

        boolean running = true;
        while (running) {
            printMenu(fileService.path());
            System.out.print(CYAN + "Choose an option: " + RESET);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> writeFlow(scanner, fileService, cryptoService, passphrase);
                case "2" -> readFlow(fileService, cryptoService, passphrase);
                case "3" -> rawViewFlow(fileService);
                case "4" -> {
                    System.out.println(GREEN + "Goodbye!" + RESET);
                    running = false;
                }
                default -> System.out.println(RED + "Invalid option. Try 1-4." + RESET);
            }

            if (running) {
                System.out.print(YELLOW + "\nPress Enter to continue..." + RESET);
                scanner.nextLine();
            }
        }
    }

    private static void writeFlow(Scanner scanner, FileService fileService, CryptoService cryptoService, String passphrase) {
        boxedTitle("Write Encrypted Text");
        System.out.println("Type your message (single line):");
        String plain = scanner.nextLine();

        if (plain.isBlank()) {
            System.out.println(RED + "Nothing to save. Empty input." + RESET);
            return;
        }

        String encrypted = cryptoService.encrypt(plain, passphrase);
        fileService.write(encrypted);
        System.out.println(GREEN + "Saved successfully (encrypted) to: " + fileService.path() + RESET);
    }

    private static void readFlow(FileService fileService, CryptoService cryptoService, String passphrase) {
        boxedTitle("Read Decrypted Text");

        if (!fileService.exists()) {
            System.out.println(RED + "No file found yet. Write something first." + RESET);
            return;
        }

        String payload = fileService.read();
        if (payload.isBlank()) {
            System.out.println(RED + "File is empty." + RESET);
            return;
        }

        try {
            String plain = cryptoService.decrypt(payload, passphrase);
            System.out.println(GREEN + "Decrypted content:" + RESET);
            System.out.println(PURPLE + "--------------------------------" + RESET);
            System.out.println(plain);
            System.out.println(PURPLE + "--------------------------------" + RESET);
        } catch (RuntimeException e) {
            System.out.println(RED + e.getMessage() + RESET);
        }
    }

    private static void rawViewFlow(FileService fileService) {
        boxedTitle("Raw File View (Encrypted)");

        if (!fileService.exists()) {
            System.out.println(RED + "No file found yet." + RESET);
            return;
        }

        String raw = fileService.read();
        if (raw.isBlank()) {
            System.out.println(RED + "File is empty." + RESET);
            return;
        }

        System.out.println(YELLOW + raw + RESET);
    }

    private static void printBanner() {
        System.out.println(PURPLE + "╔═══════════════════════════════════════════════╗");
        System.out.println("║       🔐 Java TUI Encrypted Notes Tool        ║");
        System.out.println("╚═══════════════════════════════════════════════╝" + RESET);
    }

    private static void printMenu(String path) {
        boxedTitle("Main Menu");
        System.out.println("1) Write text (encrypted)");
        System.out.println("2) Read text (decrypt in TUI)");
        System.out.println("3) View raw file content");
        System.out.println("4) Exit");
        System.out.println("\nFile: " + path);
    }

    private static void boxedTitle(String title) {
        String border = "═".repeat(Math.max(10, title.length() + 4));
        System.out.println("\n" + CYAN + "╔" + border + "╗");
        System.out.println("║  " + title + "  ║");
        System.out.println("╚" + border + "╝" + RESET);
    }
}
