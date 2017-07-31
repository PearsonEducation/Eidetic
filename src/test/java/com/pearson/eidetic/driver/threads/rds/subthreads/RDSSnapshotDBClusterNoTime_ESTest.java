/*
 * This file was automatically generated by EvoSuite
 * Wed Jun 28 16:52:43 GMT 2017
 */

package com.pearson.eidetic.driver.threads.rds.subthreads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime;
import java.util.ArrayList;
import java.util.Date;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class RDSSnapshotDBClusterNoTime_ESTest extends RDSSnapshotDBClusterNoTime_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      DBCluster dBCluster0 = new DBCluster();
      Integer integer0 = new Integer((-1482));
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("?x,6PaaoACS0Hj", "?x,6PaaoACS0Hj", "?x,6PaaoACS0Hj", "$VALES", integer0, (Integer) null, region0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      rDSSnapshotDBClusterNoTime0.getPeriod(jSONObject0, dBCluster0);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("4ggeDbEZ'3Z0%nWG3N/", "4ggeDbEZ'3Z0%nWG3N/", "4ggeDbEZ'3Z0%nWG3N/", "4ggeDbEZ'3Z0%nWG3N/", (Integer) null, (Integer) null, region0, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.run();
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      Integer integer0 = new Integer(2);
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime((String) null, (String) null, (String) null, "K*)y", integer0, integer0, region0, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.run();
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Access key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      Integer integer0 = new Integer((-1357));
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("o8Y%;", "Threshold: ", "month", "month", integer0, (Integer) null, (Region) null, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.connect((Region) null, "month", "month");
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      Regions regions0 = Regions.EU_CENTRAL_1;
      Region region0 = Region.getRegion(regions0);
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("\",Event=\"Info\", Info=\"Creating snapshot from dbCluster\", DBCluster_Identifier=\"", "snh~`DU5g?i", "", "\",Event=\"Info\", Info=\"Creating snapshot from dbCluster\", DBCluster_Identifier=\"", (Integer) null, (Integer) null, region0, (ArrayList<DBCluster>) null);
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.connect(region0, "\",Event=\"Info\", Info=\"Creating snapshot from dbCluster\", DBCluster_Identifier=\"", "\",Event=\"Info\", Info=\"Creating snapshot from dbCluster\", DBCluster_Identifier=\"");
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      Integer integer0 = new Integer(0);
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("", "\",Event=\"Error, Error=\"error in snapshotCreation\", stacktrace=\"", "", (String) null, integer0, integer0, region0, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.connect(region0, (String) null, "");
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
      Integer integer0 = new Integer(1);
      Regions regions0 = Regions.EU_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("", "", "", (String) null, integer0, integer0, region0, arrayList0);
      boolean boolean0 = rDSSnapshotDBClusterNoTime0.snapshotDeletion((AmazonRDSClient) null, (DBCluster) null, (String) null, integer0);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      DBCluster dBCluster0 = new DBCluster();
      Integer integer0 = new Integer((-1482));
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("?x,6PaaoACS0Hj", "?x,6PaaoACS0Hj", "?x,6PaaoACS0Hj", "$VALES", integer0, (Integer) null, region0, arrayList0);
      boolean boolean0 = rDSSnapshotDBClusterNoTime0.snapshotDeletion((AmazonRDSClient) null, dBCluster0, (String) null, (Integer) null);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      Integer integer0 = new Integer((-2876));
      Regions regions0 = Regions.US_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("", "", "", "", integer0, integer0, region0, arrayList0);
      DBCluster dBCluster0 = new DBCluster();
      boolean boolean0 = rDSSnapshotDBClusterNoTime0.snapshotCreation((AmazonRDSClient) null, dBCluster0, "", (Date) null);
      assertFalse(boolean0);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      Integer integer0 = new Integer(763);
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("IssueType: ", "com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest", "IssueType: ", "com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest", (Integer) null, integer0, region0, arrayList0);
      DBCluster dBCluster0 = new DBCluster();
      boolean boolean0 = rDSSnapshotDBClusterNoTime0.snapshotDecision((AmazonRDSClient) null, dBCluster0, "com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest");
      assertFalse(boolean0);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      Integer integer0 = new Integer(763);
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("IssueType: ", "com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest", "IssueType: ", "IssueType: ", (Integer) null, integer0, region0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      DBCluster dBCluster0 = new DBCluster();
      rDSSnapshotDBClusterNoTime0.getKeep(jSONObject0, dBCluster0);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      Integer integer0 = new Integer(3122);
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime((String) null, (String) null, (String) null, "f}}IM7k.~4", integer0, integer0, region0, arrayList0);
      rDSSnapshotDBClusterNoTime0.getKeep((JSONObject) null, (DBCluster) null);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      Integer integer0 = new Integer(1);
      Regions regions0 = Regions.EU_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("", "", "", (String) null, integer0, integer0, region0, arrayList0);
      rDSSnapshotDBClusterNoTime0.getPeriod((JSONObject) null, (DBCluster) null);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      Regions regions0 = Regions.EU_CENTRAL_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest", ")}K=$", ")}K=$", "com.amazonaws.services.ec2.model.CancelSpotInstanceRequestsRequest", (Integer) null, (Integer) null, region0, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.getPeriod(jSONObject0, (DBCluster) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test14()  throws Throwable  {
      Integer integer0 = new Integer(3122);
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime((String) null, (String) null, (String) null, "f}}IM7k.~4", integer0, integer0, region0, arrayList0);
      rDSSnapshotDBClusterNoTime0.getIntTagValue((AmazonRDSClient) null, (DBCluster) null);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }

  @Test(timeout = 4000)
  public void test15()  throws Throwable  {
      Integer integer0 = new Integer(763);
      Regions regions0 = Regions.AP_SOUTHEAST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("IssueType: ", "com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest", "IssueType: ", "com.amazonaws.services.simpleemail.model.GetIdentityDkimAttributesRequest", (Integer) null, integer0, region0, arrayList0);
      DBCluster dBCluster0 = new DBCluster();
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.getIntTagValue((AmazonRDSClient) null, dBCluster0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test16()  throws Throwable  {
      Integer integer0 = new Integer(0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("\", stacktrace=\"", "", "", "", integer0, integer0, (Region) null, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotDBClusterNoTime0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotDBClusterNoTime", e);
      }
  }

  @Test(timeout = 4000)
  public void test17()  throws Throwable  {
      Integer integer0 = new Integer(3122);
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime((String) null, (String) null, (String) null, "f}}IM7k.~4", integer0, integer0, region0, arrayList0);
      boolean boolean0 = rDSSnapshotDBClusterNoTime0.isFinished();
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test18()  throws Throwable  {
      Integer integer0 = new Integer(5242880);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotDBClusterNoTime rDSSnapshotDBClusterNoTime0 = new RDSSnapshotDBClusterNoTime("Eidetic_Retain", "Eidetic_Retain", "Eidetic_Retain", "Eidetic_Retain", integer0, integer0, (Region) null, arrayList0);
      JSONObject jSONObject0 = new JSONObject();
      rDSSnapshotDBClusterNoTime0.getKeep(jSONObject0, (DBCluster) null);
      assertFalse(rDSSnapshotDBClusterNoTime0.isFinished());
  }
}
