package cn.magicalsheep.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Memory {
    private int pid;
    private String st;
    private String ed;
}
