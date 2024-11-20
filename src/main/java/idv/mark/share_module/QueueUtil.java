package idv.mark.share_module;

import idv.mark.share_module._abstract.AbstractMessageHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.PriorityQueue;

public class QueueUtil {
    private PriorityQueue<Model> queue = new PriorityQueue<>();
    private Boolean stopped = false;
    private Boolean busy = false;

    public void add(String text) {
        Model model = new Model();
        model.setMessage(text);
        model.setPriority(5);
        queue.add(model);
    }

    public void add(Integer priority, String text) {
        Model model = new Model(priority, text);
        queue.add(model);
    }

    public void stop() {
        stopped = true;
    }

    public void go() {
        stopped = false;
    }

    public synchronized boolean isStopped() {
        return stopped;
    }

    public void startPollingThread(AbstractMessageHandler handlerChain) {
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
