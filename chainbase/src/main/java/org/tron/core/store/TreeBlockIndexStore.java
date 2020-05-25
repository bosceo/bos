package org.bos.core.store;

import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.bos.common.utils.ByteArray;
import org.bos.core.capsule.BytesCapsule;
import org.bos.core.db.BosStoreWithRevoking;
import org.bos.core.exception.ItemNotFoundException;

@Component
public class TreeBlockIndexStore extends BosStoreWithRevoking<BytesCapsule> {


  @Autowired
  public TreeBlockIndexStore(@Value("tree-block-index") String dbName) {
    super(dbName);

  }

  public void put(long number, byte[] key) {
    put(ByteArray.fromLong(number), new BytesCapsule(key));
  }

  public byte[] get(Long num)
      throws ItemNotFoundException {
    BytesCapsule value = getUnchecked(ByteArray.fromLong(num));
    if (value == null || value.getData() == null) {
      throw new ItemNotFoundException("number: " + num + " is not found!");
    }

    return value.getData();
  }

  @Override
  public BytesCapsule get(byte[] key)
      throws ItemNotFoundException {
    byte[] value = revokingDB.getUnchecked(key);
    if (ArrayUtils.isEmpty(value)) {
      throw new ItemNotFoundException("number: " + Arrays.toString(key) + " is not found!");
    }
    return new BytesCapsule(value);
  }
}