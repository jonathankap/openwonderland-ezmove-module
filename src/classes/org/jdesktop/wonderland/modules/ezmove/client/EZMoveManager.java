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
import org.jdesktop.wonderland.client.jme.input.MouseButtonEvent3D;
import org.jdesktop.wonderland.client.jme.input.MouseEvent3D;

/**
 *
 * @author spcworld
 */
public enum EZMoveManager {
    INSTANCE;

    private static final Logger LOGGER =
            Logger.getLogger(EZMoveManager.class.getName());

    private final EZMoveEnableListener enableListener =
            new EZMoveEnableListener();
    private final EZMoveMoveListener moveListener =
            new EZMoveMoveListener();
    private final CellSelectionManager selected = new CellSelectionManager();

    private boolean moveMode = false;

    public static EZMoveManager getInstance() {
        return INSTANCE;
    }

    public void register() {
        InputManager.inputManager().addGlobalEventListener(enableListener);
    }

    public void unregister() {
        InputManager.inputManager().removeGlobalEventListener(enableListener);
    }

    protected synchronized void enterMoveMode() {
        LOGGER.warning("Enter EZMove mode");
        moveMode = true;

        selected.register();
        InputManager.inputManager().addGlobalEventListener(moveListener);
    }

    protected synchronized void exitMoveMode() {
        LOGGER.warning("Leave EZMove mode");
        moveMode = false;

        selected.unregister();
        InputManager.inputManager().removeGlobalEventListener(moveListener);
    }

    protected synchronized boolean isInMoveMode() {
        return moveMode;
    }

    class EZMoveMoveListener extends EventClassListener {
        @Override
        public Class[] eventClassesToConsume() {
            return new Class[] { KeyEvent3D.class, MouseEvent3D.class };
        }

        @Override
        public void commitEvent(Event event) {
            if (event instanceof MouseButtonEvent3D) {
                MouseButtonEvent3D me = ((MouseButtonEvent3D) event);
                
            }
        }
    }

    class EZMoveEnableListener extends EventClassListener {

        @Override
        public Class[] eventClassesToConsume() {
            return new Class[] { KeyEvent3D.class };
        }

        @Override
        public void commitEvent(Event event) {
            if (event instanceof KeyEvent3D) {
                KeyEvent3D e = (KeyEvent3D) event;
                if (e.getKeyCode() == KeyEvent.VK_ALT && e.isPressed() && !isInMoveMode()) {
                    enterMoveMode();
                } else if (e.getKeyCode() == KeyEvent.VK_ALT && e.isReleased() && isInMoveMode()) {
                    exitMoveMode();
                }
            }
        }
    }
}
