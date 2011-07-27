/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdesktop.wonderland.modules.ezmove.client;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.jdesktop.wonderland.client.cell.Cell;
import org.jdesktop.wonderland.client.cell.MovableComponent;
import org.jdesktop.wonderland.client.input.Event;
import org.jdesktop.wonderland.client.input.EventClassListener;
import org.jdesktop.wonderland.client.input.InputManager;
import org.jdesktop.wonderland.client.jme.ClientContextJME;
import org.jdesktop.wonderland.client.jme.input.KeyEvent3D;
import org.jdesktop.wonderland.client.jme.input.MouseButtonEvent3D;
import org.jdesktop.wonderland.client.jme.input.MouseDraggedEvent3D;
import org.jdesktop.wonderland.client.jme.input.MouseEvent3D;
import org.jdesktop.wonderland.client.jme.input.MouseWheelEvent3D;
import org.jdesktop.wonderland.common.cell.CellID;
import org.jdesktop.wonderland.common.cell.CellTransform;
import org.jdesktop.wonderland.common.cell.messages.CellServerComponentMessage;
import org.jdesktop.wonderland.common.messages.ErrorMessage;
import org.jdesktop.wonderland.common.messages.ResponseMessage;

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
    private Vector3f lastDrag;
    private List<Cell> selectedCells;

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

    
        /**
     * Adds the movable component, assumes it does not already exist.
     */
    private void addMovableComponent(Cell cell) {
        
        // Go ahead and try to add the affordance. If we cannot, then log an
        // error and return.
        CellID cellID = cell.getCellID();
        String className = "org.jdesktop.wonderland.server.cell.MovableComponentMO";
        CellServerComponentMessage cscm =
                CellServerComponentMessage.newAddMessage(cellID, className);
        ResponseMessage response = cell.sendCellMessageAndWait(cscm);
        if (response instanceof ErrorMessage) {
            LOGGER.warning("Unable to add movable component for Cell"
                    + cell.getName() + " with ID " + cell.getCellID());
        }

}

    protected MovableComponent getMovable(Cell cell) {
       MovableComponent mc = cell.getComponent(MovableComponent.class);
        if(mc != null) {
           return mc;
       } else {
           addMovableComponent(cell);
           return cell.getComponent(MovableComponent.class);
       }
    }

    protected void startDrag(Vector3f start) {
        selectedCells = new ArrayList<Cell>();
        
        for(Cell cell: selected.getSelectedCells()) {
            MovableComponent mc = getMovable(cell);
            if(mc != null) {
                selectedCells.add(cell);
            }
        }
        //TODO: if a parent and children are both in the list, remove any children.

        lastDrag = Vector3f.ZERO;
        LOGGER.warning("Starting drag: "+start);
    }
    protected void handleDrag(Vector3f start, Vector3f end) {
        LOGGER.warning("Handling drag, start: "+start+""
                + "\nlast: "+ lastDrag +""
                + "\nend: "+end);
        Vector3f delta = end.subtract(lastDrag);
        applyDelta(delta);
    }

    protected void applyDelta(Vector3f delta) {
        LOGGER.warning("Applying delta: "+delta);
        for(Cell cell: selectedCells) {
            CellTransform transform = cell.getLocalTransform();
            Vector3f translate = transform.getTranslation(null);
            translate.addLocal(delta);
            transform.setTranslation(translate);
            getMovable(cell).localMoveRequest(transform);

        }
        lastDrag.addLocal(delta);
    }

    protected void endDrag() {
        LOGGER.warning("Drag ended");
        lastDrag = null;
        selectedCells = null;
    }
    class EZMoveMoveListener extends EventClassListener {
        private Point startDragMouse;
        private Vector3f startDragWorld;
        @Override
        public Class[] eventClassesToConsume() {
            return new Class[] { KeyEvent3D.class, MouseEvent3D.class };
        }

        @Override
        public void commitEvent(Event event) {
            if (event instanceof MouseButtonEvent3D) {
                MouseButtonEvent3D me = ((MouseButtonEvent3D) event);

                if(me.isPressed()) {
                MouseEvent awtMouseEvent = (MouseEvent)me.getAwtEvent();
                    startDragMouse = awtMouseEvent.getPoint();
                    startDragWorld = me.getIntersectionPointWorld();
                    startDrag(startDragWorld);
                } else if(me.isReleased()) {
                    endDrag();
                }
            } else if (event instanceof MouseDraggedEvent3D) {
                MouseDraggedEvent3D dragEvent = (MouseDraggedEvent3D)event;
                Vector3f endDragWorld = dragEvent.getDragVectorWorld(startDragWorld, startDragMouse, null);

                handleDrag(startDragWorld, endDragWorld);



            }else if (event instanceof MouseWheelEvent3D) {
                MouseWheelEvent3D wheelEvent = (MouseWheelEvent3D)event;
                int clicks = wheelEvent.getWheelRotation();
                //create vector based on unit z and scaled clicks from wheel event.
                Vector3f delta = new Vector3f(0, 0, clicks*-0.2f);
                //grab the current camera's rotation.
                Quaternion rotation = ClientContextJME.getViewManager().getCameraTransform().getRotation(null);
                //apply the rotation to our initial vector
                delta = rotation.mult(delta);

                //apply the delta to any and all selected cells
                applyDelta(delta);
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
