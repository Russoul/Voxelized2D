package org.aogde.pixelgame.core

import org.aogde.pixelgame.pack.CorePack

/**
  * Created by russoul on 14.07.2017.
  */
object Startup extends App {

  val game = new PixelGame
  PixelGame.instance = game //static game instance for basic comforts

  game.registry.addPack(new CorePack) //adding core game pack



  val thread = new Thread(() => game.start()) //new thread which runs game.start()
  thread.start()

}
