package org.jdesktop.wonderland.modules.ezmove.client;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jos
 */
public class EZMoveManagerTest {
   
    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void testGetInstance() throws IOException{
        EZMoveManager manager = EZMoveManager.getInstance();
        assertNotNull(manager);
    }

    //@Test
    public void testSetMode() {
        EZMoveManager manager = EZMoveManager.getInstance();
        manager.enterMoveMode();
        assertTrue(manager.isInMoveMode());

        manager.exitMoveMode();
        assertFalse(manager.isInMoveMode());
    }

    @Test
    public void testQuaternionMath() {
        Quaternion start = new Quaternion();
        Quaternion end = new Quaternion();
        System.out.println(end);
        end.fromAngles(0, FastMath.DEG_TO_RAD*90f, 0);
        float[] endAngles = new float[3];
        endAngles = end.toAngles(endAngles);
        System.out.println("EndX: "+endAngles[0]*FastMath.RAD_TO_DEG
                            +"\nEndY: "+endAngles[1]*FastMath.RAD_TO_DEG
                            +"\nEndZ: "+endAngles[2]*FastMath.RAD_TO_DEG);

        Quaternion current = new Quaternion();
        current.fromAngles(0, FastMath.DEG_TO_RAD*15f, 0);
                            //current.inverseLocal().mult(end);
        Quaternion delta  = end.subtract(current);
        Quaternion testCase = current.add(delta);
        float[] angles = new float[3];
        angles = delta.toAngles(angles);
        System.out.println("DeltaY: "+angles[1]*FastMath.RAD_TO_DEG+
                           "\nDeltaX: "+angles[0]*FastMath.RAD_TO_DEG+
                           "\nDeltaZ: "+angles[2]*FastMath.RAD_TO_DEG);
        assertEquals(testCase, end);
    }

}