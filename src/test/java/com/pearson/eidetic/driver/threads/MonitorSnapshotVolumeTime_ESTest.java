/*
 * This file was automatically generated by EvoSuite
 * Wed Jun 28 16:51:31 GMT 2017
 */

package com.pearson.eidetic.driver.threads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.aws.AwsAccount;
import com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class MonitorSnapshotVolumeTime_ESTest extends MonitorSnapshotVolumeTime_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[1];
      Tag tag0 = new Tag();
      tagArray0[0] = tag0;
      Volume volume1 = volume0.withTags(tagArray0);
      Tag tag1 = tag0.withKey("eidetic");
      tag1.setValue("eidetic");
      String string0 = monitorSnapshotVolumeTime0.getIntTagValue(volume1);
      assertEquals("eidetic", string0);
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Regions regions0 = Regions.GovCloud;
      Region region0 = Region.getRegion(regions0);
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.connect(region0, "iES21qkO1Bd|z", "BandwidthType: ");
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Regions regions0 = Regions.US_WEST_2;
      Region region0 = Region.getRegion(regions0);
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.connect(region0, "b>Ac}t)#W?nb1S", (String) null);
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Secret key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      boolean boolean0 = monitorSnapshotVolumeTime0.snapshotDecision((AmazonEC2Client) null, volume0, "r4_ctv#mbzbw");
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      Integer integer0 = monitorSnapshotVolumeTime0.getKeep(jSONObject0, volume0);
      assertNull(integer0);
  }

  @Test(timeout = 4000)
  public void test06()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      JSONObject jSONObject0 = new JSONObject();
      Integer integer0 = monitorSnapshotVolumeTime0.getKeep(jSONObject0, (Volume) null);
      assertNull(integer0);
  }

  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      Integer integer0 = monitorSnapshotVolumeTime0.getKeep((JSONObject) null, volume0);
      assertNull(integer0);
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      String string0 = monitorSnapshotVolumeTime0.getRunAt((JSONObject) null, volume0);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.getRunAt(jSONObject0, volume0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      String string0 = monitorSnapshotVolumeTime0.getPeriod((JSONObject) null, (Volume) null);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.getPeriod(jSONObject0, volume0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[1];
      Tag tag0 = new Tag();
      tagArray0[0] = tag0;
      Volume volume1 = volume0.withTags(tagArray0);
      tag0.withKey("eidetic");
      String string0 = monitorSnapshotVolumeTime0.getIntTagValue(volume1);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[1];
      Tag tag0 = new Tag();
      tagArray0[0] = tag0;
      Volume volume1 = volume0.withTags(tagArray0);
      String string0 = monitorSnapshotVolumeTime0.getIntTagValue(volume1);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test14()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[1];
      Volume volume1 = volume0.withTags(tagArray0);
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.getIntTagValue(volume1);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test15()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      String string0 = monitorSnapshotVolumeTime0.getIntTagValue((Volume) null);
      assertNull(string0);
  }

  @Test(timeout = 4000)
  public void test16()  throws Throwable  {
      MonitorSnapshotVolumeTime monitorSnapshotVolumeTime0 = new MonitorSnapshotVolumeTime((AwsAccount) null);
      // Undeclared exception!
      try { 
        monitorSnapshotVolumeTime0.connect((Region) null, "\", VolumeNoTimeSize=\"", (String) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.MonitorSnapshotVolumeTime", e);
      }
  }
}
