/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jdesktop.wonderland.modules.ezmove.client;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.Geometry;
import com.jme.scene.Spatial;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.jdesktop.mtgame.Entity;
import org.jdesktop.mtgame.RenderComponent;
import org.jdesktop.mtgame.RenderUpdater;
import org.jdesktop.mtgame.processor.WorkProcessor.WorkCommit;
import org.jdesktop.wonderland.client.cell.Cell;
import org.jdesktop.wonderland.client.cell.MovableComponent;
import org.jdesktop.wonderland.client.input.Event;
import org.jdesktop.wonderland.client.input.EventClassListener;
import org.jdesktop.wonderland.client.input.InputManager;
import org.jdesktop.wonderland.client.jme.ClientContextJME;
import org.jdesktop.wonderland.client.jme.SceneWorker;
import org.jdesktop.wonderland.client.jme.cellrenderer.CellRendererJME;
import org.jdesktop.wonderland.client.jme.input.MouseEvent3D;
import org.jdesktop.wonderland.client.jme.utils.traverser.ProcessNodeInterface;
import org.jdesktop.wonderland.client.jme.utils.traverser.TreeScan;
import org.jdesktop.wonderland.client.scenemanager.SceneManager;
import org.jdesktop.wonderland.client.scenemanager.event.SelectionEvent;
import org.jdesktop.wonderland.common.cell.CellID;
import org.jdesktop.wonderland.common.cell.messages.CellServerComponentMessage;
import org.jdesktop.wonderland.common.messages.ErrorMessage;
import org.jdesktop.wonderland.common.messages.ResponseMessage;

/**
 * Taken from top-placement module
 * @author jkaplan
 */
public class CellSelectionManager extends EventClassListener {
    private static Logger LOGGER =
            Logger.getLogger(CellSelectionManager.class.getName());

    private final Set<Cell> selected = new LinkedHashSet<Cell>();
    private static final ColorRGBA GLOW_COLOR = new ColorRGBA(1.0f, 0, 0, 0.5f);
    private static final Vector3f GLOW_SCALE = new Vector3f(1.1f, 1.1f, 1.1f);

    private final SelectionMouseListener mouseListener = new SelectionMouseListener();
    private EZMoveSceneManager sceneManager;

    public void register() {
        if (sceneManager != null) {
            throw new IllegalStateException("Scene manager already exists");
        }

        sceneManager = new EZMoveSceneManager();
        sceneManager.addSceneListener(this);

        InputManager.inputManager().addGlobalEventListener(mouseListener);
    }

    public void unregister() {
        InputManager.inputManager().removeGlobalEventListener(mouseListener);

        if (sceneManager == null) {
            return;
        }

        sceneManager.removeSceneListener(this);

        // cleanup will clear the selection
        sceneManager.cleanup();
        sceneManager = null;

        // we won't get the message about clearing the selection,
        // so make sure to manually remove all selection we have
        // made
        deselectAll();
    }

    public synchronized boolean areCellsSelected() {
        return !selected.isEmpty();
    }

    public synchronized Set<Cell> getSelectedCells() {
        // return a copy
        return new LinkedHashSet<Cell>(selected);
    }

    @Override
    public Class[] eventClassesToConsume() {
        return new Class[]{ SelectionEvent.class };
    }

    @Override
    public void commitEvent(Event event) {
        LOGGER.warning("Received event in CellSelectionManager: "+event.toString());
        Set<Cell> addList = new LinkedHashSet<Cell>();
        Set<Cell> removeList = getSelectedCells();

        // sync up our view of selected entities with the scene manager's
        for (Entity e : sceneManager.getSelectedEntities()) {
            Cell cell = SceneManager.getCellForEntity(e);
            if (cell != null) {
                addList.add(cell);
            }
        }

        // remove all cells that should be there from the list of current cells.
        // The remaining cells are the cells that need to be removed.
        removeList.removeAll(addList);
        for (Cell cell : removeList) {
            deselect(cell);
        }

        // remove all cells that were previously added from the list of cells to
        // add.  This gives just the new cells.
        addList.removeAll(getSelectedCells());
        for (Cell cell : addList) {
            select(cell);
        }
    }

    private synchronized void deselectAll() {
        for (Cell cell : getSelectedCells().toArray(new Cell[selected.size()])) {
            deselect(cell);
        }
    }

    // must be called on the commit thread while holding the lock on
    // this class
    private void select(Cell cell) {
        // do something to show selection
        highlightCell(cell, true, GLOW_COLOR);

        // this is a good time to make sure the cell has a movable component,
        // so that we know we can move it later
        checkMovableComponent(cell);

        selected.add(cell);
    }

    // must be called on the commit thread while holding the lock on
    // this class
    private void deselect(Cell cell) {
        selected.remove(cell);

        // undo whatever we were doing to show selection
        highlightCell(cell, false, GLOW_COLOR);
    }

    private void checkMovableComponent(final Cell cell) {
        // First try to add the movable component. The presence of the
        // movable component will determine when we actually add the
        // affordance to the scene graph. We do this in a separate thread
        // because adding the movable component is a synchronous call (it
        // waits for a response message. That would block the thread calling
        // the setStatus() method. This won't work since this thread cannot
        // be blocked when adding the component.
        new Thread() {
            @Override
            public void run() {
                MovableComponent mc = cell.getComponent(MovableComponent.class);
                if (mc == null) {
                    addMovableComponent(cell);
                }
            }
        }.start();
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
            LOGGER.warning("Unable to add movable component for Cell" +
                    cell.getName() + " with ID " + cell.getCellID());
        }
    }
    /**
     * We assume this is called on the MT-Game render thread
     * @param cell
     * @param highlight
     * @param color
     */
    public void highlightCell(final Cell cell, final boolean highlight, final ColorRGBA color) {

        CellRendererJME r = (CellRendererJME) cell.getCellRenderer(Cell.RendererType.RENDERER_JME);
        Entity entity = r.getEntity();
        RenderComponent rc = entity.getComponent(RenderComponent.class);

        if (rc == null) {
            LOGGER.warning("RenderComponent is NULL for: " + cell);
            return;
        }


        TreeScan.findNode(rc.getSceneRoot(), Geometry.class, new ProcessNodeInterface() {

            public boolean processNode(final Spatial s) {
                s.setGlowEnabled(highlight);
                s.setGlowColor(color);
                s.setGlowScale(GLOW_SCALE);
                ClientContextJME.getWorldManager().addToUpdateList(s);

                return true;
            }
        }, false, false);
    }

    /**
     * Extension of SceneManager to handle selection even when the global
     * entity is not selected
     */
    class EZMoveSceneManager extends SceneManager {
        @Override
        public void inputEvent(Event event) {
            super.inputEvent(event);
        }
    }

    /**
     * Mouse listener that feeds events to the scene manager
     */
    class SelectionMouseListener extends EventClassListener {

        @Override
        public Class[] eventClassesToConsume() {
            return new Class[] { MouseEvent3D.class };
        }

        @Override
        public void commitEvent(Event event) {
            if (sceneManager != null) {
                sceneManager.inputEvent(event);
            }
        }
    }
}
