package org.aogde.pixelgame.render.definition

/**
  * Created by russoul on 15.07.2017.
  */
trait RenderLifetime{
  def name():String
}

object LifetimeOneDraw extends RenderLifetime{
  override def name(): String = "LifetimeOneDraw"
}
