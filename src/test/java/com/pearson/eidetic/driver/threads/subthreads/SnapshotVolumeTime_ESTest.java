/*
 * This file was automatically generated by EvoSuite
 * Thu Aug 04 21:05:34 GMT 2016
 */

package com.pearson.eidetic.driver.threads.subthreads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Volume;
import com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeTime;
import java.time.chrono.JapaneseDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.evosuite.runtime.mock.java.util.MockDate;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true) 
public class SnapshotVolumeTime_ESTest extends SnapshotVolumeTime_ESTest_scaffolding {

  @Test
  public void test0()  throws Throwable  {
      Integer integer0 = new Integer((-736));
      SnapshotVolumeTime snapshotVolumeTime0 = new SnapshotVolumeTime("", "", "", integer0, integer0, (Region) null, (ArrayList<Volume>) null);
      // Undeclared exception!
      try { 
        snapshotVolumeTime0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         assertThrownBy("com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeTime", e);
      }
  }

  @Test
  public void test1()  throws Throwable  {
      Integer integer0 = new Integer((-1560));
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotVolumeTime snapshotVolumeTime0 = new SnapshotVolumeTime("2in", "2in", "hour_snapshot", integer0, integer0, (Region) null, arrayList0);
      // Undeclared exception!
      try { 
        snapshotVolumeTime0.connect((Region) null, "hour_snapshot", "2in");
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         assertThrownBy("com.pearson.eidetic.driver.threads.subthreads.SnapshotVolumeTime", e);
      }
  }

  @Test
  public void test2()  throws Throwable  {
      Integer integer0 = new Integer(1943);
      SnapshotVolumeTime snapshotVolumeTime0 = new SnapshotVolumeTime("month_snapshot", "month_snapshot", "month_snapshot", integer0, integer0, (Region) null, (ArrayList<Volume>) null);
      MockDate mockDate0 = new MockDate((long) 1943);
      boolean boolean0 = snapshotVolumeTime0.snapshotCreation((AmazonEC2Client) null, (Volume) null, "month_snapshot", mockDate0);
      assertFalse(boolean0);
      assertFalse(snapshotVolumeTime0.isFinished());
  }

  @Test
  public void test3()  throws Throwable  {
      Integer integer0 = Integer.getInteger("", (-1));
      ArrayList<Volume> arrayList0 = new ArrayList<Volume>();
      SnapshotVolumeTime snapshotVolumeTime0 = new SnapshotVolumeTime("TERMINATE_JOB_FLOW", "TERMINATE_JOB_FLOW", "com.amazonaws.services.datapipeline.model.PutPipelineDefinitionRequest", (Integer) null, integer0, (Region) null, arrayList0);
      HashMap<Integer, JapaneseDate> hashMap0 = new HashMap<Integer, JapaneseDate>();
      JSONObject jSONObject0 = new JSONObject((Map) hashMap0);
      Volume volume0 = new Volume();
      snapshotVolumeTime0.getPeriod(jSONObject0, volume0);
      assertFalse(snapshotVolumeTime0.isFinished());
  }
}
