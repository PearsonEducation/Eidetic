/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.pearson.eidetic.driver.threads.subthreads;

import com.amazonaws.services.ec2.model.Snapshot;
import com.amazonaws.services.ec2.model.Volume;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Development
 */
public class SnapshotVolumeNoTimeTest {
    
    public SnapshotVolumeNoTimeTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of run method, of class SnapshotVolumeNoTime.
     */
    @Test
    public void testRun() {

    }

    /**
     * Test of sortSnapshots method, of class SnapshotVolumeNoTime.
     */
    @Test
    public void testSortSnapshots() {
        List<Snapshot> sortedCompareList = new ArrayList<>();
        // fill sortedCompareList with snapshot objects
        
    }

    /**
     * Test of getHoursBetweenSnapshots method, of class SnapshotVolumeNoTime.
     */
    @Test
    public void testGetHoursBetweenSnapshots() {
        List<Snapshot> compareList = new ArrayList<>();
        // fill sortedCompareList with snapshot objects that you hand-craft
        
        // create SnapshotVolumeNoTime object. i left it null for now.
        SnapshotVolumeNoTime snapshotVolumeNoTime = null;
        
        int expectedResult = 0;
        //int result = snapshotVolumeNoTime.getHoursBetweenSnapshots(compareList);
        //assertEquals(expectedResult, result);
    }

    /**
     * Test of getDaysBetweenSnapshots method, of class SnapshotVolumeNoTime.
     */
    @Test
    public void testGetDaysBetweenSnapshots() {
        List<Snapshot> compareList = new ArrayList<>();
        // fill sortedCompareList with snapshot objects that you hand-craft
        
        // create SnapshotVolumeNoTime object. i left it null for now.
        SnapshotVolumeNoTime snapshotVolumeNoTime = null;
        
        int expectedResult = 0;
        //int result = snapshotVolumeNoTime.getDaysBetweenSnapshots(compareList);
        //assertEquals(expectedResult, result);
    }
    
}
