package cn.magicalsheep.data;

import cn.magicalsheep.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DataSource {

    private static final SystemInfo system = new SystemInfo();
    private static final Gson gson = new Gson();

    private static final String address = "127.0.0.1";

    private static final int port = 8080;

    private DataSource() {
    }

    private static String send(String msg) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            socket.connect("tcp://" + address + ":" + port);
            socket.send(msg.getBytes(ZMQ.CHARSET), 0);
            byte[] reply = socket.recv(0);
            return new String(reply, ZMQ.CHARSET);
        }
    }

    public static SystemInfo getSystemInfo() {
        return system;
    }

    public synchronized static void updateSystemInfo() {
        String json = send("b");
        SystemInfo res = gson.fromJson(json, SystemInfo.class);
        system.setCpuCnt(res.getCpuCnt());
        system.setPidCnt(res.getPidCnt());
        system.setJobCnt(res.getJobCnt());
        system.setProcCnt(res.getProcCnt());
        system.setRunProcCnt(res.getRunProcCnt());
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < res.getCpuCnt(); i++)
            futures.add(getProcessorInfo(i));
        CompletableFuture<List<Memory>> usrMem = getUsrMemory();
        CompletableFuture<List<Memory>> sysMem = getSysMemory();
        CompletableFuture<List<Job>> jobList = getJobList();
        CompletableFuture<List<Job>> jobResList = getJobResList();
        CompletableFuture<List<Task>> suspendQueue = getSuspendQueue();
        futures.add(usrMem);
        futures.add(sysMem);
        futures.add(jobList);
        futures.add(jobResList);
        futures.add(suspendQueue);
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        try {
            for (int i = 0; i < res.getCpuCnt(); i++) {
                if (futures.get(i).get() == null) continue;
                system.getProcessors().put(i, (Processor) futures.get(i).get());
            }
            system.setUsrMemory(usrMem.get());
            system.setSysMemory(sysMem.get());
            system.setJobList(jobList.get());
            system.setJobResList(jobResList.get());
            system.setSuspendQueue(suspendQueue.get());
        } catch (Exception ignored) {
        }
    }

    private static CompletableFuture<Processor> getProcessorInfo(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("a" + id);
            Processor ret = gson.fromJson(json, Processor.class);
            List<CompletableFuture<List<Task>>> futures = new ArrayList<>();
            futures.add(getTaskQueue(ret.getId()));
            futures.add(getIoQueue(ret.getId()));
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            try {
                ret.setQueue(futures.get(0).get());
                ret.setIoQueue(futures.get(1).get());
            } catch (Exception e) {
                return null;
            }
            return ret;
        });
    }

    private static CompletableFuture<List<Memory>> getUsrMemory() {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("2");
            return gson.fromJson(json, new TypeToken<List<Memory>>() {
            }.getType());
        });
    }

    private static CompletableFuture<List<Memory>> getSysMemory() {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("3");
            return gson.fromJson(json, new TypeToken<List<Memory>>() {
            }.getType());
        });
    }

    private static CompletableFuture<List<Task>> getTaskQueue(int cpuId) {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("7" + cpuId);
            return gson.fromJson(json, new TypeToken<List<Task>>() {
            }.getType());
        });
    }

    private static CompletableFuture<List<Task>> getIoQueue(int cpuId) {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("8" + cpuId);
            return gson.fromJson(json, new TypeToken<List<Task>>() {
            }.getType());
        });
    }

    private static CompletableFuture<List<Task>> getSuspendQueue() {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("6");
            return gson.fromJson(json, new TypeToken<List<Task>>() {
            }.getType());
        });
    }

    private static CompletableFuture<List<Job>> getJobList() {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("9");
            return gson.fromJson(json, new TypeToken<List<Job>>() {
            }.getType());
        });
    }

    private static CompletableFuture<List<Job>> getJobResList() {
        return CompletableFuture.supplyAsync(() -> {
            String json = send("1");
            return gson.fromJson(json, new TypeToken<List<Job>>() {
            }.getType());
        });
    }

}
