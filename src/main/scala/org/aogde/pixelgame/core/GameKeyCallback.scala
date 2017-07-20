package org.aogde.pixelgame.core

import org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose
import org.lwjgl.glfw.GLFWKeyCallbackI
import org.lwjgl.glfw.GLFW._
import russoul.lib.common.utils.Arr

import scala.collection.mutable

/**
  * Created by russoul on 16.07.2017.
  */
class GameKeyCallback(private val game: Voxelized2D) extends GLFWKeyCallbackI {

  val callbacks = new Arr[GLFWKeyCallbackI]()


  override def invoke(window: Long, key: Int, scancode: Int, action: Int, mods: Int): Unit = {
    if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop

    for(callback <- callbacks){
      callback.invoke(window, key, scancode, action, mods)
    }

  }
}
