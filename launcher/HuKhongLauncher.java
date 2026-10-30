import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/**
 * Launcher don gian cho Hu Khong Tu Tien Luc. Chay: java launcher/HuKhongLauncher.java (Java 21+).
 * - Doi ten (ghi username.txt)
 * - Choi (gradlew runClient)
 * - Mo may chu ngay trong launcher (log + o go lenh: stop, op <ten>, ...)
 * - Mo may chu o cua so CMD/Terminal rieng
 */
public class HuKhongLauncher {
    static final Path ROOT = findRoot();
    static final boolean WIN = System.getProperty("os.name").toLowerCase().contains("win");
    static Process server;
    static JTextArea log;
    static JTextField cmd;
    static JButton serverBtn;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HuKhongLauncher::ui);
    }

    static Path findRoot() {
        Path p = Paths.get("").toAbsolutePath();
        for (Path q = p; q != null; q = q.getParent()) if (Files.exists(q.resolve("gradlew"))) return q;
        return p;
    }

    static void ui() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        JFrame f = new JFrame("Hư Không Tu Tiên Lục - Launcher");
        f.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        f.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { stopServerThenExit(); }
        });

        JPanel top = new JPanel(new GridBagLayout());
        top.setBorder(new EmptyBorder(10, 10, 6, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.fill = GridBagConstraints.HORIZONTAL;

        JTextField name = new JTextField(readName(), 16);
        JButton save = new JButton("Lưu tên");
        save.addActionListener(e -> { writeName(name.getText().trim()); append("Đã lưu tên: " + readName() + "\n"); });
        c.gridx = 0; c.gridy = 0; top.add(new JLabel("Tên người chơi:"), c);
        c.gridx = 1; c.weightx = 1; top.add(name, c);
        c.gridx = 2; c.weightx = 0; top.add(save, c);

        JButton play = new JButton("▶ Chơi");
        play.addActionListener(e -> { writeName(name.getText().trim()); runDetached("runClient", "Đang mở game (lần đầu có thể mất vài phút)..."); });
        serverBtn = new JButton("Mở máy chủ (trong launcher)");
        serverBtn.addActionListener(e -> toggleServer());
        JButton serverCmd = new JButton("Mở máy chủ (cửa sổ CMD)");
        serverCmd.addActionListener(e -> serverInConsole());
        JButton ips = new JButton("Xem IP");
        ips.addActionListener(e -> showIps());

        JPanel btns = new JPanel(new GridLayout(1, 4, 6, 0));
        btns.add(play); btns.add(serverBtn); btns.add(serverCmd); btns.add(ips);
        c.gridx = 0; c.gridy = 1; c.gridwidth = 3; top.add(btns, c);

        log = new JTextArea(22, 90);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        log.setBackground(new Color(20, 20, 28));
        log.setForeground(new Color(220, 220, 220));
        JScrollPane sp = new JScrollPane(log);

        cmd = new JTextField();
        cmd.setEnabled(false);
        cmd.setToolTipText("Gõ lệnh máy chủ rồi Enter (vd: stop, op Ten, list)");
        cmd.addActionListener(e -> { sendCmd(cmd.getText().trim()); cmd.setText(""); });
        JPanel bottom = new JPanel(new BorderLayout(6, 0));
        bottom.setBorder(new EmptyBorder(4, 10, 10, 10));
        bottom.add(new JLabel("Lệnh máy chủ:"), BorderLayout.WEST);
        bottom.add(cmd, BorderLayout.CENTER);

        f.setLayout(new BorderLayout());
        f.add(top, BorderLayout.NORTH);
        f.add(sp, BorderLayout.CENTER);
        f.add(bottom, BorderLayout.SOUTH);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        append("Thư mục mod: " + ROOT + "\n");
    }

    // ---------------------------------------------------------------- ten
    static String readName() {
        try {
            for (String l : Files.readAllLines(ROOT.resolve("username.txt"), StandardCharsets.UTF_8))
                if (l.trim().toLowerCase().startsWith("username") && l.contains("=")) return l.split("=", 2)[1].trim();
        } catch (IOException ignored) {}
        return "Minhhoangg";
    }

    static void writeName(String n) {
        if (n.isEmpty() || !n.matches("[A-Za-z0-9_]{3,16}")) {
            JOptionPane.showMessageDialog(null, "Tên chỉ gồm chữ không dấu, số, dấu _ và dài 3-16 ký tự.");
            return;
        }
        try { Files.writeString(ROOT.resolve("username.txt"), "userName = " + n + System.lineSeparator(), StandardCharsets.UTF_8); }
        catch (IOException e) { append("Lỗi ghi username.txt: " + e + "\n"); }
    }

    // ---------------------------------------------------------------- tien trinh
    static List<String> gradle(String task) {
        return WIN ? List.of("cmd", "/c", "gradlew.bat", task) : List.of("sh", "./gradlew", task);
    }

    static void runDetached(String task, String msg) {
        append(msg + "\n");
        try {
            Process p = new ProcessBuilder(gradle(task)).directory(ROOT.toFile()).redirectErrorStream(true).start();
            pump(p, "[game] ");
        } catch (IOException e) { append("Không chạy được: " + e + "\n"); }
    }

    static void toggleServer() {
        if (server != null && server.isAlive()) { sendCmd("stop"); return; }
        prepareServerFiles();
        try {
            server = new ProcessBuilder(gradle("runServer")).directory(ROOT.toFile()).redirectErrorStream(true).start();
            append("Đang mở máy chủ... (lần đầu dựng thành mất ~2 phút)\n");
            serverBtn.setText("■ Dừng máy chủ");
            cmd.setEnabled(true);
            pump(server, "");
            new Thread(() -> {
                try { server.waitFor(); } catch (InterruptedException ignored) {}
                SwingUtilities.invokeLater(() -> {
                    append("Máy chủ đã tắt.\n");
                    serverBtn.setText("Mở máy chủ (trong launcher)");
                    cmd.setEnabled(false);
                });
            }).start();
        } catch (IOException e) { append("Không mở được máy chủ: " + e + "\n"); }
    }

    static void sendCmd(String s) {
        if (s.isEmpty() || server == null || !server.isAlive()) return;
        try {
            Writer w = new OutputStreamWriter(server.getOutputStream(), StandardCharsets.UTF_8);
            w.write(s + "\n");
            w.flush();
            append("> " + s + "\n");
        } catch (IOException e) { append("Gửi lệnh lỗi: " + e + "\n"); }
    }

    static void serverInConsole() {
        prepareServerFiles();
        try {
            if (WIN) new ProcessBuilder("cmd", "/c", "start", "\"Hu Khong Server\"", "cmd", "/k", "gradlew.bat runServer")
                    .directory(ROOT.toFile()).start();
            else new ProcessBuilder("osascript", "-e",
                    "tell application \"Terminal\" to do script \"cd '" + ROOT + "' && ./gradlew runServer\"").start();
            append("Đã mở máy chủ ở cửa sổ riêng. Tắt bằng lệnh: stop\n");
        } catch (IOException e) { append("Không mở được cửa sổ: " + e + "\n"); }
    }

    static void prepareServerFiles() {
        try {
            Path dir = ROOT.resolve("run/server");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("eula.txt"), "eula=true\n");
            Path props = dir.resolve("server.properties");
            if (!Files.exists(props)) Files.writeString(props, "online-mode=false\npvp=true\nmotd=Hu Khong Tu Tien\n");
            else {
                String s = Files.readString(props);
                if (!s.contains("online-mode=false")) Files.writeString(props, s.replaceAll("online-mode=\\w+", "online-mode=false"));
            }
        } catch (IOException e) { append("Lỗi chuẩn bị máy chủ: " + e + "\n"); }
    }

    static void pump(Process p, String prefix) {
        Thread t = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("/DEBUG]") || line.contains("[DEBUG]")) continue;
                    String l = prefix + line + "\n";
                    SwingUtilities.invokeLater(() -> append(l));
                }
            } catch (IOException ignored) {}
        });
        t.setDaemon(true);
        t.start();
    }

    static void append(String s) {
        log.append(s);
        if (log.getLineCount() > 3000) {
            try { log.replaceRange("", 0, log.getLineEndOffset(500)); } catch (Exception ignored) {}
        }
        log.setCaretPosition(log.getDocument().getLength());
    }

    static void showIps() {
        StringBuilder sb = new StringBuilder("Người chơi nhập một trong các địa chỉ sau:\n\n");
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses()))
                    if (a instanceof Inet4Address) sb.append(a.getHostAddress()).append(":25565   (").append(ni.getDisplayName()).append(")\n");
            }
        } catch (Exception ignored) {}
        sb.append("\nCùng mạng nhà: IP 192.168.x / 10.x. Radmin VPN: IP 26.x");
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false);
        JOptionPane.showMessageDialog(null, new JScrollPane(ta), "Địa chỉ máy chủ", JOptionPane.INFORMATION_MESSAGE);
    }

    static void stopServerThenExit() {
        if (server != null && server.isAlive()) {
            int r = JOptionPane.showConfirmDialog(null, "Máy chủ đang chạy. Tắt máy chủ (lưu map) rồi thoát?", "Thoát", JOptionPane.YES_NO_OPTION);
            if (r != JOptionPane.YES_OPTION) return;
            sendCmd("stop");
            new Thread(() -> {
                try { server.waitFor(60, java.util.concurrent.TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
                if (server.isAlive()) server.destroy();
                System.exit(0);
            }).start();
            return;
        }
        System.exit(0);
    }
}
