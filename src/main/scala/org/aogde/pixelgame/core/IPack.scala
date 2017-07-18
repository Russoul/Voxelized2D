package org.aogde.pixelgame.core

/**
  * Created by russoul on 17.07.2017.
  */
trait IPack {
  def name: String
  def version: String

  def init(registry: GameRegistry) : Unit

}
