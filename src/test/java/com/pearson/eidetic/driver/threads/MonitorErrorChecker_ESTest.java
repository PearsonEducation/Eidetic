/*
 * This file was automatically generated by EvoSuite
 * Fri Jun 23 21:04:28 GMT 2017
 */

package com.pearson.eidetic.driver.threads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.threads.MonitorErrorChecker;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class MonitorErrorChecker_ESTest extends MonitorErrorChecker_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      MonitorErrorChecker monitorErrorChecker0 = new MonitorErrorChecker((AwsAccount) null);
      // Undeclared exception!
      try { 
        monitorErrorChecker0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorErrorChecker", e);
      }
  }
}
