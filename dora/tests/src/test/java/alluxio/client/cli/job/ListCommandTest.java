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

package alluxio.client.cli.job;

import static org.junit.Assert.assertTrue;

import alluxio.annotation.dora.DoraTestTodoItem;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests for job list command.
 */
@DoraTestTodoItem(action = DoraTestTodoItem.Action.REMOVE, owner = "Jianjian",
    comment = "Job master and job worker no longer exists in dora")
@Ignore("ignored during dora transition")
public final class ListCommandTest extends JobShellTest {
  @Test
  public void listTest() throws Exception {
    long jobId = runPersistJob();

    waitForJobToFinish(jobId);

    sJobShell.run("ls");
    String output = mOutput.toString();

    assertTrue(output.contains(String.valueOf(jobId)));
    assertTrue(output.contains("Persist"));
    assertTrue(output.contains("COMPLETED"));
  }
}
