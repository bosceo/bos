package org.bos.common.runtime.vm;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.spongycastle.util.encoders.Hex;
import org.bos.common.application.ApplicationFactory;
import org.bos.common.application.BosApplicationContext;
import org.bos.common.runtime.Runtime;
import org.bos.common.storage.Deposit;
import org.bos.common.storage.DepositImpl;
import org.bos.common.utils.FileUtil;
import org.bos.core.Constant;
import org.bos.core.Wallet;
import org.bos.core.config.DefaultConfig;
import org.bos.core.config.args.Args;
import org.bos.core.db.Manager;
import org.bos.protos.Protocol.AccountType;

@Slf4j
public class VMTestBase {

  protected Manager manager;
  protected BosApplicationContext context;
  protected String dbPath;
  protected Deposit rootDeposit;
  protected String OWNER_ADDRESS;
  protected Runtime runtime;

  @Before
  public void init() {
    dbPath = "output_" + this.getClass().getName();
    Args.setParam(new String[]{"--output-directory", dbPath, "--debug"}, Constant.TEST_CONF);
    context = new BosApplicationContext(DefaultConfig.class);
    OWNER_ADDRESS = Wallet.getAddressPreFixString() + "abd4b9367799eaa3197fecb144eb71de1e049abc";
    manager = context.getBean(Manager.class);
    rootDeposit = DepositImpl.createRoot(manager);
    rootDeposit.createAccount(Hex.decode(OWNER_ADDRESS), AccountType.Normal);
    rootDeposit.addBalance(Hex.decode(OWNER_ADDRESS), 30000000000000L);

    rootDeposit.commit();
  }

  @After
  public void destroy() {
    Args.clearParam();
    context.destroy();
    if (FileUtil.deleteDir(new File(dbPath))) {
      logger.info("Release resources successful.");
    } else {
      logger.error("Release resources failure.");
    }
  }

}
