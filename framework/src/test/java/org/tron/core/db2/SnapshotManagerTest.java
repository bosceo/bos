package org.bos.core.db2;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.bos.common.application.Application;
import org.bos.common.application.ApplicationFactory;
import org.bos.common.application.BosApplicationContext;
import org.bos.common.utils.FileUtil;
import org.bos.core.Constant;
import org.bos.core.config.DefaultConfig;
import org.bos.core.config.args.Args;
import org.bos.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingBosStore;
import org.bos.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.bos.core.db2.core.SnapshotManager;
import org.bos.core.exception.BadItemException;
import org.bos.core.exception.ItemNotFoundException;

@Slf4j
public class SnapshotManagerTest {

  private SnapshotManager revokingDatabase;
  private BosApplicationContext context;
  private Application appT;
  private TestRevokingBosStore bosDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_SnapshotManager_test"},
        Constant.TEST_CONF);
    context = new BosApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = context.getBean(SnapshotManager.class);
    revokingDatabase.enable();
    bosDatabase = new TestRevokingBosStore("testSnapshotManager-test");
    revokingDatabase.add(bosDatabase.getRevokingDB());
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    bosDatabase.close();
    FileUtil.deleteDir(new File("output_SnapshotManager_test"));
    revokingDatabase.getCheckTmpStore().close();
    bosDatabase.close();
  }

  @Test
  public synchronized void testRefresh()
      throws BadItemException, ItemNotFoundException {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("refresh".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("refresh" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        bosDatabase.put(protoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }

    revokingDatabase.flush();
    Assert.assertEquals(new ProtoCapsuleTest("refresh10".getBytes()),
        bosDatabase.get(protoCapsule.getData()));
  }

  @Test
  public synchronized void testClose() {
    while (revokingDatabase.size() != 0) {
      revokingDatabase.pop();
    }

    revokingDatabase.setMaxFlushCount(0);
    revokingDatabase.setUnChecked(false);
    revokingDatabase.setMaxSize(5);
    ProtoCapsuleTest protoCapsule = new ProtoCapsuleTest("close".getBytes());
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("close" + i).getBytes());
      try (ISession _  = revokingDatabase.buildSession()) {
        bosDatabase.put(protoCapsule.getData(), testProtoCapsule);
      }
    }
    Assert.assertEquals(null,
        bosDatabase.get(protoCapsule.getData()));

  }
}
