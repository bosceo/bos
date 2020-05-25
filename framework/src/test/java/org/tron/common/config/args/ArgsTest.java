package org.bos.common.config.args;

import java.io.File;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.bos.common.utils.FileUtil;
import org.bos.core.Constant;
import org.bos.core.config.args.Args;

public class ArgsTest {

  @Before
  public void init() {
    Args.setParam(new String[]{"--output-directory", "output-directory", "--debug"},
        Constant.TEST_CONF);
  }

  @After
  public void destroy() {
    Args.clearParam();
    FileUtil.deleteDir(new File("output-directory"));
  }

  @Test
  public void testConfig() {
    Assert.assertEquals(Args.getInstance().getMaxTransactionPendingSize(), 2000);
  }
}