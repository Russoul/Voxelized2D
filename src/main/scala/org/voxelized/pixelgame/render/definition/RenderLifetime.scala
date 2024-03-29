package org.voxelized.pixelgame.render.definition

/**
  * Created by russoul on 15.07.2017.
  */
trait RenderLifetime{
  def name():String
}

object LifetimeOneDraw extends RenderLifetime{
  override def name(): String = "LifetimeOneDraw"
}

object LifetimeManual extends RenderLifetime{ //will be rendered until manually removed
  override def name(): String = "LifetimeManual"
}
