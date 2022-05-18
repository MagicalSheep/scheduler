package cn.magicalsheep.model;

import com.google.gson.annotations.SerializedName;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Job {
    private int id;
    private int siz;
    private int prior;
    @SerializedName("status")
    private Status status;
    private String msg;

    public enum Status {
        @SerializedName("0") JOB_RUNNING, @SerializedName("1") JOB_READY,
        @SerializedName("2") JOB_COMPLETED, @SerializedName("3") JOB_FAILED
    }

}
