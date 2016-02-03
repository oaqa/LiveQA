package edu.cmu.lti.oaqa.cache;

import java.io.IOException;

/**
 * @author Di Wang.
 */
public class NoCache<T> implements KeyObjectCache<T> {

    public NoCache(Class<?> aClass) {
        this(aClass.getSimpleName());
    }

    public NoCache(Object obj) {
        this(obj.getClass());
    }

    public NoCache(String bucket) {
    }

    @Override
    public void put(String keyText, Object obj) throws IOException {

    }

    @Override
    public T get(String keyText) throws IOException, ClassNotFoundException {
        return null;
    }

    @Override
    public void del(String keyText) throws IOException, ClassNotFoundException {

    }
}
