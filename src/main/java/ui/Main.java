package ui;

/**
 * Entry point for the Hospital &amp; Clinic Operations Optimizer console app.
 *
 * <p>Wiring only: it constructs the {@link ConsoleApp} (which owns the repository,
 * the SQLite database, and the menu loop) and runs it. All behaviour lives in
 * {@link ConsoleApp}.</p>
 *
 * <p>Launch with: {@code java -cp out/main;lib/... ui.Main}
 * (or simply {@code bash build.sh run}).</p>
 */
public final class Main {
    public static void main(String[] args) {
        new ConsoleApp().run();
    }
    private Main() {}
}
