package org.jdesktop.wonderland.modules.ezmove.client;

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

}