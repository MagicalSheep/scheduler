package cn.magicalsheep.frame;

import javax.swing.*;

public class Home {
    private JPanel rootPanel;
    private JTabbedPane systemInfoTab;
    private JLabel cpuCnt;
    private JLabel pidCnt;
    private JLabel procCnt;
    private JLabel runProcCnt;
    private JLabel jobCnt;
    private JProgressBar systemMemory;
    private JTable poolQueue;
    private JTable jobResQueue;
    private JProgressBar userMemory;
    private JComboBox comboBox1;
    private JLabel processorTaskCnt;
    private JLabel processorCurrentPid;
    private JTable priorityQueue;
    private JTable ioQueue;
    private JTable suspendQueue;
    private JTable userMemoryDetail;
    private JTable systemMemoryDetail;
    private JTable program;
    private JTextField memorySiz;
    private JTextField prior;
    private JButton submit;
    private JButton suspend;

    public JPanel getRootPanel() {
        return rootPanel;
    }
}
