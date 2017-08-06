package org.voxelized.pixelgame.core

import org.lwjgl.glfw.{GLFWMouseButtonCallback, GLFWMouseButtonCallbackI}
import russoul.lib.common.utils.Arr

class GameMouseCallback(game : Voxelized2D) extends GLFWMouseButtonCallbackI{

  val callbacks = new Arr[GLFWMouseButtonCallbackI]()

  override def invoke(window: Long, button: Int, action: Int, mods: Int): Unit = {
    for(callback <- callbacks){
      callback.invoke(window, button, action, mods)
    }
  }
}
