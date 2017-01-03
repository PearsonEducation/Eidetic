/*
 * This file was automatically generated by EvoSuite
 * Thu Aug 04 21:33:36 GMT 2016
 */

package com.pearson.eidetic.utilities;

import org.junit.Test;
import static org.junit.Assert.*;
import com.pearson.eidetic.utilities.StackTrace;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true) 
public class StackTrace_ESTest extends StackTrace_ESTest_scaffolding {

  @Test
  public void test0()  throws Throwable  {
      String string0 = StackTrace.getStringFromStackTrace((StackTraceElement[]) null);
      assertNull(string0);
  }

  @Test
  public void test1()  throws Throwable  {
      StackTraceElement[] stackTraceElementArray0 = new StackTraceElement[3];
      String string0 = StackTrace.getStringFromStackTrace(stackTraceElementArray0);
      assertEquals("null\nnull\nnull\n", string0);
      assertNotNull(string0);
  }

  @Test
  public void test2()  throws Throwable  {
      String string0 = StackTrace.getStringFromStackTrace((Exception) null);
      assertEquals("<evosuite>.<evosuite>(<evosuite>)\n<evosuite>.<evosuite>(<evosuite>)\n<evosuite>.<evosuite>(<evosuite>)\n", string0);
  }

  @Test
  public void test3()  throws Throwable  {
      StackTrace stackTrace0 = new StackTrace();
  }
}
