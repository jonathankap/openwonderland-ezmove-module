/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdesktop.wonderland.modules.ezmove.client;

import java.awt.event.KeyEvent;
import java.util.logging.Logger;
import org.jdesktop.wonderland.client.input.Event;
import org.jdesktop.wonderland.client.input.EventClassListener;
import org.jdesktop.wonderland.client.input.InputManager;
import org.jdesktop.wonderland.client.jme.input.KeyEvent3D;

/**
 *
 * @author spcworld
 */
public enum EZMoveManager {
    INSTANCE;

    private static final Logger LOGGER =
            Logger.getLogger(EZMoveManager.class.getName());

    private final EZMoveEventListener listener =
            new EZMoveEventListener();

    private boolean moveMode = false;

    public static EZMoveManager getInstance() {
        return INSTANCE;
    }

    public void register() {
        InputManager.inputManager().addGlobalEventListener(listener);
    }

    public void unregister() {
        InputManager.inputManager().removeGlobalEventListener(listener);
    }

    protected synchronized void enterMoveMode() {
        LOGGER.warning("Enter EZMove mode");
        moveMode = true;
    }

    protected synchronized void exitMoveMode() {
        LOGGER.warning("Leave EZMove mode");
        moveMode = false;
    }

    protected synchronized boolean isInMoveMode() {
        return moveMode;
    }

    class EZMoveEventListener extends EventClassListener {

        @Override
        public Class[] eventClassesToConsume() {
            return new Class[] { KeyEvent3D.class };
        }

        @Override
        public void commitEvent(Event event) {
            if (event instanceof KeyEvent3D) {
                KeyEvent3D e = (KeyEvent3D) event;
                if (e.getKeyCode() == KeyEvent.VK_ALT && e.isPressed()) {
                    enterMoveMode();
                } else if (e.getKeyCode() == KeyEvent.VK_ALT && e.isReleased()) {
                    exitMoveMode();
                }
            }
        }
    }
}
