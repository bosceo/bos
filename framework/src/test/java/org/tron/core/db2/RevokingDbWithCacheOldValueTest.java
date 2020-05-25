package org.bos.core.db2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
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
import org.bos.core.db.AbstractRevokingStore;
import org.bos.core.db.RevokingDatabase;
import org.bos.core.db.BosStoreWithRevoking;
import org.bos.core.db2.SnapshotRootTest.ProtoCapsuleTest;
import org.bos.core.exception.RevokingStoreIllegalStateException;

@Slf4j
public class RevokingDbWithCacheOldValueTest {

  private AbstractRevokingStore revokingDatabase;
  private BosApplicationContext context;
  private Application appT;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"}, Constant.TEST_CONF);
    context = new BosApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
    revokingDatabase = new TestRevokingBosDatabase();
    revokingDatabase.enable();
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testReset() {
    revokingDatabase.getStack().clear();
    TestRevokingBosStore bosDatabase = new TestRevokingBosStore(
        "testrevokingbosstore-testReset", revokingDatabase);
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("reset").getBytes());
    try (ISession tmpSession = revokingDatabase.buildSession()) {
      bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
      tmpSession.commit();
    }
    Assert.assertEquals(true, bosDatabase.has(testProtoCapsule.getData()));
    bosDatabase.reset();
    Assert.assertEquals(false, bosDatabase.has(testProtoCapsule.getData()));
    bosDatabase.reset();
  }

  @Test
  public synchronized void testPop() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingBosStore bosDatabase = new TestRevokingBosStore(
        "testrevokingbosstore-testPop", revokingDatabase);

    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("pop" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(1, revokingDatabase.getActiveDialog());
        tmpSession.commit();
        Assert.assertEquals(i, revokingDatabase.getStack().size());
        Assert.assertEquals(0, revokingDatabase.getActiveDialog());
      }
    }

    for (int i = 1; i < 11; i++) {
      revokingDatabase.pop();
      Assert.assertEquals(10 - i, revokingDatabase.getStack().size());
    }

    bosDatabase.close();

    Assert.assertEquals(0, revokingDatabase.getStack().size());
  }

  @Test
  public synchronized void testUndo() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingBosStore bosDatabase = new TestRevokingBosStore(
        "testrevokingbosstore-testUndo", revokingDatabase);

    ISession dialog = revokingDatabase.buildSession();
    for (int i = 0; i < 10; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("undo" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        Assert.assertEquals(2, revokingDatabase.getStack().size());
        tmpSession.merge();
        Assert.assertEquals(1, revokingDatabase.getStack().size());
      }
    }

    Assert.assertEquals(1, revokingDatabase.getStack().size());

    dialog.destroy();
    Assert.assertTrue(revokingDatabase.getStack().isEmpty());
    Assert.assertEquals(0, revokingDatabase.getActiveDialog());

    dialog = revokingDatabase.buildSession();
    revokingDatabase.disable();
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("del".getBytes());
    bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.enable();

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      bosDatabase.put(testProtoCapsule.getData(), new ProtoCapsuleTest("del2".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      bosDatabase.put(testProtoCapsule.getData(), new ProtoCapsuleTest("del22".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      bosDatabase.put(testProtoCapsule.getData(), new ProtoCapsuleTest("del222".getBytes()));
      tmpSession.merge();
    }

    try (ISession tmpSession = revokingDatabase.buildSession()) {
      bosDatabase.delete(testProtoCapsule.getData());
      tmpSession.merge();
    }

    dialog.destroy();

    logger.info("**********testProtoCapsule:" + String
        .valueOf(bosDatabase.getUnchecked(testProtoCapsule.getData())));
    Assert.assertArrayEquals("del".getBytes(),
        bosDatabase.getUnchecked(testProtoCapsule.getData()).getData());
    Assert.assertEquals(testProtoCapsule, bosDatabase.getUnchecked(testProtoCapsule.getData()));

    bosDatabase.close();
  }

  @Test
  public synchronized void testGetlatestValues() {
    revokingDatabase.getStack().clear();
    TestRevokingBosStore bosDatabase = new TestRevokingBosStore(
        "testrevokingbosstore-testGetlatestValues", revokingDatabase);

    for (int i = 0; i < 10; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("getLastestValues" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }
    Set<ProtoCapsuleTest> result = bosDatabase.getRevokingDB().getlatestValues(5).stream()
        .map(ProtoCapsuleTest::new)
        .collect(Collectors.toSet());

    for (int i = 9; i >= 5; i--) {
      Assert.assertEquals(true,
          result.contains(new ProtoCapsuleTest(("getLastestValues" + i).getBytes())));
    }
    bosDatabase.close();
  }

  @Test
  public synchronized void testGetValuesNext() {
    revokingDatabase.getStack().clear();
    TestRevokingBosStore bosDatabase = new TestRevokingBosStore(
        "testrevokingbosstore-testGetValuesNext", revokingDatabase);

    for (int i = 0; i < 10; i++) {
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("getValuesNext" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
        tmpSession.commit();
      }
    }
    Set<ProtoCapsuleTest> result =
        bosDatabase.getRevokingDB().getValuesNext(
            new ProtoCapsuleTest("getValuesNext2".getBytes()).getData(), 3)
            .stream()
            .map(ProtoCapsuleTest::new)
            .collect(Collectors.toSet());

    for (int i = 2; i < 5; i++) {
      Assert.assertEquals(true,
          result.contains(new ProtoCapsuleTest(("getValuesNext" + i).getBytes())));
    }
    bosDatabase.close();
  }

  @Test
  public void shutdown() throws RevokingStoreIllegalStateException {
    revokingDatabase.getStack().clear();
    TestRevokingBosStore bosDatabase = new TestRevokingBosStore(
        "testrevokingbosstore-shutdown", revokingDatabase);

    List<ProtoCapsuleTest> capsules = new ArrayList<>();
    for (int i = 1; i < 11; i++) {
      revokingDatabase.buildSession();
      ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest(("test" + i).getBytes());
      capsules.add(testProtoCapsule);
      bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
      Assert.assertEquals(revokingDatabase.getActiveDialog(), i);
      Assert.assertEquals(revokingDatabase.getStack().size(), i);
    }

    for (ProtoCapsuleTest capsule : capsules) {
      logger.info(new String(capsule.getData()));
      Assert.assertEquals(capsule, bosDatabase.getUnchecked(capsule.getData()));
    }

    revokingDatabase.shutdown();

    for (ProtoCapsuleTest capsule : capsules) {
      logger.info(bosDatabase.getUnchecked(capsule.getData()).toString());
      Assert.assertEquals(null, bosDatabase.getUnchecked(capsule.getData()).getData());
    }

    Assert.assertEquals(0, revokingDatabase.getStack().size());
    bosDatabase.close();

  }

  private static class TestRevokingBosStore extends BosStoreWithRevoking<ProtoCapsuleTest> {

    protected TestRevokingBosStore(String dbName, RevokingDatabase revokingDatabase) {
      super(dbName, revokingDatabase);
    }

    @Override
    public ProtoCapsuleTest get(byte[] key) {
      byte[] value = this.revokingDB.getUnchecked(key);
      return ArrayUtils.isEmpty(value) ? null : new ProtoCapsuleTest(value);
    }
  }

  private static class TestRevokingBosDatabase extends AbstractRevokingStore {

  }
}
