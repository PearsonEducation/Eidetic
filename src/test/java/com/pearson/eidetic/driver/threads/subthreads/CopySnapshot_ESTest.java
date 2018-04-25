/*
 * This file was automatically generated by EvoSuite
 * Wed Jun 28 16:48:57 GMT 2017
 */

package com.pearson.eidetic.driver.threads.subthreads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.driver.threads.subthreads.CopySnapshot;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import java.util.ArrayList;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class CopySnapshot_ESTest extends CopySnapshot_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("&;{", "&;{", "", (Integer) null, (Integer) null, region0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      copySnapshot0.getPeriod(jSONObject0, volume0);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      Integer integer0 = new Integer(2490);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("Eidetic", "Eidetic", "Eidetic", integer0, (Integer) null, region0, arrayList0);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[7];
      Tag tag0 = new Tag("Eidetic");
      tagArray0[0] = tag0;
      tag0.withValue("MediumChangerType: ");
      volume0.withTags(tagArray0);
      copySnapshot0.getIntTagValue(volume0);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot(">:<GrxTk1K", "fh,t-", ">:<GrxTk1K", (Integer) null, (Integer) null, (Region) null, arrayList0);
      // Undeclared exception!
      try { 
        copySnapshot0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      Integer integer0 = new Integer(0);
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("TestType: ", "TestType: ", "TestType: ", (Integer) null, integer0, region0, arrayList0);
      // Undeclared exception!
      try { 
        copySnapshot0.run();
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      Integer integer0 = new Integer(1186);
      Regions regions0 = Regions.GovCloud;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("3/ 11", "3/ 11", "7F:[X96z Wh|", integer0, integer0, region0, arrayList0);
      // Undeclared exception!
      try { 
        copySnapshot0.connect(region0, "7F:[X96z Wh|", "3/ 11");
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      Integer integer0 = new Integer(307);
      Regions regions0 = Regions.US_WEST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("rkYe^W7`[0k", "rkYe^W7`[0k", "rkYe^W7`[0k", integer0, integer0, region0, arrayList0);
      // Undeclared exception!
      try { 
        copySnapshot0.connect(region0, (String) null, (String) null);
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Access key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }

  @Test(timeout = 4000)
  public void test06()  throws Throwable  {
      Integer integer0 = new Integer((-573));
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("xF\u0004;Xf1mZGhEK1!=", "xF\u0004;Xf1mZGhEK1!=", "xF\u0004;Xf1mZGhEK1!=", integer0, integer0, (Region) null, arrayList0);
      // Undeclared exception!
      try { 
        copySnapshot0.connect((Region) null, "xF\u0004;Xf1mZGhEK1!=", "xF\u0004;Xf1mZGhEK1!=");
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      Integer integer0 = new Integer(2);
      Regions regions0 = Regions.US_WEST_2;
      Region region0 = Region.getRegion(regions0);
      CopySnapshot copySnapshot0 = new CopySnapshot("`p|d", "`p|d", "`p|d", integer0, integer0, region0, (ArrayList<Volume>) null);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      // Undeclared exception!
      try { 
        copySnapshot0.getKeep(jSONObject0, volume0, (Boolean) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      Integer integer0 = new Integer(503);
      Regions regions0 = Regions.SA_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot((String) null, (String) null, (String) null, integer0, integer0, region0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      copySnapshot0.getKeep(jSONObject0, (Volume) null, (Boolean) null);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      Integer integer0 = new Integer((-673));
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("com.amazonaws.services.ec2model.DeltepnGatewayRequest", "eietegc", ",IrGhZQ^^fm", integer0, integer0, region0, arrayList0);
      Volume volume0 = new Volume();
      copySnapshot0.getKeep((JSONObject) null, volume0, (Boolean) null);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      Integer integer0 = new Integer(1186);
      Regions regions0 = Regions.GovCloud;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("3/ 11", "3/ 11", "7F:[X96z Wh|", integer0, integer0, region0, arrayList0);
      Volume volume0 = new Volume();
      copySnapshot0.getPeriod((JSONObject) null, volume0);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      Integer integer0 = new Integer((-3757));
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("]a8>XRrd", "]a8>XRrd", "]a8>XRrd", integer0, integer0, (Region) null, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      // Undeclared exception!
      try { 
        copySnapshot0.getPeriod(jSONObject0, (Volume) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      Integer integer0 = new Integer(2490);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("Eidetic", "Eidetic", "Eidetic", integer0, (Integer) null, region0, arrayList0);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[7];
      Tag tag0 = new Tag("Eidetic");
      tagArray0[0] = tag0;
      Volume volume1 = volume0.withTags(tagArray0);
      copySnapshot0.getIntTagValue(volume1);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      Integer integer0 = new Integer(2490);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("Eidetic", "Eidetic", "Eidetic", integer0, (Integer) null, region0, arrayList0);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[7];
      Tag tag0 = new Tag("Eidetic");
      tagArray0[0] = tag0;
      tag0.withKey("MediumChangerType: ");
      volume0.withTags(tagArray0);
      // Undeclared exception!
      try { 
        copySnapshot0.getIntTagValue(volume0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.CopySnapshot", e);
      }
  }

  @Test(timeout = 4000)
  public void test14()  throws Throwable  {
      Integer integer0 = new Integer((-3757));
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("]a8>XRrd", "]a8>XRrd", "]a8>XRrd", integer0, integer0, (Region) null, arrayList0);
      copySnapshot0.getIntTagValue((Volume) null);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test15()  throws Throwable  {
      Integer integer0 = new Integer(1186);
      Regions regions0 = Regions.GovCloud;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("3/ 11", "3/ 11", "7F:[X96z Wh|", integer0, integer0, region0, arrayList0);
      Volume volume0 = new Volume();
      copySnapshot0.getIntTagValue(volume0);
      assertFalse(copySnapshot0.isFinished());
  }

  @Test(timeout = 4000)
  public void test16()  throws Throwable  {
      Regions regions0 = Regions.GovCloud;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot("Ld*jW&", "Ld*jW&", "BA-|@#A~", (Integer) null, (Integer) null, region0, arrayList0);
      boolean boolean0 = copySnapshot0.isFinished();
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test17()  throws Throwable  {
      Integer integer0 = ApplicationConfiguration.getSyncServerHttpListenerPort();
      Regions regions0 = Regions.SA_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      CopySnapshot copySnapshot0 = new CopySnapshot((String) null, (String) null, (String) null, (Integer) null, integer0, region0, arrayList0);
      // Undeclared exception!
      try { 
        copySnapshot0.run();
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Access key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }
}