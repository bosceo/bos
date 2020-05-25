package org.bos.core.net.messagehandler;

import static org.bos.core.config.Parameter.ChainConstant.BLOCK_PRODUCED_INTERVAL;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.bos.common.overlay.discover.node.statistics.MessageCount;
import org.bos.common.overlay.message.Message;
import org.bos.common.utils.Sha256Hash;
import org.bos.core.capsule.BlockCapsule.BlockId;
import org.bos.core.config.Parameter.NetConstants;
import org.bos.core.exception.P2pException;
import org.bos.core.exception.P2pException.TypeEnum;
import org.bos.core.net.BosNetDelegate;
import org.bos.core.net.message.BlockMessage;
import org.bos.core.net.message.FetchInvDataMessage;
import org.bos.core.net.message.MessageTypes;
import org.bos.core.net.message.TransactionMessage;
import org.bos.core.net.message.TransactionsMessage;
import org.bos.core.net.message.BosMessage;
import org.bos.core.net.peer.Item;
import org.bos.core.net.peer.PeerConnection;
import org.bos.core.net.service.AdvService;
import org.bos.core.net.service.SyncService;
import org.bos.protos.Protocol.Inventory.InventoryType;
import org.bos.protos.Protocol.ReasonCode;
import org.bos.protos.Protocol.Transaction;

@Slf4j(topic = "net")
@Component
public class FetchInvDataMsgHandler implements BosMsgHandler {

  private static final int MAX_SIZE = 1_000_000;
  @Autowired
  private BosNetDelegate bosNetDelegate;
  @Autowired
  private SyncService syncService;
  @Autowired
  private AdvService advService;

  @Override
  public void processMessage(PeerConnection peer, BosMessage msg) throws P2pException {

    FetchInvDataMessage fetchInvDataMsg = (FetchInvDataMessage) msg;

    check(peer, fetchInvDataMsg);

    InventoryType type = fetchInvDataMsg.getInventoryType();
    List<Transaction> transactions = Lists.newArrayList();

    int size = 0;

    for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
      Item item = new Item(hash, type);
      Message message = advService.getMessage(item);
      if (message == null) {
        try {
          message = bosNetDelegate.getData(hash, type);
        } catch (Exception e) {
          logger.error("Fetch item {} failed. reason: {}", item, hash, e.getMessage());
          peer.disconnect(ReasonCode.FETCH_FAIL);
          return;
        }
      }

      if (type == InventoryType.BLOCK) {
        BlockId blockId = ((BlockMessage) message).getBlockCapsule().getBlockId();
        if (peer.getBlockBothHave().getNum() < blockId.getNum()) {
          peer.setBlockBothHave(blockId);
        }
        peer.sendMessage(message);
      } else {
        transactions.add(((TransactionMessage) message).getTransactionCapsule().getInstance());
        size += ((TransactionMessage) message).getTransactionCapsule().getInstance()
            .getSerializedSize();
        if (size > MAX_SIZE) {
          peer.sendMessage(new TransactionsMessage(transactions));
          transactions = Lists.newArrayList();
          size = 0;
        }
      }
    }
    if (!transactions.isEmpty()) {
      peer.sendMessage(new TransactionsMessage(transactions));
    }
  }

  private void check(PeerConnection peer, FetchInvDataMessage fetchInvDataMsg) throws P2pException {
    MessageTypes type = fetchInvDataMsg.getInvMessageType();

    if (type == MessageTypes.BOS) {
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BOS)) == null) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "not spread inv: {}" + hash);
        }
      }
      int fetchCount = peer.getNodeStatistics().messageStatistics.bosInBosFetchInvDataElement
          .getCount(10);
      int maxCount = advService.getBosCount().getCount(60);
      if (fetchCount > maxCount) {
        logger.error("maxCount: " + maxCount + ", fetchCount: " + fetchCount);
        //        throw new P2pException(TypeEnum.BAD_MESSAGE,
        //            "maxCount: " + maxCount + ", fetchCount: " + fetchCount);
      }
    } else {
      boolean isAdv = true;
      for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
        if (peer.getAdvInvSpread().getIfPresent(new Item(hash, InventoryType.BLOCK)) == null) {
          isAdv = false;
          break;
        }
      }
      if (isAdv) {
        MessageCount bosOutAdvBlock = peer.getNodeStatistics().messageStatistics.bosOutAdvBlock;
        bosOutAdvBlock.add(fetchInvDataMsg.getHashList().size());
        int outBlockCountIn1min = bosOutAdvBlock.getCount(60);
        int producedBlockIn2min = 120_000 / BLOCK_PRODUCED_INTERVAL;
        if (outBlockCountIn1min > producedBlockIn2min) {
          logger.error("producedBlockIn2min: " + producedBlockIn2min + ", outBlockCountIn1min: "
              + outBlockCountIn1min);
          //throw new P2pException(TypeEnum.BAD_MESSAGE, "producedBlockIn2min: "
          // + producedBlockIn2min
          //  + ", outBlockCountIn1min: " + outBlockCountIn1min);
        }
      } else {
        if (!peer.isNeedSyncFromUs()) {
          throw new P2pException(TypeEnum.BAD_MESSAGE, "no need sync");
        }
        for (Sha256Hash hash : fetchInvDataMsg.getHashList()) {
          long blockNum = new BlockId(hash).getNum();
          long minBlockNum =
              peer.getLastSyncBlockId().getNum() - 2 * NetConstants.SYNC_FETCH_BATCH_NUM;
          if (blockNum < minBlockNum) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                "minBlockNum: " + minBlockNum + ", blockNum: " + blockNum);
          }
          if (peer.getSyncBlockIdCache().getIfPresent(hash) != null) {
            throw new P2pException(TypeEnum.BAD_MESSAGE,
                new BlockId(hash).getString() + " is exist");
          }
          peer.getSyncBlockIdCache().put(hash, System.currentTimeMillis());
        }
      }
    }
  }

}
