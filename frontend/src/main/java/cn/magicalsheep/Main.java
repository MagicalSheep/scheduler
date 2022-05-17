package cn.magicalsheep;

import cn.magicalsheep.frame.Home;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        FlatLightLaf.setup();
        JFrame frame = new JFrame("Flight-ui");
        frame.setContentPane(new Home().getRootPanel());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
