package org.bos.core.consensus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bos.common.backup.BackupManager;
import org.bos.common.backup.BackupManager.BackupStatusEnum;
import org.bos.consensus.Consensus;
import org.bos.consensus.base.BlockHandle;
import org.bos.consensus.base.Param.Miner;
import org.bos.consensus.base.State;
import org.bos.core.capsule.BlockCapsule;
import org.bos.core.db.Manager;
import org.bos.core.net.BosNetService;
import org.bos.core.net.message.BlockMessage;

@Slf4j(topic = "consensus")
@Component
public class BlockHandleImpl implements BlockHandle {

  @Autowired
  private Manager manager;

  @Autowired
  private BackupManager backupManager;

  @Autowired
  private BosNetService bosNetService;

  @Autowired
  private Consensus consensus;

  @Override
  public State getState() {
    if (!backupManager.getStatus().equals(BackupStatusEnum.MASTER)) {
      return State.BACKUP_IS_NOT_MASTER;
    }
    return State.OK;
  }

  public Object getLock() {
    return manager;
  }

  public BlockCapsule produce(Miner miner, long blockTime, long timeout) {
    BlockCapsule blockCapsule = manager.generateBlock(miner, blockTime, timeout);
    if (blockCapsule == null) {
      return null;
    }
    try {
      consensus.receiveBlock(blockCapsule);
      BlockMessage blockMessage = new BlockMessage(blockCapsule);
      bosNetService.fastForward(blockMessage);
      manager.pushBlock(blockCapsule);
      bosNetService.broadcast(blockMessage);
    } catch (Exception e) {
      logger.error("Handle block {} failed.", blockCapsule.getBlockId().getString(), e);
      return null;
    }
    return blockCapsule;
  }
}
