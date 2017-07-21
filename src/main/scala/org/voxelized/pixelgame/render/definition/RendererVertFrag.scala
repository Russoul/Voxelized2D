package org.voxelized.pixelgame.render.definition

/**
  * Created by russoul on 15.07.2017.
  */
trait RendererVertFrag {
  def renderMode(): Int
  def setAttributePointers(): Unit

  def construct() : Boolean
  def deconstruct() : Boolean
  def draw() : Boolean

  def getShaderName(): String

}
