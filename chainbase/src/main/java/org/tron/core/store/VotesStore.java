package org.bos.core.store;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.bos.core.capsule.VotesCapsule;
import org.bos.core.db.BosStoreWithRevoking;

@Component
public class VotesStore extends BosStoreWithRevoking<VotesCapsule> {

  @Autowired
  public VotesStore(@Value("votes") String dbName) {
    super(dbName);
  }

  @Override
  public VotesCapsule get(byte[] key) {
    byte[] value = revokingDB.getUnchecked(key);
    return ArrayUtils.isEmpty(value) ? null : new VotesCapsule(value);
  }
}