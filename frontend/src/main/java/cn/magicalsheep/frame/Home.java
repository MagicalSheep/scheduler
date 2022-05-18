package cn.magicalsheep.frame;

import cn.magicalsheep.data.DataSource;
import cn.magicalsheep.model.*;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

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
    private JComboBox<String> comboBox1;
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
    private JLabel cpuName;
    private JLabel usrMemLabel;
    private JLabel sysMemLabel;
    private JButton addCode;
    private JLabel curCp;
    private JLabel priorLabel;

    private static final DefaultTableModel poolQueueModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel jobResModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel priorityQueueModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel ioQueueModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel suspendQueueModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel userMemoryModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel systemMemoryModel = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private static final DefaultTableModel jobCodeModel = new DefaultTableModel();

    private static final String[] poolQueueColumnNames = {"作业ID", "所需内存大小（字节）", "优先级"};
    private static final String[] jobResColumnNames = {"作业ID", "作业状态", "状态信息"};
    private static final String[] priorityQueueColumnNames = {"进程号", "作业ID", "进程状态", "优先级", "进程空间起始地址", "进程空间终止地址", "指令地址偏移量"};
    private static final String[] ioQueueColumnNames = {"进程号", "作业ID", "进程状态", "优先级", "进程空间起始地址", "进程空间终止地址", "指令地址偏移量"};
    private static final String[] suspendQueueColumnNames = {"进程号", "作业ID", "进程状态", "优先级", "指令地址偏移量", "所属处理机号"};
    private static final String[] userMemoryColumnNames = {"所属进程号", "块起始地址", "块终止地址", "大小（字节数）"};
    private static final String[] systemMemoryColumnNames = {"所属进程号", "块起始地址", "块终止地址", "大小（字节数）"};
    private static final String[] jobCodeColumnNames = {"指令类型", "指令执行时长"};

    @SuppressWarnings("unchecked")
    public Home() {
        JMenuItem unsuspendItem = new JMenuItem("解除挂起");
        unsuspendItem.addActionListener(e -> {
            int pid;
            try {
                pid = (int) suspendQueue.getValueAt(suspendQueue.getSelectedRow(), 0);
            } catch (Exception ee) {
                return;
            }
            DataSource.unsuspendProcess(pid);
        });
        JPopupMenu unsuspendMenu = new JPopupMenu();
        unsuspendMenu.add(unsuspendItem);

        poolQueueModel.setColumnIdentifiers(poolQueueColumnNames);
        jobResModel.setColumnIdentifiers(jobResColumnNames);
        priorityQueueModel.setColumnIdentifiers(priorityQueueColumnNames);
        ioQueueModel.setColumnIdentifiers(ioQueueColumnNames);
        suspendQueueModel.setColumnIdentifiers(suspendQueueColumnNames);
        userMemoryModel.setColumnIdentifiers(userMemoryColumnNames);
        systemMemoryModel.setColumnIdentifiers(systemMemoryColumnNames);
        jobCodeModel.setColumnIdentifiers(jobCodeColumnNames);

        poolQueue.setModel(poolQueueModel);
        jobResQueue.setModel(jobResModel);
        priorityQueue.setModel(priorityQueueModel);
        ioQueue.setModel(ioQueueModel);
        suspendQueue.setModel(suspendQueueModel);
        userMemoryDetail.setModel(userMemoryModel);
        systemMemoryDetail.setModel(systemMemoryModel);
        program.setModel(jobCodeModel);

        poolQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobResQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobResQueue.setAutoCreateRowSorter(true);
        priorityQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ioQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suspendQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suspendQueue.setComponentPopupMenu(unsuspendMenu);
        userMemoryDetail.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        systemMemoryDetail.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        program.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        comboBox1.addItemListener(e -> {
            if (comboBox1.getItemCount() == 0)
                cpuName.setText("无处理机");
            else
                cpuName.setText((String) comboBox1.getSelectedItem());
            updateProcessor(DataSource.getSystemInfo());
        });
        suspend.addActionListener(e -> {
            int cpuId = comboBox1.getSelectedIndex();
            DataSource.suspendProcess(cpuId);
        });

        TableColumn column = program.getColumnModel().getColumn(0);
        JComboBox<String> typeBox = new JComboBox<>();
        typeBox.addItem("普通指令");
        typeBox.addItem("I/O指令");
        column.setCellEditor(new DefaultCellEditor(typeBox));

        addCode.addActionListener(e -> {
            Object[] info = {"普通指令", "5"};
            jobCodeModel.addRow(info);
            jobCodeModel.fireTableDataChanged();
            program.updateUI();
        });
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> {
            int selectedRow = program.getSelectedRow();
            try {
                jobCodeModel.removeRow(selectedRow);
            } catch (Exception ignored) {
            }
            jobCodeModel.fireTableDataChanged();
            program.updateUI();
        });
        JPopupMenu deleteMenu = new JPopupMenu();
        deleteMenu.add(deleteItem);
        program.setComponentPopupMenu(deleteMenu);

        submit.addActionListener(e -> {
            String sizStr = memorySiz.getText();
            if (sizStr == null || sizStr.isEmpty()) {
                JOptionPane.showConfirmDialog(rootPanel, "所需内存大小不能为空", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                return;
            }
            int siz;
            try {
                siz = Integer.parseInt(sizStr);
            } catch (NumberFormatException ee) {
                JOptionPane.showConfirmDialog(rootPanel, "无效的内存大小", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                return;
            }
            String priorStr = prior.getText();
            if (priorStr == null || priorStr.isEmpty()) {
                JOptionPane.showConfirmDialog(rootPanel, "优先级不能为空", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                return;
            }
            int prior;
            try {
                prior = Integer.parseInt(priorStr);
            } catch (NumberFormatException ee) {
                JOptionPane.showConfirmDialog(rootPanel, "无效的优先级", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (jobCodeModel.getRowCount() == 0) {
                JOptionPane.showConfirmDialog(rootPanel, "作业代码段不能为空", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<Command> commands = new ArrayList<>();
            for (Vector<String> vec : jobCodeModel.getDataVector()) {
                int type;
                long times;
                if (vec.get(0).equals("I/O指令"))
                    type = 1;
                else type = 0;
                try {
                    times = Long.parseLong(vec.get(1));
                } catch (NumberFormatException ee) {
                    JOptionPane.showConfirmDialog(rootPanel, "执行时间无效", "错误", JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                commands.add(new Command(type, times));
            }
            DataSource.submitJob(siz, prior, commands);
        });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void updateProcessorSelector(int cpuCnt) {
        if (cpuCnt == comboBox1.getItemCount())
            return;
        comboBox1.removeAllItems();
        for (int i = 0; i < cpuCnt; i++)
            comboBox1.addItem("CPU" + i);
    }

    private void updatePoolQueue(List<Job> jobList) {
        int s = poolQueue.getSelectedRow();
        poolQueueModel.getDataVector().removeAllElements();
        poolQueue.clearSelection();
        if (jobList == null)
            return;
        s = Math.min(s, jobList.size());
        for (Job job : jobList) {
            Object[] info = new Object[poolQueueColumnNames.length];
            info[0] = job.getId();
            info[1] = job.getSiz();
            info[2] = job.getPrior();
            poolQueueModel.addRow(info);
        }
        poolQueueModel.fireTableDataChanged();
        try {
            poolQueue.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        poolQueue.updateUI();
    }

    private void updateJobResQueue(List<Job> jobResList) {
        int s = jobResQueue.getSelectedRow();
        jobResModel.getDataVector().removeAllElements();
        if (jobResList == null)
            return;
        s = Math.min(s, jobResList.size());
        for (Job job : jobResList) {
            Object[] info = new Object[jobResColumnNames.length];
            info[0] = job.getId();
            info[1] = switch (job.getStatus()) {
                case JOB_READY -> "就绪";
                case JOB_FAILED -> "失败";
                case JOB_RUNNING -> "运行中";
                case JOB_COMPLETED -> "已完成";
            };
            info[2] = job.getMsg();
            jobResModel.addRow(info);
        }
        jobResModel.fireTableDataChanged();
        try {
            jobResQueue.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        jobResQueue.updateUI();
    }

    private void updateSuspendQueue(List<Task> suspendList) {
        int s = suspendQueue.getSelectedRow();
        suspendQueueModel.getDataVector().removeAllElements();
        if (suspendList == null)
            return;
        s = Math.min(s, suspendList.size());
        for (Task task : suspendList) {
            Object[] info = new Object[suspendQueueColumnNames.length];
            info[0] = task.getPid();
            info[1] = task.getJob();
            info[2] = switch (task.getStatus()) {
                case TASK_READY -> "就绪";
                case TASK_RUNNING -> "运行";
                case TASK_STOPPED -> "停止";
                case TASK_SUSPEND -> "挂起";
                case TASK_IO_SUSPEND -> "阻塞挂起";
                case TASK_INTERRUPTIBLE -> "阻塞";
            };
            info[3] = task.getPrior();
            info[4] = task.getCp();
            info[5] = task.getProcessor();
            suspendQueueModel.addRow(info);
        }
        suspendQueueModel.fireTableDataChanged();
        try {
            suspendQueue.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        suspendQueue.updateUI();
    }

    private void updateUserMemory(long maxUsrMem, List<Memory> usrMemoryList) {
        int s = userMemoryDetail.getSelectedRow();
        userMemoryModel.getDataVector().removeAllElements();
        if (usrMemoryList == null)
            return;
        s = Math.min(s, usrMemoryList.size());
        long used = 0;
        for (Memory memory : usrMemoryList) {
            Object[] info = new Object[userMemoryColumnNames.length];
            if (memory.getPid() == -1) info[0] = "空闲";
            else info[0] = memory.getPid();
            info[1] = memory.getSt();
            info[2] = memory.getEd();
            info[3] = Long.parseLong(memory.getEd().substring(2), 16) -
                    Long.parseLong(memory.getSt().substring(2), 16);
            if (memory.getPid() != -1)
                used += (long) info[3];
            userMemoryModel.addRow(info);
        }
        userMemoryModel.fireTableDataChanged();
        try {
            userMemoryDetail.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        userMemoryDetail.updateUI();
        if (maxUsrMem != 0) {
            userMemory.setValue((int) (((1.0 * used) / maxUsrMem) * 100));
            usrMemLabel.setText(String.format("（%d Bytes / %d Bytes）", used, maxUsrMem));
        }
    }

    private void updateSystemMemory(long maxSysMem, List<Memory> sysMemoryList) {
        int s = systemMemoryDetail.getSelectedRow();
        systemMemoryModel.getDataVector().removeAllElements();
        if (sysMemoryList == null)
            return;
        s = Math.min(s, sysMemoryList.size());
        long used = 0;
        for (Memory memory : sysMemoryList) {
            Object[] info = new Object[systemMemoryColumnNames.length];
            if (memory.getPid() == -1) info[0] = "空闲";
            else info[0] = memory.getPid();
            info[1] = memory.getSt();
            info[2] = memory.getEd();
            info[3] = Long.parseLong(memory.getEd().substring(2), 16) -
                    Long.parseLong(memory.getSt().substring(2), 16);
            if (memory.getPid() != -1)
                used += (long) info[3];
            systemMemoryModel.addRow(info);
        }
        systemMemoryModel.fireTableDataChanged();
        try {
            systemMemoryDetail.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        systemMemoryDetail.updateUI();
        if (maxSysMem != 0) {
            systemMemory.setValue((int) (((1.0 * used) / maxSysMem) * 100));
            sysMemLabel.setText(String.format("（%d Bytes / %d Bytes）", used, maxSysMem));
        }
    }

    private void updateReadyQueue(List<Task> queue) {
        int s = priorityQueue.getSelectedRow();
        priorityQueueModel.getDataVector().removeAllElements();
        if (queue == null)
            return;
        s = Math.min(s, queue.size());
        for (Task task : queue) {
            Object[] info = new Object[priorityQueueColumnNames.length];
            info[0] = task.getPid();
            info[1] = task.getJob();
            info[2] = switch (task.getStatus()) {
                case TASK_READY -> "就绪";
                case TASK_RUNNING -> "运行";
                case TASK_STOPPED -> "停止";
                case TASK_SUSPEND -> "挂起";
                case TASK_IO_SUSPEND -> "阻塞挂起";
                case TASK_INTERRUPTIBLE -> "阻塞";
            };
            info[3] = task.getPrior();
            info[4] = task.getSt();
            info[5] = task.getEd();
            info[6] = task.getCp();
            priorityQueueModel.addRow(info);
        }
        priorityQueueModel.fireTableDataChanged();
        try {
            priorityQueue.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        priorityQueue.updateUI();
    }

    private void updateIoQueue(List<Task> queue) {
        int s = ioQueue.getSelectedRow();
        ioQueueModel.getDataVector().removeAllElements();
        if (queue == null)
            return;
        s = Math.min(s, queue.size());
        for (Task task : queue) {
            Object[] info = new Object[ioQueueColumnNames.length];
            info[0] = task.getPid();
            info[1] = task.getJob();
            info[2] = switch (task.getStatus()) {
                case TASK_READY -> "就绪";
                case TASK_RUNNING -> "运行";
                case TASK_STOPPED -> "停止";
                case TASK_SUSPEND -> "挂起";
                case TASK_IO_SUSPEND -> "阻塞挂起";
                case TASK_INTERRUPTIBLE -> "阻塞";
            };
            info[3] = task.getPrior();
            info[4] = task.getSt();
            info[5] = task.getEd();
            info[6] = task.getCp();
            ioQueueModel.addRow(info);
        }
        ioQueueModel.fireTableDataChanged();
        try {
            ioQueue.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        ioQueue.updateUI();
    }

    private void updateProcessor(SystemInfo systemInfo) {
        if (comboBox1.getItemCount() == 0) return;
        int cpu = comboBox1.getSelectedIndex();
        Processor processor = systemInfo.getProcessors().get(cpu);
        if (processor == null) return;
        processorTaskCnt.setText(String.valueOf(processor.getTaskCnt()));
        if (processor.getCurPid() == -1)
            processorCurrentPid.setText("空闲");
        else
            processorCurrentPid.setText(String.valueOf(processor.getCurPid()));
        if (processor.getCurCp() == -1)
            curCp.setText("0");
        else
            curCp.setText(String.valueOf(processor.getCurCp()));
        updateReadyQueue(processor.getQueue());
        updateIoQueue(processor.getIoQueue());
    }

    public void updateUI(SystemInfo systemInfo) {
        cpuCnt.setText(String.valueOf(systemInfo.getCpuCnt()));
        pidCnt.setText(String.valueOf(systemInfo.getPidCnt()));
        procCnt.setText(String.valueOf(systemInfo.getProcCnt()));
        runProcCnt.setText(String.valueOf(systemInfo.getRunProcCnt()));
        jobCnt.setText(String.valueOf(systemInfo.getJobCnt()));
        priorLabel.setText("优先级（0 - " + systemInfo.getMaxPrior() + "）");

        SwingUtilities.invokeLater(() -> updateProcessorSelector(systemInfo.getCpuCnt()));
        SwingUtilities.invokeLater(() -> updatePoolQueue(systemInfo.getJobList()));
        SwingUtilities.invokeLater(() -> updateJobResQueue(systemInfo.getJobResList()));
        SwingUtilities.invokeLater(() -> updateSuspendQueue(systemInfo.getSuspendQueue()));
        SwingUtilities.invokeLater(() -> updateUserMemory(systemInfo.getMaxUsrMem(), systemInfo.getUsrMemory()));
        SwingUtilities.invokeLater(() -> updateSystemMemory(systemInfo.getMaxSysMem(), systemInfo.getSysMemory()));
        updateProcessor(systemInfo);

    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootPanel = new JPanel();
        rootPanel.setLayout(new FormLayout("fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        systemInfoTab = new JTabbedPane();
        CellConstraints cc = new CellConstraints();
        rootPanel.add(systemInfoTab, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:d:grow", "center:195px:noGrow,top:5px:noGrow,center:d:grow"));
        systemInfoTab.addTab("系统及作业信息", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,center:max(d;4px):grow"));
        panel1.add(panel2, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:299px:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel2.add(panel3, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label1 = new JLabel();
        Font label1Font = this.$$$getFont$$$(null, -1, 20, label1.getFont());
        if (label1Font != null) label1.setFont(label1Font);
        label1.setText("系统信息");
        panel3.add(label1, cc.xy(3, 3));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,center:max(d;4px):grow"));
        panel2.add(panel4, cc.xy(1, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:m:noGrow,left:5px:noGrow,fill:d:grow,left:5px:noGrow,fill:m:noGrow,left:5px:noGrow,fill:d:grow,left:5px:noGrow,fill:d:noGrow,left:5px:noGrow,fill:d:grow,left:5px:noGrow,fill:m:noGrow,left:5px:noGrow,fill:d:grow,left:5px:noGrow,fill:m:noGrow,left:5px:noGrow,fill:d:grow,left:8px:noGrow,fill:max(d;4px):noGrow", "center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel4.add(panel5, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label2 = new JLabel();
        label2.setText("CPU数：");
        panel5.add(label2, cc.xy(3, 1));
        cpuCnt = new JLabel();
        cpuCnt.setText("0");
        panel5.add(cpuCnt, cc.xy(5, 1));
        final JLabel label3 = new JLabel();
        label3.setText("进程号：");
        panel5.add(label3, cc.xy(7, 1));
        pidCnt = new JLabel();
        pidCnt.setText("0");
        panel5.add(pidCnt, cc.xy(9, 1));
        final JLabel label4 = new JLabel();
        label4.setText("允许道数：");
        panel5.add(label4, cc.xy(11, 1));
        procCnt = new JLabel();
        procCnt.setText("0");
        panel5.add(procCnt, cc.xy(13, 1));
        final JLabel label5 = new JLabel();
        label5.setText("正在运行进程数：");
        panel5.add(label5, cc.xy(15, 1));
        runProcCnt = new JLabel();
        runProcCnt.setText("0");
        panel5.add(runProcCnt, cc.xy(17, 1));
        final JLabel label6 = new JLabel();
        label6.setText("作业数：");
        panel5.add(label6, cc.xy(19, 1));
        jobCnt = new JLabel();
        jobCnt.setText("0");
        panel5.add(jobCnt, cc.xy(21, 1));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:5px:noGrow,center:max(d;4px):grow,top:4dlu:noGrow,center:d:noGrow,top:5px:noGrow,center:max(d;4px):grow"));
        panel4.add(panel6, cc.xy(1, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:noGrow,left:4px:noGrow,fill:max(d;4px):grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:noGrow"));
        panel6.add(panel7, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label7 = new JLabel();
        label7.setText("系统空间内存");
        panel7.add(label7, cc.xy(3, 1));
        sysMemLabel = new JLabel();
        sysMemLabel.setText("（0 Bytes / 0 Bytes）");
        panel7.add(sysMemLabel, cc.xy(5, 1));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel6.add(panel8, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        systemMemory = new JProgressBar();
        systemMemory.setPreferredSize(new Dimension(146, 4));
        systemMemory.setStringPainted(true);
        systemMemory.setValue(0);
        panel8.add(systemMemory, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel6.add(panel9, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.FILL));
        userMemory = new JProgressBar();
        userMemory.setPreferredSize(new Dimension(146, 4));
        userMemory.setStringPainted(true);
        userMemory.setValue(0);
        panel9.add(userMemory, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:noGrow,left:4px:noGrow,fill:max(d;4px):grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:noGrow"));
        panel6.add(panel10, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label8 = new JLabel();
        label8.setText("用户空间内存");
        panel10.add(label8, cc.xy(3, 1));
        usrMemLabel = new JLabel();
        usrMemLabel.setText("（0 Bytes / 0 Bytes）");
        panel10.add(usrMemLabel, cc.xy(5, 1));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new FormLayout("fill:d:grow", "center:m:noGrow,center:max(d;4px):grow"));
        panel1.add(panel11, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel11.add(panel12, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label9 = new JLabel();
        Font label9Font = this.$$$getFont$$$(null, -1, 20, label9.getFont());
        if (label9Font != null) label9.setFont(label9Font);
        label9.setText("作业信息");
        panel12.add(label9, cc.xy(3, 3));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:8px:noGrow,fill:max(d;4px):noGrow", "center:78px:grow"));
        panel11.add(panel13, cc.xy(1, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JSplitPane splitPane1 = new JSplitPane();
        splitPane1.setContinuousLayout(false);
        splitPane1.setDividerLocation(400);
        panel13.add(splitPane1, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new FormLayout("fill:d:grow", "center:m:noGrow,center:d:grow"));
        splitPane1.setLeftComponent(panel14);
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new FormLayout("fill:d:grow", "center:26px:grow"));
        panel14.add(panel15, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label10 = new JLabel();
        label10.setText("后备队列");
        panel15.add(label10, cc.xy(1, 1));
        final JScrollPane scrollPane1 = new JScrollPane();
        scrollPane1.setEnabled(true);
        panel14.add(scrollPane1, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        poolQueue = new JTable();
        scrollPane1.setViewportView(poolQueue);
        final JPanel panel16 = new JPanel();
        panel16.setLayout(new FormLayout("fill:d:grow", "center:26px:noGrow,center:d:grow"));
        splitPane1.setRightComponent(panel16);
        final JPanel panel17 = new JPanel();
        panel17.setLayout(new FormLayout("fill:d:grow", "center:26px:grow"));
        panel16.add(panel17, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label11 = new JLabel();
        label11.setText("完成队列");
        panel17.add(label11, cc.xy(1, 1));
        final JScrollPane scrollPane2 = new JScrollPane();
        panel16.add(scrollPane2, cc.xy(1, 2, CellConstraints.FILL, CellConstraints.FILL));
        jobResQueue = new JTable();
        scrollPane2.setViewportView(jobResQueue);
        final JPanel panel18 = new JPanel();
        panel18.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow"));
        systemInfoTab.addTab("进程调度信息", panel18);
        final JSplitPane splitPane2 = new JSplitPane();
        splitPane2.setDividerLocation(350);
        splitPane2.setOrientation(0);
        panel18.add(splitPane2, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel19 = new JPanel();
        panel19.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:78px:noGrow,top:5px:noGrow,center:max(d;4px):grow"));
        splitPane2.setLeftComponent(panel19);
        final JPanel panel20 = new JPanel();
        panel20.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:33px:noGrow,top:4px:noGrow,center:d:grow"));
        panel19.add(panel20, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel21 = new JPanel();
        panel21.setLayout(new FormLayout("fill:d:grow,left:5px:noGrow,fill:100px:noGrow", "center:d:noGrow"));
        panel20.add(panel21, cc.xy(1, 3));
        cpuName = new JLabel();
        Font cpuNameFont = this.$$$getFont$$$(null, -1, 20, cpuName.getFont());
        if (cpuNameFont != null) cpuName.setFont(cpuNameFont);
        cpuName.setText("无处理机");
        panel21.add(cpuName, cc.xy(1, 1));
        comboBox1 = new JComboBox();
        panel21.add(comboBox1, cc.xy(3, 1));
        final JPanel panel22 = new JPanel();
        panel22.setLayout(new FormLayout("fill:72px:noGrow,left:5px:noGrow,fill:50px:noGrow,left:5px:noGrow,fill:107px:noGrow,left:5px:noGrow,fill:50px:noGrow,left:4px:noGrow,fill:d:noGrow,left:4px:noGrow,fill:60px:noGrow,left:4px:noGrow,fill:81px:noGrow,left:4px:noGrow,fill:max(d;4px):grow", "center:d:grow"));
        panel20.add(panel22, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label12 = new JLabel();
        label12.setText("处理进程数：");
        panel22.add(label12, cc.xy(1, 1));
        processorTaskCnt = new JLabel();
        processorTaskCnt.setText("0");
        panel22.add(processorTaskCnt, cc.xy(3, 1));
        final JLabel label13 = new JLabel();
        label13.setText("当前运行进程PID：");
        panel22.add(label13, cc.xy(5, 1));
        processorCurrentPid = new JLabel();
        processorCurrentPid.setText("0");
        panel22.add(processorCurrentPid, cc.xy(7, 1));
        suspend = new JButton();
        suspend.setText("挂起");
        panel22.add(suspend, cc.xy(13, 1));
        final JLabel label14 = new JLabel();
        label14.setText("当前运行进程指令偏移量：");
        panel22.add(label14, cc.xy(9, 1));
        curCp = new JLabel();
        curCp.setText("0");
        panel22.add(curCp, cc.xy(11, 1));
        final JSplitPane splitPane3 = new JSplitPane();
        splitPane3.setDividerLocation(400);
        panel19.add(splitPane3, cc.xy(3, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel23 = new JPanel();
        panel23.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:5px:noGrow,center:d:grow"));
        splitPane3.setLeftComponent(panel23);
        final JPanel panel24 = new JPanel();
        panel24.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel23.add(panel24, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label15 = new JLabel();
        label15.setText("就绪队列");
        panel24.add(label15, cc.xy(1, 1));
        final JScrollPane scrollPane3 = new JScrollPane();
        panel23.add(scrollPane3, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));
        priorityQueue = new JTable();
        scrollPane3.setViewportView(priorityQueue);
        final JPanel panel25 = new JPanel();
        panel25.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:4dlu:noGrow,center:d:grow"));
        splitPane3.setRightComponent(panel25);
        final JPanel panel26 = new JPanel();
        panel26.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel25.add(panel26, cc.xy(1, 1));
        final JLabel label16 = new JLabel();
        label16.setText("I/O队列");
        panel26.add(label16, cc.xy(1, 1));
        final JScrollPane scrollPane4 = new JScrollPane();
        panel25.add(scrollPane4, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));
        ioQueue = new JTable();
        scrollPane4.setViewportView(ioQueue);
        final JPanel panel27 = new JPanel();
        panel27.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,top:5px:noGrow,center:max(d;4px):grow"));
        splitPane2.setRightComponent(panel27);
        final JPanel panel28 = new JPanel();
        panel28.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel27.add(panel28, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label17 = new JLabel();
        Font label17Font = this.$$$getFont$$$(null, -1, 20, label17.getFont());
        if (label17Font != null) label17.setFont(label17Font);
        label17.setText("挂起进程列表");
        panel28.add(label17, cc.xy(3, 1));
        final JPanel panel29 = new JPanel();
        panel29.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:grow"));
        panel27.add(panel29, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JScrollPane scrollPane5 = new JScrollPane();
        panel29.add(scrollPane5, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
        suspendQueue = new JTable();
        suspendQueue.setAutoCreateRowSorter(false);
        scrollPane5.setViewportView(suspendQueue);
        final JPanel panel30 = new JPanel();
        panel30.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow"));
        systemInfoTab.addTab("内存信息", panel30);
        final JSplitPane splitPane4 = new JSplitPane();
        splitPane4.setDividerLocation(300);
        splitPane4.setOrientation(0);
        panel30.add(splitPane4, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel31 = new JPanel();
        panel31.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:noGrow,center:max(d;4px):grow"));
        splitPane4.setLeftComponent(panel31);
        final JPanel panel32 = new JPanel();
        panel32.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:28px:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel31.add(panel32, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label18 = new JLabel();
        Font label18Font = this.$$$getFont$$$(null, -1, 20, label18.getFont());
        if (label18Font != null) label18.setFont(label18Font);
        label18.setText("用户空间 内存块分布");
        panel32.add(label18, cc.xy(1, 3));
        final JPanel panel33 = new JPanel();
        panel33.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel31.add(panel33, cc.xy(3, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JScrollPane scrollPane6 = new JScrollPane();
        panel33.add(scrollPane6, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        userMemoryDetail = new JTable();
        scrollPane6.setViewportView(userMemoryDetail);
        final JPanel panel34 = new JPanel();
        panel34.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:d:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:d:noGrow,center:max(d;4px):grow"));
        splitPane4.setRightComponent(panel34);
        final JPanel panel35 = new JPanel();
        panel35.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel34.add(panel35, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label19 = new JLabel();
        Font label19Font = this.$$$getFont$$$(null, -1, 20, label19.getFont());
        if (label19Font != null) label19.setFont(label19Font);
        label19.setText("系统空间 内存块分布");
        panel35.add(label19, cc.xy(1, 3));
        final JPanel panel36 = new JPanel();
        panel36.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel34.add(panel36, cc.xy(3, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JScrollPane scrollPane7 = new JScrollPane();
        panel36.add(scrollPane7, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        systemMemoryDetail = new JTable();
        systemMemoryDetail.setFocusable(true);
        scrollPane7.setViewportView(systemMemoryDetail);
        final JPanel panel37 = new JPanel();
        panel37.setLayout(new FormLayout("fill:8px:noGrow,left:8px:noGrow,fill:393px:grow,left:4dlu:noGrow,fill:max(d;4px):noGrow", "center:max(d;4px):grow"));
        systemInfoTab.addTab("提交作业", panel37);
        final JSplitPane splitPane5 = new JSplitPane();
        splitPane5.setDividerLocation(500);
        panel37.add(splitPane5, cc.xy(3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JPanel panel38 = new JPanel();
        panel38.setLayout(new FormLayout("fill:d:grow", "center:d:noGrow,center:d:grow"));
        splitPane5.setLeftComponent(panel38);
        final JPanel panel39 = new JPanel();
        panel39.setLayout(new FormLayout("fill:d:noGrow,left:4dlu:noGrow,fill:max(d;4px):grow,left:5dlu:noGrow,fill:d:noGrow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel38.add(panel39, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label20 = new JLabel();
        label20.setText("执行代码段");
        panel39.add(label20, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        addCode = new JButton();
        addCode.setText("添加");
        panel39.add(addCode, cc.xy(5, 3));
        final JPanel panel40 = new JPanel();
        panel40.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel38.add(panel40, cc.xy(1, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JScrollPane scrollPane8 = new JScrollPane();
        panel40.add(scrollPane8, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        program = new JTable();
        scrollPane8.setViewportView(program);
        final JPanel panel41 = new JPanel();
        panel41.setLayout(new FormLayout("fill:d:grow", "center:41px:noGrow,center:35px:noGrow,top:8px:noGrow,center:d:noGrow,center:d:noGrow,top:4dlu:noGrow,center:max(d;4px):grow"));
        splitPane5.setRightComponent(panel41);
        final JPanel panel42 = new JPanel();
        panel42.setLayout(new FormLayout("fill:d:grow", "center:max(d;4px):noGrow,top:4dlu:noGrow,center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel41.add(panel42, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
        final JLabel label21 = new JLabel();
        label21.setText("所需内存大小（字节）");
        panel42.add(label21, cc.xy(1, 3));
        final JPanel panel43 = new JPanel();
        panel43.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel41.add(panel43, cc.xy(1, 2, CellConstraints.DEFAULT, CellConstraints.FILL));
        memorySiz = new JTextField();
        panel43.add(memorySiz, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JPanel panel44 = new JPanel();
        panel44.setLayout(new FormLayout("fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        panel41.add(panel44, cc.xy(1, 4, CellConstraints.DEFAULT, CellConstraints.FILL));
        priorLabel = new JLabel();
        priorLabel.setText("优先级（0 - ?）");
        panel44.add(priorLabel, cc.xy(1, 1));
        final JPanel panel45 = new JPanel();
        panel45.setLayout(new FormLayout("fill:d:grow", "center:d:grow"));
        panel41.add(panel45, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.FILL));
        prior = new JTextField();
        panel45.add(prior, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        final JPanel panel46 = new JPanel();
        panel46.setLayout(new FormLayout("fill:d:grow", "center:d:grow,center:d:noGrow"));
        panel41.add(panel46, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.FILL));
        submit = new JButton();
        submit.setText("提交");
        panel46.add(submit, cc.xy(1, 2));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }
}
