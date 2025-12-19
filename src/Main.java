import java.util.*;

class CacheManager {
    private static CacheManager instance = null;
    private final Map<String, Object> cache = new LinkedHashMap<>();
    private final Map<String, Long> expireTime = new HashMap<>();
    private final Map<String, Integer> accessCount = new HashMap<>();
    private int MAX_SIZE = 5;
    private String policy = "LRU";

    private CacheManager() {}

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public void put(String key, Object value, long ttlMillis) {
        cleanExpired();
        if (cache.size() >= MAX_SIZE) {
            evict();
        }
        cache.put(key, value);
        expireTime.put(key, System.currentTimeMillis() + ttlMillis);
        accessCount.put(key, 0);
    }

    public Object get(String key) {
        cleanExpired();
        if (!cache.containsKey(key)) {
            return null;
        }
        accessCount.put(key, accessCount.get(key) + 1);
        return cache.get(key);
    }

    public void evict() {
        if (cache.isEmpty()) return;
        if (policy.equals("LRU")) {
            String firstKey = cache.keySet().iterator().next();
            removeKey(firstKey);
        } else if (policy.equals("LFU")) {
            String minKey = null;
            int minCount = Integer.MAX_VALUE;
            for (Map.Entry<String, Integer> entry : accessCount.entrySet()) {
                if (entry.getValue() < minCount) {
                    minCount = entry.getValue();
                    minKey = entry.getKey();
                }
            }
            removeKey(minKey);
        }
    }

    private void removeKey(String key) {
        cache.remove(key);
        expireTime.remove(key);
        accessCount.remove(key);
    }

    private void cleanExpired() {
        Iterator<String> it = cache.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if (System.currentTimeMillis() > expireTime.get(key)) {
                it.remove();
                expireTime.remove(key);
                accessCount.remove(key);
            }
        }
    }

    public void invalidateAll() {
        cache.clear();
        expireTime.clear();
        accessCount.clear();
    }

    public void setPolicy(String newPolicy) {
        if (newPolicy.equals("LRU") || newPolicy.equals("LFU")) {
            policy = newPolicy;
        }
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CacheManager cache = CacheManager.getInstance();
        cache.put("user1", "Alice", 3000);
        cache.put("user2", "Bob", 5000);
        System.out.println(cache.get("user1"));
        Thread.sleep(4000);
        System.out.println(cache.get("user1"));
        cache.setPolicy("LFU");
        cache.put("user3", "Charlie", 5000);
        System.out.println(cache.get("user3"));
    }
}
