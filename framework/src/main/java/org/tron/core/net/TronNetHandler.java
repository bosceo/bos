package org.bos.core.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.bos.common.overlay.server.Channel;
import org.bos.common.overlay.server.MessageQueue;
import org.bos.core.net.message.BosMessage;
import org.bos.core.net.peer.PeerConnection;

@Component
@Scope("prototype")
public class BosNetHandler extends SimpleChannelInboundHandler<BosMessage> {

  protected PeerConnection peer;

  private MessageQueue msgQueue;

  @Autowired
  private BosNetService bosNetService;

  @Override
  public void channelRead0(final ChannelHandlerContext ctx, BosMessage msg) throws Exception {
    msgQueue.receivedMessage(msg);
    bosNetService.onMessage(peer, msg);
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    peer.processException(cause);
  }

  public void setMsgQueue(MessageQueue msgQueue) {
    this.msgQueue = msgQueue;
  }

  public void setChannel(Channel channel) {
    this.peer = (PeerConnection) channel;
  }

}