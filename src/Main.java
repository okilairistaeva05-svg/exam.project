import java.util.*;

class CacheItem {
    Object value;
    long expireTime;

    public CacheItem(Object value, long ttlMillis) {
        this.value = value;
        this.expireTime = System.currentTimeMillis() + ttlMillis;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expireTime;
    }
}

interface CachePolicy {
    void evict(Map<String, CacheItem> cache, Map<String, Integer> accessCount);
}

class LRUPolicy implements CachePolicy {
    @Override
    public void evict(Map<String, CacheItem> cache, Map<String, Integer> accessCount) {
        if (!cache.isEmpty()) {
            String firstKey = cache.keySet().iterator().next();
            cache.remove(firstKey);
            accessCount.remove(firstKey);
        }
    }
}

class LFUPolicy implements CachePolicy {
    @Override
    public void evict(Map<String, CacheItem> cache, Map<String, Integer> accessCount) {
        String minKey = null;
        int minCount = Integer.MAX_VALUE;

        for (String key : accessCount.keySet()) {
            if (accessCount.get(key) < minCount) {
                minCount = accessCount.get(key);
                minKey = key;
            }
        }

        if (minKey != null) {
            cache.remove(minKey);
            accessCount.remove(minKey);
        }
    }
}

class CacheManager {
    private static CacheManager instance;
    private final Map<String, CacheItem> cache;
    private final Map<String, Integer> accessCount;
    private CachePolicy policy;
    private final int MAX_SIZE = 5;

    private CacheManager() {
        cache = new LinkedHashMap<>();
        accessCount = new HashMap<>();
        policy = new LRUPolicy();
    }

    public static CacheManager getInstance() {
        if (instance == null) {
            instance = new CacheManager();
        }
        return instance;
    }

    public void put(String key, Object value, long ttlMillis) {
        if (cache.containsKey(key)) {
            cache.remove(key);
        }

        if (cache.size() >= MAX_SIZE) {
            policy.evict(cache, accessCount);
        }

        cache.put(key, new CacheItem(value, ttlMillis));
        accessCount.put(key, 0);
    }

    public Object get(String key) {
        CacheItem item = cache.get(key);
        if (item == null || item.isExpired()) {
            cache.remove(key);
            accessCount.remove(key);
            return null;
        }

        accessCount.put(key, accessCount.get(key) + 1);
        return item.value;
    }

    public void evict(String key) {
        cache.remove(key);
        accessCount.remove(key);
    }

    public void invalidateAll() {
        cache.clear();
        accessCount.clear();
    }

    public void configurePolicy(CachePolicy newPolicy) {
        policy = newPolicy;
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

        cache.configurePolicy(new LFUPolicy());
        cache.put("user3", "Charlie", 5000);
        System.out.println(cache.get("user3"));
    }
}
