/*
 * This file was automatically generated by EvoSuite
 * Thu Aug 04 21:19:59 GMT 2016
 */

package com.pearson.eidetic.driver.threads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.threads.MonitorCopySnapshot;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true) 
public class MonitorCopySnapshot_ESTest extends MonitorCopySnapshot_ESTest_scaffolding {

  @Test
  public void test0()  throws Throwable  {
      Integer integer0 = new Integer(2970);
      MonitorCopySnapshot monitorCopySnapshot0 = new MonitorCopySnapshot((AwsAccount) null, integer0);
      // Undeclared exception!
      try { 
        monitorCopySnapshot0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         assertThrownBy("com.pearson.eidetic.driver.threads.MonitorCopySnapshot", e);
      }
  }

  @Test
  public void test1()  throws Throwable  {
      MonitorCopySnapshot monitorCopySnapshot0 = null;
      try {
        monitorCopySnapshot0 = new MonitorCopySnapshot((AwsAccount) null, (Integer) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         assertThrownBy("com.pearson.eidetic.driver.threads.MonitorCopySnapshot", e);
      }
  }
}
