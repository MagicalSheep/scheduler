package cn.magicalsheep.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Processor {
    @SerializedName("id")
    private int id;
    @SerializedName("task_cnt")
    private int taskCnt;
    @SerializedName("cur")
    private int curPid;
    private List<Task> queue, ioQueue;
}
