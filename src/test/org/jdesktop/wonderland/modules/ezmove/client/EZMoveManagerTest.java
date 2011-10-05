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
        end.fromAngles(FastMath.DEG_TO_RAD*90f,0, 0);
        float[] endAngles = new float[3];
        endAngles = end.toAngles(endAngles);
        System.out.println("EndX: "+endAngles[0]*FastMath.RAD_TO_DEG
                            +"\nEndY: "+endAngles[1]*FastMath.RAD_TO_DEG
                            +"\nEndZ: "+endAngles[2]*FastMath.RAD_TO_DEG);

        Quaternion current = new Quaternion();
        current.fromAngles(FastMath.DEG_TO_RAD*120f, 0, 0);
                            //current.inverseLocal().mult(end);
        Quaternion delta  = end.mult(current.inverse());
        Quaternion testCase = current.mult(delta);
        float[] angles = new float[3];
        angles = delta.toAngles(angles);
        System.out.println("DeltaX: "+angles[0]*FastMath.RAD_TO_DEG+
                           "\nDeltaY: "+angles[1]*FastMath.RAD_TO_DEG+
                           "\nDeltaZ: "+angles[2]*FastMath.RAD_TO_DEG);

        float[] testAngles = new float[3];
         testAngles = testCase.toAngles(testAngles);
        System.out.println("TestX: "+testAngles[0]*FastMath.RAD_TO_DEG+
                            "\nTestY: "+testAngles[1]*FastMath.RAD_TO_DEG+
                            "\nTestZ: "+testAngles[2]*FastMath.RAD_TO_DEG);

        assertEquals(testAngles[1], endAngles[1], 0.5f);
    }

}