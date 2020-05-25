package org.bos.consensus.base;

import org.bos.consensus.base.Param.Miner;
import org.bos.core.capsule.BlockCapsule;

public interface BlockHandle {

  State getState();

  Object getLock();

  BlockCapsule produce(Miner miner, long blockTime, long timeout);

}