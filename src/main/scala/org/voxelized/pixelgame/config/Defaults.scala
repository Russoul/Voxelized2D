package org.voxelized.pixelgame.config

import russoul.lib.common._

/**
  * Created by russoul on 14.07.2017.
  */
object Defaults {

  final val initialWindowWidth = 800F
  final val initialWindowHeight = 600F

  final val initialBackgroundColor = Float3(135F/255F, 206F/255F, 250F/255F) //sky color

  final val defaultShaderPath = "src/main/resources/shaders/" //TODO path will not work on shipped game

  final val defaultShaders = Seq("color"->"color", "texture"->"texture", "texture_color"->"texture_color")

}
