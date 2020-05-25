package org.bos.common.application;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.bos.common.overlay.discover.DiscoverServer;
import org.bos.common.overlay.discover.node.NodeManager;
import org.bos.common.overlay.server.ChannelManager;
import org.bos.core.db.Manager;

public class BosApplicationContext extends AnnotationConfigApplicationContext {

  public BosApplicationContext() {
  }

  public BosApplicationContext(DefaultListableBeanFactory beanFactory) {
    super(beanFactory);
  }

  public BosApplicationContext(Class<?>... annotatedClasses) {
    super(annotatedClasses);
  }

  public BosApplicationContext(String... basePackages) {
    super(basePackages);
  }

  @Override
  public void destroy() {

    Application appT = ApplicationFactory.create(this);
    appT.shutdownServices();
    appT.shutdown();

    DiscoverServer discoverServer = getBean(DiscoverServer.class);
    discoverServer.close();
    ChannelManager channelManager = getBean(ChannelManager.class);
    channelManager.close();
    NodeManager nodeManager = getBean(NodeManager.class);
    nodeManager.close();

    Manager dbManager = getBean(Manager.class);
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    super.destroy();
  }
}
