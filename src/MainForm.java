import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class MainForm extends JFrame{
    private JFrame frame;
    private TrayIcon trayIcon = new TrayIcon(Objects.requireNonNull(loadImage("/ico.png")), "????");
    private JTextField ipTextField;
    public Boolean server = false;
    public String serverID = "";
    public int timer = 0;

    public MainForm() {

        String ipAddress = loadConfig();
        if (ipAddress != null && validateServer(ipAddress)) {
            //createTrayIcon();
            server = true;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    trayIcon.setImage(loadImage("/ico.png"));
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    trayIcon.displayMessage("短信转发" , "短信转发服务已启用" , TrayIcon.MessageType.NONE);
                    serverID = loadConfig();
                }
            });
        } else {
            createWindow();
            frame.setName("更改服务器ip地址");
        }
    }

    private void createWindow() {
        frame = new JFrame("短信接收转发服务");
        JFrame.setDefaultLookAndFeelDecorated(true);
        frame.setSize(400, 80);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setLocationRelativeTo(null); // ????
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage("/ico.png"));
        JPanel panel = new JPanel();
        ipTextField = new JTextField(20);
        JButton confirmButton = new JButton("确定");

        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ipAddress = ipTextField.getText();
                if (validateServer(ipAddress)) {
                    saveConfig(ipAddress);
                    JOptionPane.showMessageDialog(frame, "已设置服务器IP: " + ipAddress);
                    server = true;
                    frame.setVisible(false); // ????
                    serverID = loadConfig();
                    trayIcon.displayMessage("短信转发" , "短信转发服务已启用！" , TrayIcon.MessageType.NONE);
                    //createTrayIcon(); // ??????
                } else {
                    JOptionPane.showMessageDialog(frame, "请输入正确的IP地址！");
                }
            }
        });

        panel.add(new JLabel("服务端IP地址:"));
        panel.add(ipTextField);
        panel.add(confirmButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.setIconImage(Toolkit.getDefaultToolkit().getImage("/ico.png"));

        frame.setVisible(true);

    }

    private void createTrayIcon() {
        // ????????
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = loadImage("/ico.png");
            if (image == null) {
                System.err.println("Failed to load image.");
                return;
            }

            PopupMenu popup = new PopupMenu();

            MenuItem editItem = new MenuItem("更改服务器ip地址");
            editItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    createWindow();
                }
            });
            popup.add(editItem);

            MenuItem exitItem = new MenuItem("退出应用");
            exitItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            });
            popup.add(exitItem);
            //popup.setFont(new Font("??", Font.PLAIN,15));

            trayIcon = new TrayIcon(image, "短信转发", popup);

            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        createWindow();
                    }
                }
            });
            //trayIcon.setToolTip("a");
            try {
                tray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        } else {
            System.err.println("SystemTray is not supported");
        }
    }

    private String loadConfig() {
        File configFile = new File("config.ini");
        if (configFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(configFile))) {
                String line = reader.readLine();
                if (line != null && line.startsWith("ServerIP=")) {
                    return line.substring("ServerIP=".length());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private boolean validateServer(String ipAddress) {
        try {
            URL url = new URL("http://" + ipAddress);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // ??JSON??
                JSONObject jsonResponse = JSON.parseObject(response.toString());
                String message = jsonResponse.getString("message");
                if ("Hello World".equals(message)) {
                    return true;
                }
            }
        } catch (IOException _) {

        }
        return false;
    }
    private void getDateServer(String ipAddress , TrayIcon trayIcon) {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {

            throw new RuntimeException(e);
        }
        try {

            URL url = new URL("http://" + ipAddress + "/getMsg");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                    //System.out.println(inputLine);
                }
                in.close();
                
                JSONObject jsonResponse = JSON.parseObject(response.toString());
                String message = jsonResponse.getString("message");
                if ("No message in the list".equals(message)) {
                    timer = 0;
                }else {
                    trayIcon.displayMessage("收到新的短信" , jsonResponse.getString("data") , TrayIcon.MessageType.INFO);
                    timer = 0;
                }

            }
        } catch (IOException e) {
            System.out.println(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()) + " [WRONG]     服务端错误或ip错误");
            timer ++;
        }
    }

    private void saveConfig(String ipAddress) {
        try (FileWriter writer = new FileWriter("config.ini")) {
            writer.write("ServerIP=" + ipAddress);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "错误！" + e.getMessage());
        }
    }

    private Image loadImage(String path) {
        URL imageURL = getClass().getResource(path);
        if (imageURL == null) {
            System.err.println("Resource not found: " + path);
            return null;
        }
        return Toolkit.getDefaultToolkit().getImage(imageURL);
    }

    public static void main(String[] args) {
        MainForm form = new MainForm();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                form.createTrayIcon();
            }
        });

        ((Runnable) () -> {
            while (true) {
                while (form.server) {
                    form.getDateServer(form.serverID, form.trayIcon);
                    if (form.timer >= 2) {
                        form.server = false;
                        form.trayIcon.displayMessage("短信转发" , "服务器失效！请重新设置" , TrayIcon.MessageType.WARNING);
                        form.createWindow();
                        break;
                    }
                }
            }

        }).run();
    }
}
