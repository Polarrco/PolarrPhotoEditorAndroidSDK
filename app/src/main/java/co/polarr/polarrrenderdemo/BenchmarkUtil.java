package co.polarr.polarrrenderdemo;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;


public class BenchmarkUtil {
    private static final String TAG = "BenchmarkUtil";
    private static final boolean ENABLE = !false;
    private static final int M = 1024 * 1024;
    private static final Runtime runtime = Runtime.getRuntime();

    private static Map<String, Long> memMap = new HashMap<>();
    private static Map<String, Long> memResultMap = new HashMap<>();
    private static Map<String, Long> timeMap = new HashMap<>();
    private static Map<String, Long> timeResultMap = new HashMap<>();

    public static void MemStart(String name) {
        if (!ENABLE) return;

        memMap.put(name, currentMem());
    }

    public static void MemEnd(String name) {
        if (!ENABLE) return;

        if (memMap.containsKey(name)) {
            long usedMem = currentMem() - memMap.get(name);

            memResultMap.put(name, usedMem);

            Log.d(TAG, "Mem of " + name + ": " + usedMem + "MB");
        }
    }

    public static void TimeStart(String name) {
        if (!ENABLE) return;

        timeMap.put(name, System.currentTimeMillis());
    }

    public static void TimeEnd(String name) {
        if (!ENABLE) return;

        if (timeMap.containsKey(name)) {
            long timeSpend = System.currentTimeMillis() - timeMap.get(name);
            timeMap.remove(name);
            timeResultMap.put(name, timeSpend);

            Log.d(TAG, "Time spend of " + name + ": " + timeSpend + "ms");
        }
    }

    public static void TraceAllResult() {
        if (!ENABLE) return;

        for (String key : memResultMap.keySet()) {
            Log.d(TAG, "Mem of " + key + ": " + memResultMap.get(key) + "MB");
        }
        for (String key : timeResultMap.keySet()) {
            Log.d(TAG, "Time spend of " + key + ": " + timeResultMap.get(key) + "ms");
        }
    }

    private static long currentMem() {
        return (runtime.totalMemory() - runtime.freeMemory()) / M;
    }
}
