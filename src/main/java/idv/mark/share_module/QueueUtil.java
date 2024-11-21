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
    }

    public static void go() {
        stopped = false;
    }

    private static synchronized boolean isStopped() {
        return stopped;
    }

    public static void startPollingThread(AbstractMessageHandler handlerChain) {
        new Thread(() -> {
            while (!isStopped()) {
                if (!busy && !queue.isEmpty()) {
                    busy = true;
                    try {
                        Model model = queue.poll();
                        if (model != null) {
                            handlerChain.handle(model.getMessage());
                        }
                    } finally {
                        busy = false;
                    }
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
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
