package org.bos.core.db2.core;

import com.google.protobuf.InvalidProtocolBufferException;
import java.util.Map.Entry;
import org.bos.common.utils.Quitable;
import org.bos.core.exception.BadItemException;
import org.bos.core.exception.ItemNotFoundException;

public interface IBosChainBase<T> extends Iterable<Entry<byte[], T>>, Quitable {

  /**
   * reset the database.
   */
  void reset();

  /**
   * close the database.
   */
  void close();

  void put(byte[] key, T item);

  void delete(byte[] key);

  T get(byte[] key) throws InvalidProtocolBufferException, ItemNotFoundException, BadItemException;

  T getUnchecked(byte[] key);

  boolean has(byte[] key);

  String getName();

  String getDbName();

}
