package org.bos.consensus.base;

import org.bos.consensus.pbft.message.PbftBaseMessage;
import org.bos.core.capsule.BlockCapsule;

public interface PbftInterface {

  boolean isSyncing();

  void forwardMessage(PbftBaseMessage message);

  BlockCapsule getBlock(long blockNum) throws Exception;

}