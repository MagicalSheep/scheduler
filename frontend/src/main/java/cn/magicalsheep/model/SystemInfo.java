package cn.magicalsheep.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SystemInfo {
    @SerializedName("cpu_cnt")
    private int cpuCnt;
    @SerializedName("pid_cnt")
    private int pidCnt;
    @SerializedName("proc_cnt")
    private int procCnt;
    @SerializedName("run_proc_cnt")
    private int runProcCnt;
    @SerializedName("job_cnt")
    private int jobCnt;
    @SerializedName("max_sys_mem")
    private int maxSysMem;
    @SerializedName("max_usr_mem")
    private int maxUsrMem;
    private Map<Integer, Processor> processors = new HashMap<>();
    private List<Job> jobList = new ArrayList<>(), jobResList = new ArrayList<>();
    private List<Task> suspendQueue = new ArrayList<>();
    private List<Memory> usrMemory = new ArrayList<>(), sysMemory = new ArrayList<>();
}
