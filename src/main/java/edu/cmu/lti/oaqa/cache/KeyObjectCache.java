package edu.cmu.lti.oaqa.cache;

import java.io.IOException;

/**
 * Cache interface
 * @author Di Wang
 * @param <T>
 */
public interface KeyObjectCache<T> {

  public void put(String keyText, T obj) throws IOException;

  public T get(String keyText) throws IOException, ClassNotFoundException;

  void del(String keyText) throws IOException, ClassNotFoundException;
}
