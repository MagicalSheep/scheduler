package cn.magicalsheep.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Task {
    private int pid;
    @SerializedName("status")
    private Status status;
    private String st;
    private String ed;
    private int cp;
    private int prior;
    private int processor;
    private int job;

    public enum Status {
        @SerializedName("0") TASK_RUNNING, @SerializedName("1") TASK_READY,
        @SerializedName("2") TASK_INTERRUPTIBLE, @SerializedName("3") TASK_STOPPED,
        @SerializedName("4") TASK_SUSPEND, @SerializedName("5") TASK_IO_SUSPEND
    }
}
