package idv.mark.platform.core.process;

public final class ProcessFixture {
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "flood" -> {
                Thread err = new Thread(() -> { for (int i = 0; i < 12_000; i++) System.err.println("error-" + i + "-" + "x".repeat(100)); });
                err.start();
                for (int i = 0; i < 12_000; i++) System.out.print("[download] " + (i % 100) + "% " + "x".repeat(100) + "\r");
                err.join();
            }
            case "silent" -> Thread.sleep(60_000);
            case "fail" -> { System.err.println("API_KEY=super-secret-value"); System.exit(7); }
            case "giant" -> System.out.print("x".repeat(2_000_000));
            case "child" -> {
                Process child = new ProcessBuilder(java(), "-cp", System.getProperty("java.class.path"), ProcessFixture.class.getName(), "silent").start();
                System.out.println("CHILD=" + child.pid());
                Thread.sleep(60_000);
            }
            default -> System.out.println(String.join("|", args));
        }
    }
    public static String java() { return java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString(); }
}
