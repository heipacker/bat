/**
 * add some extension class, for example: Configuration, TNameManager
 * <p>
 * 1.you may need hot config, some you can implement Configuration, call callback method when config changed.
 * 2.you application may need on unique name, so you can implement TNameManager
 * <p>
 * note that:
 * you should implement index method, and return a low integer which lower than default value 300.
 *
 * @author heipacker
 * @date 16-5-28.
 */
package com.dlmu.bat.plugin;