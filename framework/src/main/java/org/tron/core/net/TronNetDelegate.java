package org.bos.core.net;

import static org.bos.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bos.common.backup.BackupServer;
import org.bos.common.overlay.message.Message;
import org.bos.common.overlay.server.ChannelManager;
import org.bos.common.overlay.server.SyncPool;
import org.bos.common.utils.Sha256Hash;
import org.bos.core.ChainBaseManager;
import org.bos.core.capsule.BlockCapsule;
import org.bos.core.capsule.BlockCapsule.BlockId;
import org.bos.core.capsule.TransactionCapsule;
import org.bos.core.db.Manager;
import org.bos.core.exception.AccountResourceInsufficientException;
import org.bos.core.exception.BadBlockException;
import org.bos.core.exception.BadItemException;
import org.bos.core.exception.BadNumberBlockException;
import org.bos.core.exception.ContractExeException;
import org.bos.core.exception.ContractSizeNotEqualToOneException;
import org.bos.core.exception.ContractValidateException;
import org.bos.core.exception.DupTransactionException;
import org.bos.core.exception.ItemNotFoundException;
import org.bos.core.exception.NonCommonBlockException;
import org.bos.core.exception.P2pException;
import org.bos.core.exception.P2pException.TypeEnum;
import org.bos.core.exception.ReceiptCheckErrException;
import org.bos.core.exception.StoreException;
import org.bos.core.exception.TaposException;
import org.bos.core.exception.TooBigTransactionException;
import org.bos.core.exception.TooBigTransactionResultException;
import org.bos.core.exception.TransactionExpirationException;
import org.bos.core.exception.UnLinkedBlockException;
import org.bos.core.exception.VMIllegalException;
import org.bos.core.exception.ValidateScheduleException;
import org.bos.core.exception.ValidateSignatureException;
import org.bos.core.exception.ZksnarkException;
import org.bos.core.metrics.MetricsService;
import org.bos.core.net.message.BlockMessage;
import org.bos.core.net.message.MessageTypes;
import org.bos.core.net.message.TransactionMessage;
import org.bos.core.net.peer.PeerConnection;
import org.bos.core.store.WitnessScheduleStore;
import org.bos.protos.Protocol.Inventory.InventoryType;

@Slf4j(topic = "net")
@Component
public class BosNetDelegate {

  @Autowired
  private SyncPool syncPool;

  @Autowired
  private ChannelManager channelManager;

  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private WitnessScheduleStore witnessScheduleStore;

  @Getter
  private Object blockLock = new Object();

  @Autowired
  private BackupServer backupServer;

  @Autowired
  private MetricsService metricsService;

  private volatile boolean backupServerStartFlag;

  private int blockIdCacheSize = 100;

  private Queue<BlockId> freshBlockId = new ConcurrentLinkedQueue<BlockId>() {
    @Override
    public boolean offer(BlockId blockId) {
      if (size() > blockIdCacheSize) {
        super.poll();
      }
      return super.offer(blockId);
    }
  };

  public void trustNode(PeerConnection peer) {
    channelManager.getTrustNodes().put(peer.getInetAddress(), peer.getNode());
  }

  public Collection<PeerConnection> getActivePeer() {
    return syncPool.getActivePeers();
  }

  public long getSyncBeginNumber() {
    return dbManager.getSyncBeginNumber();
  }

  public long getBlockTime(BlockId id) throws P2pException {
    try {
      return chainBaseManager.getBlockById(id).getTimeStamp();
    } catch (BadItemException | ItemNotFoundException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, id.getString());
    }
  }

  public BlockId getHeadBlockId() {
    return chainBaseManager.getHeadBlockId();
  }

  public BlockId getSolidBlockId() {
    return chainBaseManager.getSolidBlockId();
  }

  public BlockId getGenesisBlockId() {
    return chainBaseManager.getGenesisBlockId();
  }

  public BlockId getBlockIdByNum(long num) throws P2pException {
    try {
      return chainBaseManager.getBlockIdByNum(num);
    } catch (ItemNotFoundException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND, "num: " + num);
    }
  }

  public BlockCapsule getGenesisBlock() {
    return chainBaseManager.getGenesisBlock();
  }

  public long getHeadBlockTimeStamp() {
    return chainBaseManager.getHeadBlockTimeStamp();
  }

  public boolean containBlock(BlockId id) {
    return chainBaseManager.containBlock(id);
  }

  public boolean containBlockInMainChain(BlockId id) {
    return chainBaseManager.containBlockInMainChain(id);
  }

  public List<BlockId> getBlockChainHashesOnFork(BlockId forkBlockHash) throws P2pException {
    try {
      return dbManager.getBlockChainHashesOnFork(forkBlockHash);
    } catch (NonCommonBlockException e) {
      throw new P2pException(TypeEnum.HARD_FORKED, forkBlockHash.getString());
    }
  }

  public boolean canChainRevoke(long num) {
    return num >= dbManager.getSyncBeginNumber();
  }

  public boolean contain(Sha256Hash hash, MessageTypes type) {
    if (type.equals(MessageTypes.BLOCK)) {
      return chainBaseManager.containBlock(hash);
    } else if (type.equals(MessageTypes.BOS)) {
      return dbManager.getTransactionStore().has(hash.getBytes());
    }
    return false;
  }

  public Message getData(Sha256Hash hash, InventoryType type) throws P2pException {
    try {
      switch (type) {
        case BLOCK:
          return new BlockMessage(chainBaseManager.getBlockById(hash));
        case BOS:
          TransactionCapsule tx = chainBaseManager.getTransactionStore().get(hash.getBytes());
          if (tx != null) {
            return new TransactionMessage(tx.getInstance());
          }
          throw new StoreException();
        default:
          throw new StoreException();
      }
    } catch (StoreException e) {
      throw new P2pException(TypeEnum.DB_ITEM_NOT_FOUND,
          "type: " + type + ", hash: " + hash.getByteString());
    }
  }

  public void processBlock(BlockCapsule block, boolean isSync) throws P2pException {
    BlockId blockId = block.getBlockId();
    synchronized (blockLock) {
      try {
        if (!freshBlockId.contains(blockId)) {
          if (block.getNum() <= getHeadBlockId().getNum()) {
            logger.warn("Receive a fork block {} witness {}, head {}",
                block.getBlockId().getString(),
                Hex.toHexString(block.getWitnessAddress().toByteArray()),
                getHeadBlockId().getString());
          }
          if (!isSync) {
            //record metrics
            metricsService.applyBlock(block);
          }
          dbManager.pushBlock(block);
          freshBlockId.add(blockId);
          logger.info("Success process block {}.", blockId.getString());
          if (!backupServerStartFlag
              && System.currentTimeMillis() - block.getTimeStamp() < BLOCK_PRODUCED_INTERVAL) {
            backupServerStartFlag = true;
            backupServer.initServer();
          }
        }
      } catch (ValidateSignatureException
          | ContractValidateException
          | ContractExeException
          | UnLinkedBlockException
          | ValidateScheduleException
          | AccountResourceInsufficientException
          | TaposException
          | TooBigTransactionException
          | TooBigTransactionResultException
          | DupTransactionException
          | TransactionExpirationException
          | BadNumberBlockException
          | BadBlockException
          | NonCommonBlockException
          | ReceiptCheckErrException
          | VMIllegalException
          | ZksnarkException e) {
        metricsService.failProcessBlock(block.getNum(), e.getMessage());
        logger.error("Process block failed, {}, reason: {}.", blockId.getString(), e.getMessage());
        throw new P2pException(TypeEnum.BAD_BLOCK, e);
      }
    }
  }

  public void pushTransaction(TransactionCapsule trx) throws P2pException {
    try {
      trx.setTime(System.currentTimeMillis());
      dbManager.pushTransaction(trx);
    } catch (ContractSizeNotEqualToOneException
        | VMIllegalException e) {
      throw new P2pException(TypeEnum.BAD_BOS, e);
    } catch (ContractValidateException
        | ValidateSignatureException
        | ContractExeException
        | DupTransactionException
        | TaposException
        | TooBigTransactionException
        | TransactionExpirationException
        | ReceiptCheckErrException
        | TooBigTransactionResultException
        | AccountResourceInsufficientException e) {
      throw new P2pException(TypeEnum.BOS_EXE_FAILED, e);
    }
  }

  public boolean validBlock(BlockCapsule block) throws P2pException {
    try {
      return witnessScheduleStore.getActiveWitnesses().contains(block.getWitnessAddress())
          && block
          .validateSignature(dbManager.getDynamicPropertiesStore(), dbManager.getAccountStore());
    } catch (ValidateSignatureException e) {
      throw new P2pException(TypeEnum.BAD_BLOCK, e);
    }
  }
}
