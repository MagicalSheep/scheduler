package cn.magicalsheep.frame;

import cn.magicalsheep.data.DataSource;
import cn.magicalsheep.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

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
    private static final String[] suspendQueueColumnNames = {"进程号", "作业ID", "进程状态", "优先级", "指令地址偏移量"};
    private static final String[] userMemoryColumnNames = {"所属进程号", "块起始地址", "块终止地址", "长度"};
    private static final String[] systemMemoryColumnNames = {"所属进程号", "块起始地址", "块终止地址", "长度"};
    private static final String[] jobCodeColumnNames = {"指令类型", "指令执行时长"};

    public Home() {
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
        priorityQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ioQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        suspendQueue.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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
            suspendQueueModel.addRow(info);
        }
        suspendQueueModel.fireTableDataChanged();
        try {
            suspendQueue.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        suspendQueue.updateUI();
    }

    private void updateUserMemory(List<Memory> usrMemoryList) {
        int s = userMemoryDetail.getSelectedRow();
        userMemoryModel.getDataVector().removeAllElements();
        if (usrMemoryList == null)
            return;
        s = Math.min(s, usrMemoryList.size());
        for (Memory memory : usrMemoryList) {
            Object[] info = new Object[userMemoryColumnNames.length];
            if (memory.getPid() == -1) info[0] = "空闲";
            else info[0] = memory.getPid();
            info[1] = memory.getSt();
            info[2] = memory.getEd();
            info[3] = Long.parseLong(memory.getEd().substring(2), 16) -
                    Long.parseLong(memory.getSt().substring(2), 16);
            userMemoryModel.addRow(info);
        }
        userMemoryModel.fireTableDataChanged();
        try {
            userMemoryDetail.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        userMemoryDetail.updateUI();
    }

    private void updateSystemMemory(List<Memory> sysMemoryList) {
        int s = systemMemoryDetail.getSelectedRow();
        systemMemoryModel.getDataVector().removeAllElements();
        if (sysMemoryList == null)
            return;
        s = Math.min(s, sysMemoryList.size());
        for (Memory memory : sysMemoryList) {
            Object[] info = new Object[systemMemoryColumnNames.length];
            if (memory.getPid() == -1) info[0] = "空闲";
            else info[0] = memory.getPid();
            info[1] = memory.getSt();
            info[2] = memory.getEd();
            info[3] = Long.parseLong(memory.getEd().substring(2), 16) -
                    Long.parseLong(memory.getSt().substring(2), 16);
            systemMemoryModel.addRow(info);
        }
        systemMemoryModel.fireTableDataChanged();
        try {
            systemMemoryDetail.setRowSelectionInterval(s, s);
        } catch (IllegalArgumentException ignored) {
        }
        systemMemoryDetail.updateUI();
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
        updateReadyQueue(processor.getQueue());
        updateIoQueue(processor.getIoQueue());
    }

    public void updateUI(SystemInfo systemInfo) {
        cpuCnt.setText(String.valueOf(systemInfo.getCpuCnt()));
        pidCnt.setText(String.valueOf(systemInfo.getPidCnt()));
        procCnt.setText(String.valueOf(systemInfo.getProcCnt()));
        runProcCnt.setText(String.valueOf(systemInfo.getRunProcCnt()));
        jobCnt.setText(String.valueOf(systemInfo.getJobCnt()));

        SwingUtilities.invokeLater(() -> updateProcessorSelector(systemInfo.getCpuCnt()));
        SwingUtilities.invokeLater(() -> updatePoolQueue(systemInfo.getJobList()));
        SwingUtilities.invokeLater(() -> updateJobResQueue(systemInfo.getJobResList()));
        SwingUtilities.invokeLater(() -> updateSuspendQueue(systemInfo.getSuspendQueue()));
        SwingUtilities.invokeLater(() -> updateUserMemory(systemInfo.getUsrMemory()));
        SwingUtilities.invokeLater(() -> updateSystemMemory(systemInfo.getSysMemory()));
        updateProcessor(systemInfo);

    }
}
