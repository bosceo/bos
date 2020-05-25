package org.bos.core.net.messagehandler;

import org.bos.core.exception.P2pException;
import org.bos.core.net.message.BosMessage;
import org.bos.core.net.peer.PeerConnection;

public interface BosMsgHandler {

  void processMessage(PeerConnection peer, BosMessage msg) throws P2pException;

}
