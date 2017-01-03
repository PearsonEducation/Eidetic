/*
 * This file was automatically generated by EvoSuite
 * Thu Aug 04 21:31:00 GMT 2016
 */

package com.pearson.eidetic.driver.threads.subthreads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.threads.subthreads.SnapshotCreationTime;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true) 
public class SnapshotCreationTime_ESTest extends SnapshotCreationTime_ESTest_scaffolding {

  @Test
  public void test0()  throws Throwable  {
      SnapshotCreationTime snapshotCreationTime0 = null;
      try {
        snapshotCreationTime0 = new SnapshotCreationTime((AwsAccount) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         assertThrownBy("com.pearson.eidetic.driver.threads.subthreads.SnapshotCreationTime", e);
      }
  }
}