/*
 * This file was automatically generated by EvoSuite
 * Wed Jun 28 17:03:55 GMT 2017
 */

package com.pearson.eidetic.utilities;

import org.junit.Test;
import static org.junit.Assert.*;
import com.pearson.eidetic.utilities.PropertiesConfigurationWrapper;
import java.io.File;
import java.io.InputStream;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class PropertiesConfigurationWrapper_ESTest extends PropertiesConfigurationWrapper_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      boolean boolean0 = propertiesConfigurationWrapper0.isValid();
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("/");
      assertNull(propertiesConfigurationWrapper0.getConfigurationDirectory());
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper((String) null);
      assertNull(propertiesConfigurationWrapper0.getConfigurationFilename());
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      PropertiesConfigurationWrapper.savePropertiesConfigurationFile((String) null, "com.pearson.eidetic.utilities.PropertiesConfigurationWrapper", (PropertiesConfiguration) null);
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      InputStream inputStream0 = propertiesConfigurationWrapper0.getConfigurationInputStream();
      assertNull(inputStream0);
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper((File) null);
      PropertiesConfiguration propertiesConfiguration0 = propertiesConfigurationWrapper0.getPropertiesConfiguration();
      assertNull(propertiesConfiguration0);
  }

  @Test(timeout = 4000)
  public void test06()  throws Throwable  {
      PropertiesConfigurationWrapper.savePropertiesConfigurationFile((File) null, (PropertiesConfiguration) null);
  }

  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      propertiesConfigurationWrapper0.savePropertiesConfigurationFile((File) null);
      assertNull(propertiesConfigurationWrapper0.getConfigurationFilename());
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      propertiesConfigurationWrapper0.savePropertiesConfigurationFile((String) null, "com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      assertNull(propertiesConfigurationWrapper0.getConfigurationFilename());
  }

  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper((InputStream) null);
      assertNull(propertiesConfigurationWrapper0.getConfigurationFilename());
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      String string0 = propertiesConfigurationWrapper0.getConfigurationFilename();
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      String string0 = propertiesConfigurationWrapper0.savePropertiesConfigurationToString();
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      String string0 = PropertiesConfigurationWrapper.savePropertiesConfigurationToString((PropertiesConfiguration) null);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      PropertiesConfigurationWrapper propertiesConfigurationWrapper0 = new PropertiesConfigurationWrapper("com.pearson.eidetic.uGilties.ProprtiesCojfigurationWrapper");
      String string0 = propertiesConfigurationWrapper0.getConfigurationDirectory();
      assertNull(string0);
  }
}
