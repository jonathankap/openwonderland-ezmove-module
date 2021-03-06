/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jdesktop.wonderland.modules.ezmove.client;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jdesktop.wonderland.client.cell.Cell;
import org.jdesktop.wonderland.client.cell.MovableComponent;
import org.jdesktop.wonderland.client.input.Event;
import org.jdesktop.wonderland.client.input.EventClassListener;
import org.jdesktop.wonderland.client.input.InputManager;
import org.jdesktop.wonderland.client.jme.ClientContextJME;
import org.jdesktop.wonderland.client.jme.ViewManager;
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
    
    //used for incrementing movement for keystrokes and mouse events.
    private static final float MOVE_INCREMENT = 0.05f;
    private static final float ZOOM_INCREMENT = 0.20f;
    private final EZMoveEnableListener enableListener =
            new EZMoveEnableListener();
    private final EZMoveMoveListener moveListener =
            new EZMoveMoveListener();
    private final CellSelectionManager selected = new CellSelectionManager();

    private boolean moveMode = false;
    private Vector3f lastTranslation;
    private Quaternion lastRotation;
    private boolean dragging = false;

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
        
        InputManager inputManager = InputManager.inputManager();
        inputManager.addGlobalEventListener(moveListener);

        // disable events to our avatar
        inputManager.removeKeyMouseFocus(inputManager.getGlobalFocusEntity());
    }

    protected synchronized void exitMoveMode() {
        LOGGER.warning("Leave EZMove mode");
        moveMode = false;

        selected.unregister();

        InputManager inputManager = InputManager.inputManager();
        inputManager.removeGlobalEventListener(moveListener);

        // enable events to our avatar
        inputManager.addKeyMouseFocus(inputManager.getGlobalFocusEntity());
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

    protected void startDrag() {
//        selectedCells = new ArrayList<Cell>();
//
//        for(Cell cell: selected.getSelectedCells()) {
//            MovableComponent mc = getMovable(cell);
//            if(mc != null) {
//                selectedCells.add(cell);
//            }
//        }
        //TODO: if a parent and children are both in the list, remove any children.
        dragging = true;

        lastTranslation = new Vector3f(Vector3f.ZERO);
        lastRotation = new Quaternion();
        LOGGER.warning("Starting drag");
    }
    protected void handleMove(Vector3f start, Vector3f end) {
        LOGGER.warning("Handling move, start: "+start+""
                + "\nlast: "+ lastTranslation +""
                + "\nend: "+end);
        Vector3f delta = end.subtract(lastTranslation);
        applyDelta(delta, new Quaternion());
    }

    protected void handleRotate(Vector3f start, Vector3f end, float direction) {
        LOGGER.warning("Handling rotate, start: "+start+""
                + "\nlast: "+lastTranslation+""
                + "\nend: "+end);
        Vector3f increment = end.subtract(lastTranslation);
        increment.y = 0;
        float length = increment.length();
        float magic = 3f; // ~ 180 degrees
        float percent = length/magic;

        // if the difference is negative, we need to rotate in the opposite
        // direction. Caclulate the difference after taking into account 
        // the direction of the camera
//        CellTransform viewTransform = ViewManager.getViewManager().getCameraTransform();
//        Quaternion viewRotation = viewTransform.getRotation(null);
//        increment = viewRotation.mult(increment);
//
//        LOGGER.warning("viewTransform: " + viewTransform + ", increment: " +
//                       increment);
//
//        if (increment.x < 0 || increment.z < 0) {
//            percent = -percent;
//        }

        // take direction into account
        percent *= direction;

        Quaternion rotation = new Quaternion();
        rotation.fromAngles(0f, percent*FastMath.PI, 0f);
      
        // rotation is the total rotation since we started moving the mouse,
        // so calculate just the delta to apply
        Quaternion deltaRotation = rotation.mult(lastRotation.inverse());

        LOGGER.warning("Percent: " + percent + " = "  +
                       toAnglesString(rotation) + ". Last = " +
                       toAnglesString(lastRotation) + ". Delta = " +
                       toAnglesString(deltaRotation));

        applyDelta(Vector3f.ZERO, deltaRotation);
    }

    private static String toAnglesString(Quaternion q) {
        float[] angles = q.toAngles(null);
        return "(X: " + (FastMath.RAD_TO_DEG * angles[0]) + ", " +
               "Y: " + (FastMath.RAD_TO_DEG * angles[1]) + ", " +
               "Z: " + (FastMath.RAD_TO_DEG * angles[2]) + ")";
    }


    protected void applyDelta(Vector3f deltaTranslation, Quaternion deltaRotation) {
        LOGGER.warning("Applying delta: "+deltaTranslation + " " + deltaRotation);

        boolean startedDrag = false;
        if (!dragging) {
            // no drag in progress. Start one now and end it after the drag
            // operation
            startDrag();
            startedDrag = true;
        }

        for(Cell cell : selected.getSelectedCells()) {
            CellTransform transform = cell.getLocalTransform();
            Vector3f translate = transform.getTranslation(null);
            Quaternion rotation = transform.getRotation(null);
            
            // if the cell has a parent, make sure to take the parent's
            // rotation and scale into account when applying the delta
            Vector3f localDeltaTranslation = deltaTranslation.clone();
            Cell parent = cell.getParent();
            if (parent != null) {
                CellTransform parentWorld = parent.getWorldTransform();
                Quaternion parentRotation = parentWorld.getRotation(null);
                float parentScale = parentWorld.getScaling();

                LOGGER.warning("Parent transform: " + parentWorld);

                // invert the rotation to get the child rotation
                parentRotation.inverseLocal();
                localDeltaTranslation = parentRotation.mult(deltaTranslation);
                localDeltaTranslation.multLocal(parentScale);

                LOGGER.warning("Local delta translation: " + localDeltaTranslation);
            }
            
            translate.addLocal(localDeltaTranslation);
            rotation.multLocal(deltaRotation);
            transform.setTranslation(translate);
            transform.setRotation(rotation);
            
            MovableComponent mc = getMovable(cell);
            if (mc != null) {
                mc.localMoveRequest(transform);
            }

        }
        lastTranslation.addLocal(deltaTranslation);
        lastRotation.multLocal(deltaRotation);
        // if we started a drag, remember to end it
        if (startedDrag) {
            endDrag();
        }
    }

    protected void endDrag() {
        LOGGER.warning("Drag ended");
        lastTranslation = null;
        lastRotation = null;
        dragging = false;
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
            LOGGER.warning("Received event: "+event.toString());

            if (event instanceof MouseButtonEvent3D) {
                MouseButtonEvent3D me = ((MouseButtonEvent3D) event);

                if(me.isPressed()) {
                MouseEvent awtMouseEvent = (MouseEvent)me.getAwtEvent();
                    startDragMouse = awtMouseEvent.getPoint();
                    startDragWorld = me.getIntersectionPointWorld();
                    startDrag();
                } else if(me.isReleased()) {
                    endDrag();
                }
            } else if (event instanceof MouseDraggedEvent3D) {

                MouseDraggedEvent3D dragEvent = (MouseDraggedEvent3D)event;
                MouseEvent awtMouseEvent = (MouseEvent) dragEvent.getAwtEvent();
                Vector3f endDragWorld = dragEvent.getDragVectorWorld(startDragWorld, startDragMouse, null);
                if(SwingUtilities.isLeftMouseButton(awtMouseEvent)){
                    handleMove(startDragWorld, endDragWorld);
                } else if(SwingUtilities.isRightMouseButton(awtMouseEvent)) {
                    float direction = 1f;
                    if (awtMouseEvent.getX() < startDragMouse.getX()) {
                        direction = -1f;
                    }

                    handleRotate(startDragWorld, endDragWorld, direction);
                }
            }else if (event instanceof MouseWheelEvent3D) {
                MouseWheelEvent3D wheelEvent = (MouseWheelEvent3D)event;
                int clicks = wheelEvent.getWheelRotation();
                
                //create vector based on unit z and scaled clicks from wheel event.
                Vector3f delta = new Vector3f(0, 0, clicks*-MOVE_INCREMENT);
                moveObject(delta);
                //grab the current camera's rotation.
//                Quaternion rotation = ClientContextJME.getViewManager().getCameraTransform().getRotation(null);
                //apply the rotation to our initial vector
//                delta = rotation.mult(delta);

                //apply the delta to any and all selected cells
//                applyDelta(delta, new Quaternion());
            } else if (event instanceof KeyEvent3D) {
                KeyEvent3D keyEvent = (KeyEvent3D)event;
                switch(keyEvent.getKeyCode()) {
                    case KeyEvent.VK_MINUS:
                        moveObject(new Vector3f(0, 0, -ZOOM_INCREMENT));
                        break;
                    case KeyEvent.VK_PLUS:
                    case KeyEvent.VK_EQUALS:
                        moveObject(new Vector3f(0, 0, ZOOM_INCREMENT));
                        break;
                    case KeyEvent.VK_UP:
                        moveObject(new Vector3f(0,MOVE_INCREMENT,0));
                        break;
                    case KeyEvent.VK_DOWN:
                        moveObject(new Vector3f(0, -MOVE_INCREMENT, 0));
                        break;
                    case KeyEvent.VK_LEFT:
                        moveObject(new Vector3f(MOVE_INCREMENT,0,0));
                        break;
                    case KeyEvent.VK_RIGHT:
                        moveObject(new Vector3f(-MOVE_INCREMENT, 0, 0));
                        break;
                }
            }
        }

        private void moveObject(Vector3f delta) {
//            Vector3f delta = new Vector3f(0, 0, -0.2f);
            Quaternion rotation = ClientContextJME.getViewManager().getCameraTransform().getRotation(null);
            delta = rotation.mult(delta);
            applyDelta(delta, new Quaternion());
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
