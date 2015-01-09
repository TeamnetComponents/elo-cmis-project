package org.cmis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Lucian.Dragomir on 12/13/2014.
 */
public class ExpirableMemoryCache<K, V> implements Map<K, V> {
    private static final Logger LOG = LoggerFactory.getLogger(ExpirableMemoryCache.class);

    private long timeToLive;   //dureata cat consider obiectul de actualitate in contitiile in care nu este accesat (get, set)
    private long timeToExpire; //durata cat consider obiectul de actualitate de la momentul setarii valorii (set)
    private Map<K, ExpirableCacheObject> expirableCacheObjectMap;
    private Thread evictThread;
    private long timeToEvict;
    private boolean evictEnabled;

    public ExpirableMemoryCache(long timeToExpire, long timeToLive, long timeToEvict) {
        this.timeToLive = timeToLive;
        this.timeToExpire = timeToExpire;
        this.timeToEvict = timeToEvict;
        this.evictEnabled = false;
        this.expirableCacheObjectMap = new ConcurrentHashMap<>();
        initEvictor();
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        disableEvict();
    }

    public boolean isEvictEnabled() {
        return evictEnabled;
    }

    public void disableEvict() {
        this.evictEnabled = false;
        this.evictThread = null;
    }

    private void initEvictor() {
        if (timeToEvict > 0) {
            this.evictThread = new Thread(new Runnable() {
                public void run() {
                    while (isEvictEnabled()) {
                        try {
                            Thread.sleep(timeToEvict);
                        } catch (InterruptedException ex) {
                        }
                        evict();
                    }
                }
            });
            this.evictThread.setDaemon(true);
            this.evictThread.start();
        }
    }

    public boolean isExpired(Object key) {
        ExpirableCacheObject expirableCacheObject = this.expirableCacheObjectMap.get(key);
        if (expirableCacheObject == null) {
            return false;
        }
        return isExpired(expirableCacheObject, 0);
    }

    private boolean isExpired(ExpirableCacheObject expirableCacheObject, long now) {
        if (expirableCacheObject == null) {
            return false;
        } else {
            if (now == 0) {
                now = System.currentTimeMillis();
            }
            return (now > expirableCacheObject.getUpdateTime() + this.timeToExpire);
        }
    }

    private boolean needCleanup(ExpirableCacheObject expirableCacheObject, long now) {
        if (expirableCacheObject == null) {
            return false;
        }
        if (now == 0) {
            now = System.currentTimeMillis();
        }
        if (isExpired(expirableCacheObject, now)) {
            return true;
        }
        return (now > expirableCacheObject.getAccessTime() + this.timeToLive);
    }

    private void evict() {
        try {
            long now = System.currentTimeMillis();
            Set<K> keys = this.expirableCacheObjectMap.keySet();
            for (K key : keys) {
                try {
                    ExpirableCacheObject expirableCacheObject = this.expirableCacheObjectMap.get(key);
                    if (needCleanup(expirableCacheObject, now)) {
                        expirableCacheObjectMap.remove(key);
                    }
                } catch (Exception e) {
                    //probably some related thread concurrent exceptions were raised
                }
            }

            Thread.yield();
        } catch (Exception e) {
            //
        }
    }

    @Override
    public int size() {
        return this.expirableCacheObjectMap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.expirableCacheObjectMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.expirableCacheObjectMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (value == null) {
            return false;
        }
        for (K key : this.expirableCacheObjectMap.keySet()) {
            ExpirableCacheObject expirableCacheObject = this.expirableCacheObjectMap.get(key);
            if (expirableCacheObject != null) {
                if (value.equals(expirableCacheObject.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        ExpirableCacheObject cacheObject = this.expirableCacheObjectMap.get(key);
        if (cacheObject == null) {
            return null;
        } else {
            return cacheObject.getValue();
        }
    }

    @Override
    public V put(K key, V value) {
        ExpirableCacheObject cacheObject = this.expirableCacheObjectMap.get(key);
        if (cacheObject == null) {
            cacheObject = new ExpirableCacheObject(value);
        } else {
            cacheObject.setValue(value);
        }
        this.expirableCacheObjectMap.put(key, cacheObject);
        return cacheObject.getValue();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (K key : m.keySet()) {
            this.expirableCacheObjectMap.put(key, new ExpirableCacheObject(m.get(key)));
        }
    }

    @Override
    public void clear() {
        this.expirableCacheObjectMap.clear();
    }

    @Override
    public V remove(Object key) {
        ExpirableCacheObject expirableCacheObject = this.expirableCacheObjectMap.remove(key);
        return expirableCacheObject == null ? null : expirableCacheObject.getValue();
    }


    public Set<K> keySet() {
        return this.expirableCacheObjectMap.keySet();
    }

    @Override
    public Collection<V> values() {
        List<V> values = new ArrayList();
        for (ExpirableCacheObject expirableCacheObject : this.expirableCacheObjectMap.values()) {
            values.add(expirableCacheObject.getValue());
        }
        return values;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> values = new HashSet<>();
        for (K key : this.expirableCacheObjectMap.keySet()) {
            ExpirableCacheObject expirableCacheObject = this.expirableCacheObjectMap.get(key);
            values.add(new AbstractMap.SimpleEntry(key, expirableCacheObject.getValue()));
        }
        return values;
    }

    @Override
    public boolean equals(Object o) {
        return this.equals(o);
    }

    @Override
    public int hashCode() {
        return this.hashCode();
    }

    protected class ExpirableCacheObject {
        private V value;
        private long accessTime;
        private long updateTime;

        private ExpirableCacheObject() {
            refreshAccessTime();
            refreshUpdateTime();
        }

        protected ExpirableCacheObject(V value) {
            this();
            setValue(value);
        }

        public V getValue() {
            refreshAccessTime();
            return value;
        }

        public void setValue(V value) {
            this.value = value;
            refreshAccessTime();
            refreshUpdateTime();
        }

        public long getAccessTime() {
            return accessTime;
        }

        private void setAccessTime(long accessTime) {
            this.accessTime = accessTime;
        }

        public long getUpdateTime() {
            return updateTime;
        }

        private void setUpdateTime(long updateTime) {
            this.updateTime = updateTime;
        }

        private void refreshAccessTime() {
            setAccessTime(System.currentTimeMillis());
        }

        private void refreshUpdateTime() {
            setUpdateTime(System.currentTimeMillis());
        }
    }
}
