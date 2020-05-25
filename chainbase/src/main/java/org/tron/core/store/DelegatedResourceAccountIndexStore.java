package org.bos.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.bos.core.capsule.DelegatedResourceAccountIndexCapsule;
import org.bos.core.db.BosStoreWithRevoking;

@Component
public class DelegatedResourceAccountIndexStore extends
    BosStoreWithRevoking<DelegatedResourceAccountIndexCapsule> {

  @Autowired
  public DelegatedResourceAccountIndexStore(@Value("DelegatedResourceAccountIndex") String dbName) {
    super(dbName);
  }

  @Override
  public DelegatedResourceAccountIndexCapsule get(byte[] key) {

    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new DelegatedResourceAccountIndexCapsule(value);
  }

}