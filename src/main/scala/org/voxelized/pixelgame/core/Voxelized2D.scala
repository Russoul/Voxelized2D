package org.voxelized.pixelgame.core

import java.nio.IntBuffer

import org.voxelized.pixelgame.component.WindowInfo
import org.voxelized.pixelgame.config.Defaults
import org.voxelized.pixelgame.render.RenderingEngine
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW._
import org.lwjgl.glfw.{GLFWErrorCallback, GLFWVidMode}
import org.lwjgl.opengl.{GL, GL11}
import org.lwjgl.opengl.GL11._
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import russoul.lib.common._

/**
  * Created by russoul on 14.07.2017.
  */

object Voxelized2D{
  var instance : Voxelized2D = _ //TODO good idea ?
}
class Voxelized2D{

  val renderingEngine = new RenderingEngine(this)
  val keyCallback = new GameKeyCallback(this)
  val mouseCallback = new GameMouseCallback(this)
  val registry = new GameRegistry(this)
  private val windowInfo = new WindowInfo(Defaults.initialWindowWidth, Defaults.initialWindowHeight)

  def getConstWindowInfo() = windowInfo.const()

  def start(): Unit ={
    println("Using LWJGL " + Version.getVersion)

    initGraphics()
    initGame()
    run()

    deinitGame()

    // Free the window callbacks and destroy the window
    glfwFreeCallbacks(windowInfo.getID())
    glfwDestroyWindow(windowInfo.getID())

    // Terminate GLFW and free the error callback
    glfwTerminate()
    glfwSetErrorCallback(null).free()
  }

  private def initGraphics(): Unit = {
    // Setup an error callback. The default implementation
    // will print the error message in System.err.
    GLFWErrorCallback.createPrint(System.err).set

    // Initialize GLFW. Most GLFW functions will not work before doing this.
    if (!glfwInit) throw new IllegalStateException("Unable to initialize GLFW")

    // Configure GLFW
    glfwDefaultWindowHints() // optional, the current window hints are already the default

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE) // the window will stay hidden after creation

    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE) // the window will be resizable


    // Create the window
    windowInfo.setID(glfwCreateWindow(windowInfo.getWidth().toInt, windowInfo.getHeight().toInt, "Pixel-Game", NULL, NULL))
    if (windowInfo.getID() == NULL) throw new RuntimeException("Failed to create the GLFW window")

    // Setup a key callback. It will be called every time a key is pressed, repeated or released.
    glfwSetKeyCallback(windowInfo.getID(), keyCallback)

    //mouse button callback
    glfwSetMouseButtonCallback(windowInfo.getID(), mouseCallback)


    auto(stackPush){ stack =>
      val pWidth = stack.mallocInt(1)
      // int*
      val pHeight = stack.mallocInt(1)
      // Get the window size passed to glfwCreateWindow
      glfwGetWindowSize(windowInfo.getID(), pWidth, pHeight)
      // Get the resolution of the primary monitor
      val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor)
      // Center the window
      glfwSetWindowPos(windowInfo.getID(), (vidmode.width - pWidth.get(0)) / 2, (vidmode.height - pHeight.get(0)) / 2)
    }


    // Make the OpenGL context current
    glfwMakeContextCurrent(windowInfo.getID())
    // Enable v-sync
    glfwSwapInterval(1)

    // Make the window visible
    glfwShowWindow(windowInfo.getID())

    // This line is critical for LWJGL's interoperation with GLFW's
    // OpenGL context, or any context that is managed externally.
    // LWJGL detects the context that is current in the current thread,
    // creates the GLCapabilities instance and makes the OpenGL
    // bindings available for use.
    GL.createCapabilities

    glfwSetFramebufferSizeCallback(windowInfo.getID(), (win, width, height) => {
      windowInfo.setWidth(width)
      windowInfo.setHeight(height)

      glViewport(0,0,width, height)
    })

    println("Graphics initialized")
  }

  private def initGame(): Unit ={
    renderingEngine.System.Init.loadDefaultShaders(Defaults.defaultShaderPath, Defaults.defaultShaders)
    println("Shaders loaded")

    registry.System.init()
    println("All packs loaded")
  }

  private def deinitGame() : Unit = {
    registry.System.deinit()
    println("All packs unloaded")
  }

  private def run(): Unit ={


    // Set the clear color
    glClearColor(1.0f, 0.0f, 0.0f, 0.0f)

    // Run the rendering loop until the user has attempted to close
    // the window or has pressed the ESCAPE key.
    while ( {
      !glfwWindowShouldClose(windowInfo.getID())
    }) {
      events()
      update()
      draw()
    }
  }

  def update(): Unit = {
    registry.System.gameUpdate(windowInfo.const())
  }

  def events(): Unit ={
    // Poll for window events. The key callback above will only be
    // invoked during this call.
    glfwPollEvents()
  }

  def draw(): Unit = { //no manual drawing should be done here, all calls should use RenderingEngine pushes and be performed in update function
    renderingEngine.System.Render.draw(windowInfo)
  }
}
