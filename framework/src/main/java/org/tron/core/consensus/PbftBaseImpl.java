package org.bos.core.consensus;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bos.common.overlay.server.SyncPool;
import org.bos.consensus.base.PbftInterface;
import org.bos.consensus.pbft.message.PbftBaseMessage;
import org.bos.core.capsule.BlockCapsule;
import org.bos.core.db.Manager;
import org.bos.core.exception.BadItemException;
import org.bos.core.exception.ItemNotFoundException;

@Component
public class PbftBaseImpl implements PbftInterface {

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private Manager manager;

  @Override
  public boolean isSyncing() {
    if (syncPool == null) {
      return true;
    }
    AtomicBoolean result = new AtomicBoolean(false);
    syncPool.getActivePeers().forEach(peerConnection -> {
      if (peerConnection.isNeedSyncFromPeer()) {
        result.set(true);
        return;
      }
    });
    return result.get();
  }

  @Override
  public void forwardMessage(PbftBaseMessage message) {
    if (syncPool == null) {
      return;
    }
    syncPool.getActivePeers().forEach(peerConnection -> {
      peerConnection.sendMessage(message);
    });
  }

  @Override
  public BlockCapsule getBlock(long blockNum) throws BadItemException, ItemNotFoundException {
    return manager.getChainBaseManager().getBlockByNum(blockNum);
  }
}