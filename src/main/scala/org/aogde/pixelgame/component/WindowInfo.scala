package org.aogde.pixelgame.component
import org.lwjgl.system.MemoryUtil._

/**
  * Created by russoul on 14.07.2017.
  */
class WindowInfo(private var width: Float, private var height: Float, private var id: Long = NULL) {

  def getWidth() = width
  def getHeight() = height
  def setWidth(width: Float) = this.width = width
  def setHeight(height: Float) = this.height = height
  def getID() = id
  def setID(id: Long) = this.id = id

  def const() = WindowInfoConst(width, height, id)
}

case class WindowInfoConst(width: Float, height: Float, id: Long)
