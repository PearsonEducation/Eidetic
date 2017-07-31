/*
 * This file was automatically generated by EvoSuite
 * Wed Jun 28 16:53:46 GMT 2017
 */

package com.pearson.eidetic.driver.threads.subthreads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker;
import com.pearson.eidetic.globals.ApplicationConfiguration;
import java.util.ArrayList;
import java.util.Date;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class SnapshotChecker_ESTest extends SnapshotChecker_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      Integer integer0 = new Integer(1537);
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", "", "", integer0, integer0, region0, arrayList0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      snapshotChecker0.getPeriod(jSONObject0, volume0);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      Integer integer0 = new Integer(2);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("Z|`iUwLc1.2$s'kPaL", "", "Z|`iUwLc1.2$s'kPaL", integer0, integer0, (Region) null, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        snapshotChecker0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      Integer integer0 = new Integer(60);
      Regions regions0 = Regions.CN_NORTH_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("K", "1E<9jas{", "com.amazonaws.services.cognitosync.model.DescribeIdentityPoolUsageRequest", integer0, (Integer) null, region0, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        snapshotChecker0.run();
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("Interval", "]-Uc$)mY$", "_KuV~Rwgw>", (Integer) null, (Integer) null, (Region) null, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        snapshotChecker0.connect((Region) null, "]-Uc$)mY$", "_KuV~Rwgw>");
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      Integer integer0 = new Integer(0);
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("Failed to reset the request input stream", "Failed to reset the request input stream", "Failed to reset the request input stream", integer0, integer0, region0, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        snapshotChecker0.connect(region0, "Failed to reset the request input stream", "");
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      Integer integer0 = new Integer((-1522));
      Regions regions0 = Regions.SA_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker(". Volume attached to ", ". Volume attached to ", "GCBrMZ", integer0, integer0, region0, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        snapshotChecker0.connect(region0, (String) null, "GCBrMZ");
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
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", (String) null, "{N*n'` OeNG", (Integer) null, (Integer) null, region0, (ArrayList<Volume>) null, (ArrayList<Volume>) null);
      Volume volume0 = new Volume();
      Integer integer0 = new Integer((-598));
      boolean boolean0 = snapshotChecker0.snapshotDeletion((AmazonEC2Client) null, volume0, "", integer0);
      assertFalse(boolean0);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", (String) null, "{N*n'` OeNG", (Integer) null, (Integer) null, region0, (ArrayList<Volume>) null, (ArrayList<Volume>) null);
      Volume volume0 = new Volume();
      boolean boolean0 = snapshotChecker0.snapshotDeletion((AmazonEC2Client) null, volume0, "", (Integer) null);
      assertFalse(snapshotChecker0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      Integer integer0 = Integer.getInteger("Bg");
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("mfl@c[(iG", "mfl@c[(iG", "TargetDBSnapshotIdentifier: ", (Integer) null, integer0, region0, arrayList0, arrayList0);
      boolean boolean0 = snapshotChecker0.snapshotCreation((AmazonEC2Client) null, (Volume) null, "Bg", (Date) null);
      assertFalse(boolean0);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      Integer integer0 = new Integer(1537);
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", "", "", integer0, integer0, region0, arrayList0, arrayList0);
      Volume volume0 = new Volume();
      boolean boolean0 = snapshotChecker0.snapshotDecision((AmazonEC2Client) null, volume0, (String) null);
      assertFalse(snapshotChecker0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      Integer integer0 = new Integer(1537);
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", "", "", integer0, integer0, region0, arrayList0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      Volume volume0 = new Volume();
      snapshotChecker0.getKeep(jSONObject0, volume0);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", (String) null, "{N*n'` OeNG", (Integer) null, (Integer) null, region0, (ArrayList<Volume>) null, (ArrayList<Volume>) null);
      JSONObject jSONObject0 = new JSONObject();
      snapshotChecker0.getKeep(jSONObject0, (Volume) null);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker((String) null, (String) null, (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0);
      Volume volume0 = new Volume();
      snapshotChecker0.getKeep((JSONObject) null, volume0);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      Integer integer0 = Integer.getInteger("Bg");
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("mfl@c[(iG", "mfl@c[(iG", "TargetDBSnapshotIdentifier: ", (Integer) null, integer0, region0, arrayList0, arrayList0);
      snapshotChecker0.getPeriod((JSONObject) null, (Volume) null);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test14()  throws Throwable  {
      Integer integer0 = new Integer((-3099));
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("com.amazonaws.services.machinelearning.model.DescribeBatchPredictionsRequest", "com.amazonaws.services.machinelearning.model.DescribeBatchPredictionsRequest", "com.amazonaws.services.machinelearning.model.DescribeBatchPredictionsRequest", integer0, integer0, region0, arrayList0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      // Undeclared exception!
      try { 
        snapshotChecker0.getPeriod(jSONObject0, (Volume) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test15()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker((String) null, (String) null, (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[1];
      Tag tag0 = new Tag("y?quud+c/oe");
      tagArray0[0] = tag0;
      volume0.withTags(tagArray0);
      snapshotChecker0.getIntTagValue(volume0);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test16()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker((String) null, (String) null, (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0);
      Volume volume0 = new Volume();
      Tag[] tagArray0 = new Tag[1];
      volume0.withTags(tagArray0);
      // Undeclared exception!
      try { 
        snapshotChecker0.getIntTagValue(volume0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.subthreads.SnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test17()  throws Throwable  {
      Integer integer0 = new Integer(900000);
      Regions regions0 = Regions.EU_CENTRAL_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("y\"rs}v,~", "^\"@v%m;ar", (String) null, integer0, integer0, region0, arrayList0, arrayList0);
      snapshotChecker0.getIntTagValue((Volume) null);
      assertFalse(snapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test18()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      SnapshotChecker snapshotChecker0 = new SnapshotChecker("", (String) null, "{N*n'` OeNG", (Integer) null, (Integer) null, region0, (ArrayList<Volume>) null, (ArrayList<Volume>) null);
      boolean boolean0 = snapshotChecker0.isFinished();
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test19()  throws Throwable  {
      Integer integer0 = ApplicationConfiguration.getrunTimeInterval();
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotChecker snapshotChecker0 = new SnapshotChecker((String) null, (String) null, (String) null, (Integer) null, integer0, region0, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        snapshotChecker0.run();
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Access key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }
}
