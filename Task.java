import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Task {
    public static class Items<TKey extends Comparable<TKey>> {
        private int time = 0;

        public synchronized int now() {
            time = time + 1;
            return time;
        }

        private class Item {
            private TKey id;
            private boolean isDeleted;
            private int updatedAt;

            public TKey getId() {
                return id;
            }
            public void setId(TKey id) {
                this.id = id;
            }

            public boolean isDeleted() {
                return isDeleted;
            }
            public void setDeleted(boolean deleted) {
                isDeleted = deleted;
            }

            public int getUpdatedAt() {
                return updatedAt;
            }
            public void setUpdatedAt(int updatedAt) {
                this.updatedAt = updatedAt;
            }
        }

        private final Collection<Item> items = new ArrayList<>();

        public void add(TKey id) throws Exception {
            Item item = null;
            for (Item i : items) {
                if (i.getId().equals(id)) {
                    item = i;
                    break;
                }
            }

            if (item != null)
                throw new Exception(String.format("Element with key '%s' was already added", id));

            final Item newItem = new Item();
            newItem.setId(id);
            newItem.setUpdatedAt(now());

            items.add(newItem);
        }

        public void update(TKey id) throws Exception {
            Item item = null;
            for (Item i : items) {
                if (i.getId().equals(id)) {
                    item = i;
                    break;
                }
            }
            if (item == null)
                throw new Exception(String.format("Element with key '%s' not found", id));

            if (item.isDeleted()) {
                item.setDeleted(false);
                item.setUpdatedAt(now());
            }
        }

        public void delete(TKey id) throws Exception {
            Item item = null;
            for (Item i : items) {
                if (i.getId().equals(id)) {
                    item = i;
                    break;
                }
            }
            if (item == null)
                throw new Exception(String.format("Element with key '%s' not found", id));

            if (!item.isDeleted()) {
                item.setDeleted(true);
                item.setUpdatedAt(now());
            }
        }

        public Collection<TKey> getActiveItems() {
            return items.stream().filter(item -> !item.isDeleted)
                    .sorted(Comparator.comparing(Item::getUpdatedAt).reversed())
                    .map(Item::getId)
                    .collect(Collectors.toList());
        }

        public Collection<TKey> getDeletedItems() {
            return items.stream().filter(item -> item.isDeleted)
                    .sorted(Comparator.comparing(Item::getUpdatedAt))
                    .map(Item::getId)
                    .collect(Collectors.toList());
        }

        public void setActiveItems(Collection<TKey> ids) throws Exception {
            throw new Exception("Method Items.SetActiveItems is not implemented, you must implement it first");
        }
    }

    public static void main(String[] args) {
        try {
            Items<Integer> items = new Items<>();
            items.add(0);
            items.add(1);
            items.add(2);
            items.delete(2);
            items.add(3);
            items.delete(3);

            items.setActiveItems(Arrays.asList(1, 2, 4));

            items.add(5);

            List<Integer> expected = Arrays.asList(5, 4, 2, 1);
            Collection<Integer> actual = items.getActiveItems();
            if (!expected.equals(actual))
                throw new Exception(String.format("Incorrect answer. Expected '%s'. Was '%s'",
                        expected.stream().map(String::valueOf).collect(Collectors.joining(",")),
                        actual.stream().map(String::valueOf).collect(Collectors.joining(","))));

            expected = Arrays.asList(3, 0);
            actual = items.getDeletedItems();
            if (!expected.equals(actual))
                throw new Exception(String.format("Incorrect answer. Expected '%s'. Was '%s'",
                        expected.stream().map(String::valueOf).collect(Collectors.joining(",")),
                        actual.stream().map(String::valueOf).collect(Collectors.joining(","))));

            Items<Integer> items2 = new Items<>();
            for (int i = 0; i < 10000; i++)
                items2.add(i);

            final ExecutorService es = Executors.newFixedThreadPool(1);

            es.execute(() -> {
                List<Integer> integers =
                        IntStream.range(5000, 15000)
                                .boxed()
                                .collect(Collectors.toList());
                Collections.shuffle(integers);

                try {
                    items2.setActiveItems(integers);
                } catch (Exception e) {
                    System.out.println(String.format("Task not solved: %s", e.getMessage()));
                }
            });

            es.shutdown();
            if (!es.awaitTermination(1, TimeUnit.SECONDS)) {
                throw new Exception("Task is not solved optimally");
            } else {
                if (!IntStream.range(5000, 15000).boxed().collect(Collectors.toList())
                        .equals(items2.getActiveItems().stream().sorted().collect(Collectors.toList())))
                    throw new Exception("Incorrect answer");
                else
                    System.out.println("Task solved");
            }

        } catch (Exception e) {
            System.out.println(String.format("Task not solved: %s", e.getMessage()));
        }
    }
}
