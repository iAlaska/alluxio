/*
 * The Alluxio Open Foundation licenses this work under the Apache License, version 2.0
 * (the "License"). You may not use this work except in compliance with the License, which is
 * available at www.apache.org/licenses/LICENSE-2.0
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied, as more fully set forth in the License.
 *
 * See the NOTICE file distributed with this work for information regarding copyright ownership.
 */

package alluxio.client.cli.fsadmin.pathconf;

import alluxio.AlluxioURI;
import alluxio.annotation.dora.DoraTestTodoItem;
import alluxio.cli.fs.FileSystemShell;
import alluxio.cli.fsadmin.FileSystemAdminShell;
import alluxio.cli.fsadmin.pathconf.AddCommand;
import alluxio.client.ReadType;
import alluxio.client.WriteType;
import alluxio.client.cli.fs.AbstractShellIntegrationTest;
import alluxio.client.file.FileSystem;
import alluxio.client.file.FileSystemTestUtils;
import alluxio.client.file.URIStatus;
import alluxio.conf.Configuration;
import alluxio.conf.InstancedConfiguration;
import alluxio.conf.PropertyKey;
import alluxio.grpc.CreateDirectoryPOptions;
import alluxio.grpc.CreateFilePOptions;
import alluxio.master.file.meta.PersistenceState;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for pathConf add command.
 */
public class AddCommandIntegrationTest extends AbstractShellIntegrationTest {
  private static final String PATH1 = "/a/b";
  private static final String PATH2 = "/a/b/c";
  private static final String READ_TYPE_NO_CACHE =
      format(PropertyKey.USER_FILE_READ_TYPE_DEFAULT, ReadType.NO_CACHE);
  private static final String READ_TYPE_CACHE =
      format(PropertyKey.USER_FILE_READ_TYPE_DEFAULT, ReadType.CACHE);
  private static final String WRITE_TYPE_CACHE_THROUGH =
      format(PropertyKey.USER_FILE_WRITE_TYPE_DEFAULT, WriteType.CACHE_THROUGH);
  private static final String WRITE_TYPE_THROUGH =
      format(PropertyKey.USER_FILE_WRITE_TYPE_DEFAULT, WriteType.THROUGH);

  private static String format(PropertyKey key, Object value) {
    return String.format("%s=%s", key.getName(), value);
  }

  @Test
  public void add() throws Exception {
    try (FileSystemAdminShell shell = new FileSystemAdminShell(Configuration.global())) {
      int ret = shell.run("pathConf", "list");
      Assert.assertEquals(0, ret);
      String output = mOutput.toString();
      Assert.assertEquals("", output);

      ret = shell.run("pathConf", "show", PATH1);
      Assert.assertEquals(0, ret);
      output = mOutput.toString();
      Assert.assertEquals("", output);

      mOutput.reset();
      ret = shell.run("pathConf", "show", PATH2);
      Assert.assertEquals(0, ret);
      output = mOutput.toString();
      Assert.assertEquals("", output);

      mOutput.reset();
      ret = shell.run("pathConf", "add", "--property", READ_TYPE_NO_CACHE, "--property",
          WRITE_TYPE_CACHE_THROUGH, PATH1);
      Assert.assertEquals(0, ret);
      output = mOutput.toString();
      Assert.assertEquals("", output);

      mOutput.reset();
      ret = shell.run("pathConf", "list");
      Assert.assertEquals(0, ret);
      output = mOutput.toString();
      Assert.assertEquals(PATH1 + "\n", output);

      mOutput.reset();
      ret = shell.run("pathConf", "add", "--property", WRITE_TYPE_THROUGH, PATH2);
      Assert.assertEquals(0, ret);
      output = mOutput.toString();
      Assert.assertEquals("", output);

      mOutput.reset();
      ret = shell.run("pathConf", "list");
      Assert.assertEquals(0, ret);
      output = mOutput.toString();
      Assert.assertEquals(PATH1 + "\n" + PATH2 + "\n", output);

      mOutput.reset();
      ret = shell.run("pathConf", "show", PATH1);
      Assert.assertEquals(0, ret);
      String expected = READ_TYPE_NO_CACHE + "\n" + WRITE_TYPE_CACHE_THROUGH + "\n";
      output = mOutput.toString();
      Assert.assertEquals(expected, output);

      mOutput.reset();
      ret = shell.run("pathConf", "show", PATH2);
      Assert.assertEquals(0, ret);
      expected = WRITE_TYPE_THROUGH + "\n";
      output = mOutput.toString();
      Assert.assertEquals(expected, output);
    }
  }

  @Test
  public void invalidPropertyKey() throws Exception {
    try (FileSystemAdminShell shell = new FileSystemAdminShell(Configuration.global())) {
      int ret = shell.run("pathConf", "add", "--property", "unknown=value", "/");
      Assert.assertEquals(-1, ret);
      String output = mOutput.toString();
      Assert.assertEquals("Invalid property key unknown\n", output);
    }
  }

  @Test
  @Ignore
  @DoraTestTodoItem(action = DoraTestTodoItem.Action.REMOVE, owner = "jiacheng",
      comment = "path conf does not exist in dora")
  public void immediatelyEffectiveForShellCommands() throws Exception {
    // Tests that after adding some path configuration, it's immediately effective for command
    // line calls afterwards.
    InstancedConfiguration conf = Configuration.modifiableGlobal();
    try (FileSystemShell fsShell = new FileSystemShell(conf);
         FileSystemAdminShell fsAdminShell = new FileSystemAdminShell(conf)) {
      Assert.assertEquals(0,
          fsAdminShell.run("pathConf", "add", "--property", WRITE_TYPE_THROUGH, PATH1));
      Assert.assertEquals(0,
          fsAdminShell.run("pathConf", "add", "--property", WRITE_TYPE_CACHE_THROUGH, PATH2));

      FileSystem fs = sLocalAlluxioClusterResource.get().getClient();
      String file = "/file";
      FileSystemTestUtils.createByteFile(fs, file, 100, CreateFilePOptions.getDefaultInstance());

      fs.createDirectory(new AlluxioURI(PATH1),
          CreateDirectoryPOptions.newBuilder().setRecursive(true).build());
      fs.createDirectory(new AlluxioURI(PATH2),
          CreateDirectoryPOptions.newBuilder().setRecursive(true).build());

      AlluxioURI target = new AlluxioURI(PATH1 + file);
      Assert.assertEquals(0, fsShell.run("cp", file, target.toString()));
      URIStatus status = fs.getStatus(target);
      Assert.assertEquals(0, status.getInMemoryPercentage());
      Assert.assertEquals(PersistenceState.PERSISTED.toString(), status.getPersistenceState());

      target = new AlluxioURI(PATH2 + file);
      Assert.assertEquals(0, fsShell.run("cp", file, target.toString()));
      status = fs.getStatus(target);
      Assert.assertEquals(100, status.getInMemoryPercentage());
      Assert.assertEquals(PersistenceState.PERSISTED.toString(), status.getPersistenceState());
    }
  }

  @Test
  public void addNoProperty() throws Exception {
    try (FileSystemAdminShell shell = new FileSystemAdminShell(Configuration.global())) {
      int ret = shell.run("pathConf", "add", "/");
      Assert.assertEquals(0, ret);
    }
  }

  @Test
  public void overwriteProperty() throws Exception {
    try (FileSystemAdminShell shell = new FileSystemAdminShell(Configuration.global())) {
      int ret = shell.run("pathConf", "add", "--property", READ_TYPE_NO_CACHE, "/");
      Assert.assertEquals(0, ret);

      mOutput.reset();
      ret = shell.run("pathConf", "show", "/");
      Assert.assertEquals(0, ret);
      Assert.assertEquals(READ_TYPE_NO_CACHE + "\n", mOutput.toString());

      ret = shell.run("pathConf", "add", "--property", READ_TYPE_CACHE, "/");
      Assert.assertEquals(0, ret);

      mOutput.reset();
      ret = shell.run("pathConf", "show", "/");
      Assert.assertEquals(0, ret);
      Assert.assertEquals(READ_TYPE_CACHE + "\n", mOutput.toString());
    }
  }

  @Test
  public void nonClientScopeKey() throws Exception {
    try (FileSystemAdminShell shell = new FileSystemAdminShell(Configuration.global())) {
      PropertyKey key = PropertyKey.NETWORK_CONNECTION_SERVER_SHUTDOWN_TIMEOUT;
      int ret = shell.run("pathConf", "add", "--property",
          format(key, "10ms"), "/");
      Assert.assertEquals(-1, ret);
      String output = mOutput.toString();
      Assert.assertEquals(AddCommand.nonClientScopePropertyException(key) + "\n", output);
    }
  }
}
