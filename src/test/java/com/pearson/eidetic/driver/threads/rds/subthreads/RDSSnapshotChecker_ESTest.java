/*
 * This file was automatically generated by EvoSuite
 * Wed Jun 28 16:50:09 GMT 2017
 */

package com.pearson.eidetic.driver.threads.rds.subthreads;

import org.junit.Test;
import static org.junit.Assert.*;
import static org.evosuite.runtime.EvoAssertions.*;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.model.DBCluster;
import com.amazonaws.services.rds.model.DBInstance;
import com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.evosuite.runtime.mock.java.util.MockDate;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class RDSSnapshotChecker_ESTest extends RDSSnapshotChecker_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test00()  throws Throwable  {
      Regions regions0 = Regions.EU_CENTRAL_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("", "N=US", "", "&", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBCluster dBCluster0 = new DBCluster();
      JSONObject jSONObject0 = new JSONObject();
      rDSSnapshotChecker0.getPeriod(jSONObject0, dBCluster0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test01()  throws Throwable  {
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("TaskId: ", "DefaultChildPolicy: ", "TaskId: ", "TaskId: ", (Integer) null, (Integer) null, (Region) null, arrayList0, arrayList0, (ArrayList<DBCluster>) null, (ArrayList<DBCluster>) null);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.run();
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test02()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("M,9.Z@fcM#? d~N&x", "", "un[=L{-", "M,9.Z@fcM#? d~N&x", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.run();
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test03()  throws Throwable  {
      Integer integer0 = new Integer((-4444));
      Regions regions0 = Regions.US_WEST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker((String) null, (String) null, "", (String) null, integer0, integer0, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.run();
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Access key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }

  @Test(timeout = 4000)
  public void test04()  throws Throwable  {
      Integer integer0 = new Integer((-1151));
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("", "", "\", dbCluster identifier is : ", "", integer0, integer0, (Region) null, arrayList0, arrayList0, (ArrayList<DBCluster>) null, (ArrayList<DBCluster>) null);
      JSONObject jSONObject0 = new JSONObject();
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.getPeriod(jSONObject0, (DBInstance) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test05()  throws Throwable  {
      Integer integer0 = new Integer((-9));
      Regions regions0 = Regions.SA_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("Y", "w", "Y", "LaeOAu:jrFP!", integer0, integer0, region0, (ArrayList<DBInstance>) null, (ArrayList<DBInstance>) null, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.connect(region0, "Y", "B6MQ:}IHw/E6ra>zx y");
        fail("Expecting exception: NoClassDefFoundError");
      
      } catch(NoClassDefFoundError e) {
         //
         // Could not initialize class com.amazonaws.ClientConfiguration
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test06()  throws Throwable  {
      Integer integer0 = new Integer((-2389));
      Regions regions0 = Regions.GovCloud;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      ArrayList<DBInstance> arrayList1 = new ArrayList<DBInstance>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("fe*-sJB'ICNTkT8`R", "", "`FQZzdZc0DS4", "\"H0\"", integer0, integer0, region0, arrayList1, arrayList1, arrayList0, arrayList0);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.connect(region0, "", (String) null);
        fail("Expecting exception: IllegalArgumentException");
      
      } catch(IllegalArgumentException e) {
         //
         // Secret key cannot be null.
         //
         verifyException("com.amazonaws.auth.BasicAWSCredentials", e);
      }
  }

  @Test(timeout = 4000)
  public void test07()  throws Throwable  {
      Integer integer0 = new Integer((-9));
      Regions regions0 = Regions.SA_EAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList0 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("Y", "w", "Y", "LaeOAu:jrFP!", integer0, integer0, region0, (ArrayList<DBInstance>) null, (ArrayList<DBInstance>) null, arrayList0, arrayList0);
      DBCluster dBCluster0 = new DBCluster();
      boolean boolean0 = rDSSnapshotChecker0.snapshotDeletion((AmazonRDSClient) null, dBCluster0, "LaeOAu:jrFP!", integer0);
      assertFalse(boolean0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test08()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBCluster dBCluster0 = new DBCluster();
      boolean boolean0 = rDSSnapshotChecker0.snapshotDeletion((AmazonRDSClient) null, dBCluster0, "S3ObjectVersion: ", (Integer) null);
      assertFalse(boolean0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test09()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBCluster dBCluster0 = new DBCluster();
      MockDate mockDate0 = new MockDate(0, 2, (-868), (-868), 1, 1);
      boolean boolean0 = rDSSnapshotChecker0.snapshotCreation((AmazonRDSClient) null, dBCluster0, "S3ObjectVersion: ", (Date) mockDate0);
      assertFalse(boolean0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test10()  throws Throwable  {
      Integer integer0 = new Integer((-4121));
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker((String) null, (String) null, (String) null, (String) null, integer0, integer0, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBCluster dBCluster0 = new DBCluster();
      boolean boolean0 = rDSSnapshotChecker0.snapshotCreation((AmazonRDSClient) null, dBCluster0, "|u+UQs57)Nr*^o#'", (Date) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test11()  throws Throwable  {
      Integer integer0 = new Integer(201);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker(">3@u#<i", ">3@u#<i", ">3@u#<i", "", integer0, integer0, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      boolean boolean0 = rDSSnapshotChecker0.snapshotDecision((AmazonRDSClient) null, (DBCluster) null, ">3@u#<i");
      assertFalse(rDSSnapshotChecker0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test12()  throws Throwable  {
      Regions regions0 = Regions.EU_CENTRAL_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("Xs0", "Xs0", "YW\u0002WNiTda`", "&", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBCluster dBCluster0 = new DBCluster();
      JSONObject jSONObject0 = new JSONObject();
      rDSSnapshotChecker0.getKeep(jSONObject0, dBCluster0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test13()  throws Throwable  {
      Integer integer0 = new Integer(201);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker(">3@u#<i", ">3@u#<i", ">3@u#<i", "", integer0, integer0, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      rDSSnapshotChecker0.getKeep((JSONObject) null, (DBCluster) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test14()  throws Throwable  {
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      Integer integer0 = new Integer(1);
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("RunAt", "3CYZ(4NBjrof", "3CYZ(4NBjrof", "com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", integer0, integer0, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      JSONObject jSONObject0 = new JSONObject();
      rDSSnapshotChecker0.getKeep(jSONObject0, (DBCluster) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test15()  throws Throwable  {
      Integer integer0 = new Integer(201);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker(">3@u#<i", ">3@u#<i", ">3@u#<i", "", integer0, integer0, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      rDSSnapshotChecker0.getPeriod((JSONObject) null, (DBCluster) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test16()  throws Throwable  {
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker((String) null, (String) null, "", (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      JSONObject jSONObject0 = new JSONObject();
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.getPeriod(jSONObject0, (DBCluster) null);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test17()  throws Throwable  {
      Integer integer0 = new Integer(201);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker(">3@u#<i", ">3@u#<i", ">3@u#<i", "", integer0, integer0, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      rDSSnapshotChecker0.getIntTagValue((AmazonRDSClient) null, (DBCluster) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test18()  throws Throwable  {
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("xD&<Q7XAY9w/Tq,", "cOjxl_2ztt#R/.N>:", "xD&<Q7XAY9w/Tq,", (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBCluster dBCluster0 = new DBCluster();
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.getIntTagValue((AmazonRDSClient) null, dBCluster0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test19()  throws Throwable  {
      Integer integer0 = new Integer(201);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker(">3@u#<i", ">3@u#<i", ">3@u#<i", "", integer0, integer0, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      DBInstance dBInstance0 = new DBInstance();
      boolean boolean0 = rDSSnapshotChecker0.snapshotDeletion((AmazonRDSClient) null, dBInstance0, "@UVC};So", integer0);
      assertFalse(boolean0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test20()  throws Throwable  {
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("xD&<Q7XAY9wTT|,", "cOjxl_2ztt#R/.N>:", "xD&<Q7XAY9wTT|,", (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBInstance dBInstance0 = new DBInstance();
      boolean boolean0 = rDSSnapshotChecker0.snapshotDeletion((AmazonRDSClient) null, dBInstance0, (String) null, (Integer) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test21()  throws Throwable  {
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("xD&<Q7XAY9w/Tq,", "cOjxl_2ztt#R/.N>:", "xD&<Q7XAY9w/Tq,", (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBInstance dBInstance0 = new DBInstance();
      MockDate mockDate0 = new MockDate((-2143), (-2143), (-1645), (-2143), 1, (-2105));
      boolean boolean0 = rDSSnapshotChecker0.snapshotCreation((AmazonRDSClient) null, dBInstance0, "Z^.Qi@d~}0X|)[wi /+", (Date) mockDate0);
      assertFalse(rDSSnapshotChecker0.isFinished());
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test22()  throws Throwable  {
      Integer integer0 = new Integer((-4121));
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker((String) null, (String) null, (String) null, (String) null, integer0, integer0, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBInstance dBInstance0 = new DBInstance();
      boolean boolean0 = rDSSnapshotChecker0.snapshotCreation((AmazonRDSClient) null, dBInstance0, "|u+UQs57)Nr*^o#'", (Date) null);
      assertFalse(boolean0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test23()  throws Throwable  {
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("xD&<Q7XAY9w/Tq,", "cOjxl_2ztt#R/.N>:", "xD&<Q7XAY9w/Tq,", (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      DBInstance dBInstance0 = new DBInstance();
      boolean boolean0 = rDSSnapshotChecker0.snapshotDecision((AmazonRDSClient) null, dBInstance0, "cOjxl_2ztt#R/.N>:");
      assertFalse(boolean0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test24()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      rDSSnapshotChecker0.getKeep((JSONObject) null, (DBInstance) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test25()  throws Throwable  {
      Integer integer0 = new Integer(201);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker(">3@u#<i", ">3@u#<i", ">3@u#<i", "", integer0, integer0, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      DBInstance dBInstance0 = new DBInstance();
      rDSSnapshotChecker0.getKeep((JSONObject) null, dBInstance0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test26()  throws Throwable  {
      Integer integer0 = new Integer((-1102));
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("", "", "\", dbCluster identifier is : ", "", integer0, integer0, (Region) null, arrayList0, arrayList0, (ArrayList<DBCluster>) null, (ArrayList<DBCluster>) null);
      JSONObject jSONObject0 = new JSONObject();
      DBInstance dBInstance0 = new DBInstance();
      rDSSnapshotChecker0.getKeep(jSONObject0, dBInstance0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test27()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      rDSSnapshotChecker0.getPeriod((JSONObject) null, (DBInstance) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test28()  throws Throwable  {
      Integer integer0 = new Integer((-4444));
      Regions regions0 = Regions.US_WEST_2;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker((String) null, (String) null, "", (String) null, integer0, integer0, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      HashMap<String, Object> hashMap0 = new HashMap<String, Object>();
      JSONObject jSONObject0 = new JSONObject((Map) hashMap0);
      DBInstance dBInstance0 = new DBInstance();
      rDSSnapshotChecker0.getPeriod(jSONObject0, dBInstance0);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test29()  throws Throwable  {
      Regions regions0 = Regions.AP_NORTHEAST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", "S3ObjectVersion: ", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      rDSSnapshotChecker0.getIntTagValue((AmazonRDSClient) null, (DBInstance) null);
      assertFalse(rDSSnapshotChecker0.isFinished());
  }

  @Test(timeout = 4000)
  public void test30()  throws Throwable  {
      Regions regions0 = Regions.EU_CENTRAL_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      DBInstance dBInstance0 = new DBInstance();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("", "N=US", "", "&", (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.getIntTagValue((AmazonRDSClient) null, dBInstance0);
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }

  @Test(timeout = 4000)
  public void test31()  throws Throwable  {
      Regions regions0 = Regions.US_WEST_1;
      Region region0 = Region.getRegion(regions0);
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker("xD&<Q7XAY9w/Tq,", "cOjxl_2ztt#R/.N>:", "xD&<Q7XAY9w/Tq,", (String) null, (Integer) null, (Integer) null, region0, arrayList0, arrayList0, arrayList1, arrayList1);
      boolean boolean0 = rDSSnapshotChecker0.isFinished();
      assertFalse(boolean0);
  }

  @Test(timeout = 4000)
  public void test32()  throws Throwable  {
      ArrayList<DBInstance> arrayList0 = new ArrayList<DBInstance>();
      ArrayList<DBCluster> arrayList1 = new ArrayList<DBCluster>();
      RDSSnapshotChecker rDSSnapshotChecker0 = new RDSSnapshotChecker((String) null, (String) null, (String) null, "(F", (Integer) null, (Integer) null, (Region) null, arrayList0, arrayList0, arrayList1, arrayList1);
      // Undeclared exception!
      try { 
        rDSSnapshotChecker0.connect((Region) null, "(F", "b'';|+yUHBU+1|;M");
        fail("Expecting exception: NullPointerException");
      
      } catch(NullPointerException e) {
         //
         // no message in exception (getMessage() returned null)
         //
         verifyException("com.pearson.eidetic.driver.threads.rds.subthreads.RDSSnapshotChecker", e);
      }
  }
}