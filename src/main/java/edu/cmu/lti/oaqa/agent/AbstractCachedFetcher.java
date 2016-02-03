package edu.cmu.lti.oaqa.agent;

import edu.cmu.lti.oaqa.cache.KeyObjectCache;
import edu.cmu.lti.oaqa.cache.NoCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author diwang
 */
public abstract class AbstractCachedFetcher<V> {

    private static final int THREADS = 8;

    protected Logger log = LoggerFactory.getLogger(this.getClass());

    protected KeyObjectCache<V> cacheServer;

    public AbstractCachedFetcher() {
        cacheServer = new NoCache<V>(this);
    }

    public AbstractCachedFetcher(KeyObjectCache<V> cacheServer) {
        this.cacheServer = cacheServer;
    }

    static boolean RETRY_EMPTY = false;


    public V fetch(String key) {
        V val = null;
        try {
            val = cacheServer.get(key);

            boolean isEmpty = false;
            if (val instanceof String) {
                isEmpty = ((String) val).isEmpty();
            }

            if (val instanceof List) {
                isEmpty = ((List) val).isEmpty();
            }

            if (val == null || (RETRY_EMPTY && isEmpty)) {
                val = fetchOnline(key);
                if (RETRY_EMPTY && isEmpty){
                    cacheServer.del(key);
                }
                cacheServer.put(key, val);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return val;
    }

    public V fetch(String key, boolean renewCache) {
        V val = null;
        try {
            if (!renewCache) {
                val = cacheServer.get(key);
            } else {
                cacheServer.del(key);
            }
            if (val == null) {
                val = fetchOnline(key);
                cacheServer.put(key, val);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return val;
    }

    protected abstract V fetchOnline(String key);

    public LinkedHashMap<String, V> batchFetch(List<String> keys) {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        ArrayList<Future<V>> list = new ArrayList<Future<V>>();
        for (String key : keys) {
            Callable<V> worker = new FetcherCallable(key);
            Future<V> submit = executor.submit(worker);
            list.add(submit);
        }
        LinkedHashMap<String, V> results = new LinkedHashMap<String, V>();
        Iterator<Future<V>> futureIter = list.iterator();
        Iterator<String> keyIter = keys.iterator();
        while (futureIter.hasNext() && keyIter.hasNext()) {
            try {
                Future<V> future = futureIter.next();
                V val = future.get();
                results.put(keyIter.next(), val);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        return results;
    }

    public class FetcherCallable implements Callable<V> {
        String key;

        public FetcherCallable(String key) {
            super();
            this.key = key;
        }

        @Override
        public V call() throws Exception {
            return fetch(key);
        }
    }

}
