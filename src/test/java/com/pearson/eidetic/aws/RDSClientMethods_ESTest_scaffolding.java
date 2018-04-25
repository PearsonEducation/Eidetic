/**
 * Scaffolding file used to store all the setups needed to run 
 * tests automatically generated by EvoSuite
 * Wed Jun 28 16:57:57 GMT 2017
 */

package com.pearson.eidetic.aws;

import org.evosuite.runtime.annotation.EvoSuiteClassExclude;
import org.junit.BeforeClass;
import org.junit.Before;
import org.junit.After;
import org.junit.AfterClass;
import org.evosuite.runtime.sandbox.Sandbox;
import org.evosuite.runtime.sandbox.Sandbox.SandboxMode;

@EvoSuiteClassExclude
public class RDSClientMethods_ESTest_scaffolding {

  @org.junit.Rule 
  public org.evosuite.runtime.vnet.NonFunctionalRequirementRule nfr = new org.evosuite.runtime.vnet.NonFunctionalRequirementRule();

  private static final java.util.Properties defaultProperties = (java.util.Properties) java.lang.System.getProperties().clone(); 

  private org.evosuite.runtime.thread.ThreadStopper threadStopper =  new org.evosuite.runtime.thread.ThreadStopper (org.evosuite.runtime.thread.KillSwitchHandler.getInstance(), 3000);


  @BeforeClass 
  public static void initEvoSuiteFramework() { 
    org.evosuite.runtime.RuntimeSettings.className = "com.pearson.eidetic.aws.RDSClientMethods"; 
    org.evosuite.runtime.GuiSupport.initialize(); 
    org.evosuite.runtime.RuntimeSettings.maxNumberOfThreads = 100; 
    org.evosuite.runtime.RuntimeSettings.maxNumberOfIterationsPerLoop = 10000; 
    org.evosuite.runtime.RuntimeSettings.mockSystemIn = true; 
    org.evosuite.runtime.RuntimeSettings.sandboxMode = org.evosuite.runtime.sandbox.Sandbox.SandboxMode.RECOMMENDED; 
    org.evosuite.runtime.sandbox.Sandbox.initializeSecurityManagerForSUT(); 
    org.evosuite.runtime.classhandling.JDKClassResetter.init();
    setSystemProperties();
    initializeClasses();
    org.evosuite.runtime.Runtime.getInstance().resetRuntime(); 
  } 

  @AfterClass 
  public static void clearEvoSuiteFramework(){ 
    Sandbox.resetDefaultSecurityManager(); 
    java.lang.System.setProperties((java.util.Properties) defaultProperties.clone()); 
  } 

  @Before 
  public void initTestCase(){ 
    threadStopper.storeCurrentThreads();
    threadStopper.startRecordingTime();
    org.evosuite.runtime.jvm.ShutdownHookHandler.getInstance().initHandler(); 
    org.evosuite.runtime.sandbox.Sandbox.goingToExecuteSUTCode(); 
    setSystemProperties(); 
    org.evosuite.runtime.GuiSupport.setHeadless(); 
    org.evosuite.runtime.Runtime.getInstance().resetRuntime(); 
    org.evosuite.runtime.agent.InstrumentingAgent.activate(); 
  } 

  @After 
  public void doneWithTestCase(){ 
    threadStopper.killAndJoinClientThreads();
    org.evosuite.runtime.jvm.ShutdownHookHandler.getInstance().safeExecuteAddedHooks(); 
    org.evosuite.runtime.classhandling.JDKClassResetter.reset(); 
    resetClasses(); 
    org.evosuite.runtime.sandbox.Sandbox.doneWithExecutingSUTCode(); 
    org.evosuite.runtime.agent.InstrumentingAgent.deactivate(); 
    org.evosuite.runtime.GuiSupport.restoreHeadlessMode(); 
  } 

  public static void setSystemProperties() {
 
    java.lang.System.setProperties((java.util.Properties) defaultProperties.clone()); 
    java.lang.System.setProperty("os.name", "Mac OS X"); 
    java.lang.System.setProperty("java.awt.headless", "true"); 
    java.lang.System.setProperty("user.home", "/Users/testuser"); 
    java.lang.System.setProperty("java.home", "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre"); 
    java.lang.System.setProperty("user.dir", "/Users/testuser/Documents/NetBeansProjects/eidetic"); 
    java.lang.System.setProperty("java.io.tmpdir", "/var/folders/cs/d6h1sj7j2g9_zhmr2syzkrysl5mt07/T/"); 
    java.lang.System.setProperty("line.separator", "\n"); 
    java.lang.System.setProperty("java.specification.version", "1.8"); 
    java.lang.System.setProperty("awt.toolkit", "sun.lwawt.macosx.LWCToolkit"); 
    java.lang.System.setProperty("file.encoding", "UTF-8"); 
    java.lang.System.setProperty("file.separator", "/"); 
    java.lang.System.setProperty("java.awt.graphicsenv", "sun.awt.CGraphicsEnvironment"); 
    java.lang.System.setProperty("java.awt.printerjob", "sun.lwawt.macosx.CPrinterJob"); 
    java.lang.System.setProperty("java.class.path", "/var/folders/cs/d6h1sj7j2g9_zhmr2syzkrysl5mt07/T/EvoSuite_pathingJar7572732525332816664.jar"); 
    java.lang.System.setProperty("java.class.version", "52.0"); 
    java.lang.System.setProperty("java.endorsed.dirs", "/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/endorsed"); 
    java.lang.System.setProperty("java.ext.dirs", "/Users/testuser/Library/Java/Extensions:/Library/Java/JavaVirtualMachines/jdk1.8.0_25.jdk/Contents/Home/jre/lib/ext:/Library/Java/Extensions:/Network/Library/Java/Extensions:/System/Library/Java/Extensions:/usr/lib/java"); 
    java.lang.System.setProperty("java.library.path", "lib"); 
    java.lang.System.setProperty("java.runtime.name", "Java(TM) SE Runtime Environment"); 
    java.lang.System.setProperty("java.runtime.version", "1.8.0_25-b17"); 
    java.lang.System.setProperty("java.specification.name", "Java Platform API Specification"); 
    java.lang.System.setProperty("java.specification.vendor", "Oracle Corporation"); 
    java.lang.System.setProperty("java.vendor", "Oracle Corporation"); 
    java.lang.System.setProperty("java.vendor.url", "http://java.oracle.com/"); 
    java.lang.System.setProperty("java.version", "1.8.0_25"); 
    java.lang.System.setProperty("java.vm.info", "mixed mode"); 
    java.lang.System.setProperty("java.vm.name", "Java HotSpot(TM) 64-Bit Server VM"); 
    java.lang.System.setProperty("java.vm.specification.name", "Java Virtual Machine Specification"); 
    java.lang.System.setProperty("java.vm.specification.vendor", "Oracle Corporation"); 
    java.lang.System.setProperty("java.vm.specification.version", "1.8"); 
    java.lang.System.setProperty("java.vm.vendor", "Oracle Corporation"); 
    java.lang.System.setProperty("java.vm.version", "25.25-b02"); 
    java.lang.System.setProperty("os.arch", "x86_64"); 
    java.lang.System.setProperty("os.version", "10.10.5"); 
    java.lang.System.setProperty("path.separator", ":"); 
    java.lang.System.setProperty("user.country", "US"); 
    java.lang.System.setProperty("user.language", "en"); 
    java.lang.System.setProperty("user.name", "testuser"); 
    java.lang.System.setProperty("user.timezone", "America/Chicago"); 
  }

  private static void initializeClasses() {
    org.evosuite.runtime.classhandling.ClassStateSupport.initializeClasses(RDSClientMethods_ESTest_scaffolding.class.getClassLoader() ,
      "com.amazonaws.services.rds.model.CreateDBSnapshotRequest",
      "com.amazonaws.AmazonWebServiceClient",
      "com.pearson.eidetic.utilities.Threads",
      "com.amazonaws.services.rds.AmazonRDS",
      "com.amazonaws.services.rds.model.DescribeDBSnapshotsRequest",
      "com.amazonaws.services.rds.model.ListTagsForResourceResult",
      "com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsResult",
      "com.amazonaws.services.rds.model.DescribeDBInstancesResult",
      "com.amazonaws.services.rds.model.AddTagsToResourceRequest",
      "com.amazonaws.services.rds.model.DBClusterSnapshot",
      "com.amazonaws.services.rds.AmazonRDSClient",
      "com.amazonaws.services.rds.model.DescribeDBSnapshotsResult",
      "com.amazonaws.services.rds.model.DBSnapshot",
      "com.amazonaws.services.rds.model.Tag",
      "com.amazonaws.AmazonWebServiceRequest",
      "com.pearson.eidetic.aws.RDSClientMethods",
      "com.amazonaws.services.rds.model.DeleteDBClusterSnapshotRequest",
      "com.amazonaws.services.rds.model.DBInstance",
      "com.amazonaws.services.rds.model.ListTagsForResourceRequest",
      "com.amazonaws.services.rds.model.DeleteDBSnapshotRequest",
      "com.amazonaws.services.rds.model.DescribeDBClusterSnapshotsRequest",
      "com.amazonaws.services.rds.model.CreateDBClusterSnapshotRequest",
      "com.amazonaws.services.rds.model.DBCluster",
      "com.amazonaws.services.rds.model.DescribeDBClustersRequest",
      "com.pearson.eidetic.aws.AwsAccount",
      "com.amazonaws.services.rds.model.DescribeDBClustersResult",
      "com.amazonaws.services.rds.model.DescribeDBInstancesRequest",
      "com.pearson.eidetic.globals.GlobalVariables",
      "com.amazonaws.ReadLimitInfo"
    );
  } 

  private static void resetClasses() {
    org.evosuite.runtime.classhandling.ClassResetter.getInstance().setClassLoader(RDSClientMethods_ESTest_scaffolding.class.getClassLoader()); 

    org.evosuite.runtime.classhandling.ClassStateSupport.resetClasses(
      "com.pearson.eidetic.aws.RDSClientMethods",
      "com.pearson.eidetic.utilities.Threads",
      "com.pearson.eidetic.utilities.StackTrace",
      "com.pearson.eidetic.globals.GlobalVariables"
    );
  }
}