package idv.mark.share_module;

import idv.mark.share_module._abstract.AbstractMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.PriorityQueue;

public class QueueUtil {
    private static final PriorityQueue<Model> queue = new PriorityQueue<>();
    private static Boolean stopped = false;
    private static Boolean busy = false;
    private static Thread pollingThread;

    public static void add(String text) {
        Model model = new Model();
        model.setMessage(text);
        model.setPriority(5);
        queue.add(model);
    }

    public static void add(Integer priority, String text) {
        Model model = new Model(priority, text);
        queue.add(model);
    }

    public static void stop() {
        stopped = true;
        if (pollingThread != null) {
            while (busy) {
                try {
                    Thread.sleep(3000); // 短暫等待，降低 CPU 負擔
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            pollingThread.interrupt();
        }
    }

    public static void go() {
        stopped = false;
    }

    private synchronized static boolean isStopped() {
        return stopped;
    }

    private synchronized static boolean threadDead() {
        return pollingThread == null || !pollingThread.isAlive();
    }

    public synchronized static void startPollingThread(AbstractMessageHandler handlerChain) {
        if (threadDead()) {
            pollingThread = new Thread(() -> {
                while (!isStopped()) {
                    if (!busy && !queue.isEmpty()) {
                        busy = true;
                        try {
                            Model model = queue.poll();
                            if (model != null) {
                                handlerChain.handle(model.getMessage());
                            }
                        } finally {
                            synchronized (QueueUtil.class) {
                                busy = false;
                                QueueUtil.class.notify(); // 通知停止等待
                            }
                        }
                    }

                    try {
                        Thread.sleep(5000); // 每5秒檢查一次
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break; // 中斷時退出循環
                    }
                }
            });
            System.out.printf("create new polling thread, %s\n", pollingThread.getId());
            pollingThread.start(); // 啟動輪詢執行緒
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Model implements Comparable<Model> {
        private Integer priority;
        private String message;

        @Override
        public int compareTo(Model o) {
            return Integer.compare(this.priority, o.priority);
        }
    }
}
