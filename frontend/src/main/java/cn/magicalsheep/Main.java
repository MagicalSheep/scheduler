package cn.magicalsheep;

import cn.magicalsheep.data.DataSource;
import cn.magicalsheep.frame.Home;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class Main {

    private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static void main(String[] args) {
        FlatLightLaf.setup();

        Home home = new Home();
        JFrame frame = new JFrame("Operating system lab");
        frame.setContentPane(home.getRootPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                executor.submit(DataSource::updateSystemInfo);
                SwingUtilities.invokeLater(() -> home.updateUI(DataSource.getSystemInfo()));
            }
        }, new Date(), 700);
    }
}
