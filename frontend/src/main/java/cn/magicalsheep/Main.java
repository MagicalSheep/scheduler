package cn.magicalsheep;

import cn.magicalsheep.data.DataSource;
import cn.magicalsheep.frame.Home;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

public class Main {
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
                CompletableFuture.runAsync(DataSource::updateSystemInfo);
                SwingUtilities.invokeLater(() -> home.updateUI(DataSource.getSystemInfo()));
            }
        }, new Date(), 100);



    }
}
