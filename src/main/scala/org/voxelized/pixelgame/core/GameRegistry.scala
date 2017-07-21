package org.voxelized.pixelgame.core

import org.voxelized.pixelgame.component.WindowInfoConst
import org.voxelized.pixelgame.render.RenderingEngine
import org.lwjgl.glfw.{GLFWKeyCallback, GLFWKeyCallbackI}
import russoul.lib.common.utils.Arr

/**
  * Created by russoul on 16.07.2017.
  */

//this class should be used to register and provide for use ANY game action (logic,events,updates,rendering,sounds,tiles,...)
//TODO for now all registry is common, later add support for packages (games ships with core package, modifications can be provided by adding custom packages)
class GameRegistry(private val game: Voxelized2D) { registry =>

  private val packs = new Arr[IPack]()

  private val gameUpdateCallbacks = new Arr[(WindowInfoConst, RenderingEngine) => Unit]()

  object System{  //called by PixelGame

    //for packs
    def init(): Unit ={
      packs.foreach(_.init(registry))
    }

    def deinit() : Unit = {
      packs.foreach(_.deinit(registry))
    }

    def gameUpdate(windowInfo: WindowInfoConst) : Unit = {
      gameUpdateCallbacks.foreach(_(windowInfo, game.renderingEngine))
    }
    //............
  }

  object Pack{ //code for packs

    val renderer = game.renderingEngine
    def windowInfoConst() = game.getConstWindowInfo()


    def addKeyCallback(callback: GLFWKeyCallbackI) : Boolean = {
      if(!game.keyCallback.callbacks.contains(callback)){
        game.keyCallback.callbacks += callback

        true
      }else
        false

    }

    def removeKeyCallback(callback: GLFWKeyCallbackI) : Boolean = {
      val find = game.keyCallback.callbacks.find(callback)

      if(find.nonEmpty){
        game.keyCallback.callbacks.remove(find.get)

        true
      }else
        false
    }

    def addGameUpdateCallback(f: (WindowInfoConst, RenderingEngine) => Unit): Boolean ={
      if(!gameUpdateCallbacks.contains(f)){
        gameUpdateCallbacks += f

        true
      }else
        false
    }

    def removeGameUpdateCallback(f: (WindowInfoConst, RenderingEngine) => Unit) : Boolean = {
      val find = gameUpdateCallbacks.find(f)

      if(find.nonEmpty){
        gameUpdateCallbacks.remove(find.get)

        true
      }else
        false
    }
  }

  def addPack(pack: IPack) : Unit = {
    packs += pack
  }

}
