/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jdesktop.wonderland.modules.ezmove.client;

import org.jdesktop.wonderland.client.BaseClientPlugin;
import org.jdesktop.wonderland.common.annotation.Plugin;

/**
 *
 * @author spcworld
 */
@Plugin
public class EZMoveClientPlugin extends BaseClientPlugin {

    @Override
    public void activate() {
        EZMoveManager.getInstance().register();
    }

    @Override
    public void deactivate() {
        EZMoveManager.getInstance().unregister();
    }   
}
