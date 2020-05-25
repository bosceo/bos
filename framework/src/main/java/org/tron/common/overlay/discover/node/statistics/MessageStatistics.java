package org.bos.common.overlay.discover.node.statistics;

import lombok.extern.slf4j.Slf4j;
import org.bos.common.net.udp.message.UdpMessageTypeEnum;
import org.bos.common.overlay.message.Message;
import org.bos.core.net.message.FetchInvDataMessage;
import org.bos.core.net.message.InventoryMessage;
import org.bos.core.net.message.MessageTypes;
import org.bos.core.net.message.TransactionsMessage;

@Slf4j
public class MessageStatistics {

  //udp discovery
  public final MessageCount discoverInPing = new MessageCount();
  public final MessageCount discoverOutPing = new MessageCount();
  public final MessageCount discoverInPong = new MessageCount();
  public final MessageCount discoverOutPong = new MessageCount();
  public final MessageCount discoverInFindNode = new MessageCount();
  public final MessageCount discoverOutFindNode = new MessageCount();
  public final MessageCount discoverInNeighbours = new MessageCount();
  public final MessageCount discoverOutNeighbours = new MessageCount();

  //tcp p2p
  public final MessageCount p2pInHello = new MessageCount();
  public final MessageCount p2pOutHello = new MessageCount();
  public final MessageCount p2pInPing = new MessageCount();
  public final MessageCount p2pOutPing = new MessageCount();
  public final MessageCount p2pInPong = new MessageCount();
  public final MessageCount p2pOutPong = new MessageCount();
  public final MessageCount p2pInDisconnect = new MessageCount();
  public final MessageCount p2pOutDisconnect = new MessageCount();

  //tcp bos
  public final MessageCount bosInMessage = new MessageCount();
  public final MessageCount bosOutMessage = new MessageCount();

  public final MessageCount bosInSyncBlockChain = new MessageCount();
  public final MessageCount bosOutSyncBlockChain = new MessageCount();
  public final MessageCount bosInBlockChainInventory = new MessageCount();
  public final MessageCount bosOutBlockChainInventory = new MessageCount();

  public final MessageCount bosInBosInventory = new MessageCount();
  public final MessageCount bosOutBosInventory = new MessageCount();
  public final MessageCount bosInBosInventoryElement = new MessageCount();
  public final MessageCount bosOutBosInventoryElement = new MessageCount();

  public final MessageCount bosInBlockInventory = new MessageCount();
  public final MessageCount bosOutBlockInventory = new MessageCount();
  public final MessageCount bosInBlockInventoryElement = new MessageCount();
  public final MessageCount bosOutBlockInventoryElement = new MessageCount();

  public final MessageCount bosInBosFetchInvData = new MessageCount();
  public final MessageCount bosOutBosFetchInvData = new MessageCount();
  public final MessageCount bosInBosFetchInvDataElement = new MessageCount();
  public final MessageCount bosOutBosFetchInvDataElement = new MessageCount();

  public final MessageCount bosInBlockFetchInvData = new MessageCount();
  public final MessageCount bosOutBlockFetchInvData = new MessageCount();
  public final MessageCount bosInBlockFetchInvDataElement = new MessageCount();
  public final MessageCount bosOutBlockFetchInvDataElement = new MessageCount();


  public final MessageCount bosInBos = new MessageCount();
  public final MessageCount bosOutBos = new MessageCount();
  public final MessageCount bosInBoss = new MessageCount();
  public final MessageCount bosOutBoss = new MessageCount();
  public final MessageCount bosInBlock = new MessageCount();
  public final MessageCount bosOutBlock = new MessageCount();
  public final MessageCount bosOutAdvBlock = new MessageCount();

  public void addUdpInMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, true);
  }

  public void addUdpOutMessage(UdpMessageTypeEnum type) {
    addUdpMessage(type, false);
  }

  public void addTcpInMessage(Message msg) {
    addTcpMessage(msg, true);
  }

  public void addTcpOutMessage(Message msg) {
    addTcpMessage(msg, false);
  }

  private void addUdpMessage(UdpMessageTypeEnum type, boolean flag) {
    switch (type) {
      case DISCOVER_PING:
        if (flag) {
          discoverInPing.add();
        } else {
          discoverOutPing.add();
        }
        break;
      case DISCOVER_PONG:
        if (flag) {
          discoverInPong.add();
        } else {
          discoverOutPong.add();
        }
        break;
      case DISCOVER_FIND_NODE:
        if (flag) {
          discoverInFindNode.add();
        } else {
          discoverOutFindNode.add();
        }
        break;
      case DISCOVER_NEIGHBORS:
        if (flag) {
          discoverInNeighbours.add();
        } else {
          discoverOutNeighbours.add();
        }
        break;
      default:
        break;
    }
  }

  private void addTcpMessage(Message msg, boolean flag) {

    if (flag) {
      bosInMessage.add();
    } else {
      bosOutMessage.add();
    }

    switch (msg.getType()) {
      case P2P_HELLO:
        if (flag) {
          p2pInHello.add();
        } else {
          p2pOutHello.add();
        }
        break;
      case P2P_PING:
        if (flag) {
          p2pInPing.add();
        } else {
          p2pOutPing.add();
        }
        break;
      case P2P_PONG:
        if (flag) {
          p2pInPong.add();
        } else {
          p2pOutPong.add();
        }
        break;
      case P2P_DISCONNECT:
        if (flag) {
          p2pInDisconnect.add();
        } else {
          p2pOutDisconnect.add();
        }
        break;
      case SYNC_BLOCK_CHAIN:
        if (flag) {
          bosInSyncBlockChain.add();
        } else {
          bosOutSyncBlockChain.add();
        }
        break;
      case BLOCK_CHAIN_INVENTORY:
        if (flag) {
          bosInBlockChainInventory.add();
        } else {
          bosOutBlockChainInventory.add();
        }
        break;
      case INVENTORY:
        InventoryMessage inventoryMessage = (InventoryMessage) msg;
        int inventorySize = inventoryMessage.getInventory().getIdsCount();
        if (flag) {
          if (inventoryMessage.getInvMessageType() == MessageTypes.BOS) {
            bosInBosInventory.add();
            bosInBosInventoryElement.add(inventorySize);
          } else {
            bosInBlockInventory.add();
            bosInBlockInventoryElement.add(inventorySize);
          }
        } else {
          if (inventoryMessage.getInvMessageType() == MessageTypes.BOS) {
            bosOutBosInventory.add();
            bosOutBosInventoryElement.add(inventorySize);
          } else {
            bosOutBlockInventory.add();
            bosOutBlockInventoryElement.add(inventorySize);
          }
        }
        break;
      case FETCH_INV_DATA:
        FetchInvDataMessage fetchInvDataMessage = (FetchInvDataMessage) msg;
        int fetchSize = fetchInvDataMessage.getInventory().getIdsCount();
        if (flag) {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.BOS) {
            bosInBosFetchInvData.add();
            bosInBosFetchInvDataElement.add(fetchSize);
          } else {
            bosInBlockFetchInvData.add();
            bosInBlockFetchInvDataElement.add(fetchSize);
          }
        } else {
          if (fetchInvDataMessage.getInvMessageType() == MessageTypes.BOS) {
            bosOutBosFetchInvData.add();
            bosOutBosFetchInvDataElement.add(fetchSize);
          } else {
            bosOutBlockFetchInvData.add();
            bosOutBlockFetchInvDataElement.add(fetchSize);
          }
        }
        break;
      case BOSS:
        TransactionsMessage transactionsMessage = (TransactionsMessage) msg;
        if (flag) {
          bosInBoss.add();
          bosInBos.add(transactionsMessage.getTransactions().getTransactionsCount());
        } else {
          bosOutBoss.add();
          bosOutBos.add(transactionsMessage.getTransactions().getTransactionsCount());
        }
        break;
      case BOS:
        if (flag) {
          bosInMessage.add();
        } else {
          bosOutMessage.add();
        }
        break;
      case BLOCK:
        if (flag) {
          bosInBlock.add();
        }
        bosOutBlock.add();
        break;
      default:
        break;
    }
  }

}
