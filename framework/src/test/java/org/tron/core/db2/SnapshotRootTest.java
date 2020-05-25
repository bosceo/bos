package org.bos.core.db2;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.bos.common.application.Application;
import org.bos.common.application.ApplicationFactory;
import org.bos.common.application.BosApplicationContext;
import org.bos.common.utils.FileUtil;
import org.bos.common.utils.SessionOptional;
import org.bos.core.Constant;
import org.bos.core.capsule.ProtoCapsule;
import org.bos.core.config.DefaultConfig;
import org.bos.core.config.args.Args;
import org.bos.core.db2.RevokingDbWithCacheNewValueTest.TestRevokingBosStore;
import org.bos.core.db2.core.Snapshot;
import org.bos.core.db2.core.SnapshotManager;
import org.bos.core.db2.core.SnapshotRoot;

public class SnapshotRootTest {

  private TestRevokingBosStore bosDatabase;
  private BosApplicationContext context;
  private Application appT;
  private SnapshotManager revokingDatabase;

  @Before
  public void init() {
    Args.setParam(new String[]{"-d", "output_revokingStore_test"}, Constant.TEST_CONF);
    context = new BosApplicationContext(DefaultConfig.class);
    appT = ApplicationFactory.create(context);
  }

  @After
  public void removeDb() {
    Args.clearParam();
    context.destroy();
    FileUtil.deleteDir(new File("output_revokingStore_test"));
  }

  @Test
  public synchronized void testRemove() {
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("test".getBytes());
    bosDatabase = new TestRevokingBosStore("testSnapshotRoot-testRemove");
    bosDatabase.put("test".getBytes(), testProtoCapsule);
    Assert.assertEquals(testProtoCapsule, bosDatabase.get("test".getBytes()));

    bosDatabase.delete("test".getBytes());
    Assert.assertEquals(null, bosDatabase.get("test".getBytes()));
    bosDatabase.close();
  }

  @Test
  public synchronized void testMerge() {
    bosDatabase = new TestRevokingBosStore("testSnapshotRoot-testMerge");
    revokingDatabase = context.getBean(SnapshotManager.class);
    revokingDatabase.enable();
    revokingDatabase.add(bosDatabase.getRevokingDB());

    SessionOptional dialog = SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("merge".getBytes());
    bosDatabase.put(testProtoCapsule.getData(), testProtoCapsule);
    revokingDatabase.getDbs().forEach(db -> db.getHead().getRoot().merge(db.getHead()));
    dialog.reset();
    Assert.assertEquals(bosDatabase.get(testProtoCapsule.getData()), testProtoCapsule);

    bosDatabase.close();
  }

  @Test
  public synchronized void testMergeList() {
    bosDatabase = new TestRevokingBosStore("testSnapshotRoot-testMergeList");
    revokingDatabase = context.getBean(SnapshotManager.class);
    revokingDatabase.enable();
    revokingDatabase.add(bosDatabase.getRevokingDB());

    SessionOptional.instance().setValue(revokingDatabase.buildSession());
    ProtoCapsuleTest testProtoCapsule = new ProtoCapsuleTest("test".getBytes());
    bosDatabase.put("merge".getBytes(), testProtoCapsule);
    for (int i = 1; i < 11; i++) {
      ProtoCapsuleTest tmpProtoCapsule = new ProtoCapsuleTest(("mergeList" + i).getBytes());
      try (ISession tmpSession = revokingDatabase.buildSession()) {
        bosDatabase.put(tmpProtoCapsule.getData(), tmpProtoCapsule);
        tmpSession.commit();
      }
    }
    revokingDatabase.getDbs().forEach(db -> {
      List<Snapshot> snapshots = new ArrayList<>();
      SnapshotRoot root = (SnapshotRoot) db.getHead().getRoot();
      Snapshot next = root;
      for (int i = 0; i < 11; ++i) {
        next = next.getNext();
        snapshots.add(next);
      }
      root.merge(snapshots);
      root.resetSolidity();

      for (int i = 1; i < 11; i++) {
        ProtoCapsuleTest tmpProtoCapsule = new ProtoCapsuleTest(("mergeList" + i).getBytes());
        Assert.assertEquals(tmpProtoCapsule, bosDatabase.get(tmpProtoCapsule.getData()));
      }

    });
    revokingDatabase.updateSolidity(10);
    bosDatabase.close();
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class ProtoCapsuleTest implements ProtoCapsule<Object> {

    private byte[] value;

    @Override
    public byte[] getData() {
      return value;
    }

    @Override
    public Object getInstance() {
      return value;
    }

    @Override
    public String toString() {
      return "ProtoCapsuleTest{"
          + "value=" + Arrays.toString(value)
          + ", string=" + (value == null ? "" : new String(value))
          + '}';
    }
  }
}
