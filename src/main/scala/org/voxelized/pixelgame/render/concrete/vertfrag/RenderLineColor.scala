package org.voxelized.pixelgame.render.concrete.vertfrag

import org.voxelized.pixelgame.render.definition.{AttribPointersVertFragColor, RendererVertFragDefault}
import org.lwjgl.opengl.GL20.{glEnableVertexAttribArray, glVertexAttribPointer}
import russoul.lib.common._
import Implicits._
import org.lwjgl.opengl.GL11._

/**
  * Created by russoul on 20.07.2017.
  */
class RenderLineColor extends RendererVertFragDefault with AttribPointersVertFragColor{
  //override val vertexSize: Int = 6 already set in AttribPointersVertFragColor

  override def renderMode(): Int = GL_LINES

  override def getShaderName(): String = "color"


  def add(line: Line2F, zLevel: Float, color: Float3) : Unit = {
    vertexPool += line.start.x
    vertexPool += line.start.y
    vertexPool += zLevel

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    vertexPool += line.end.x
    vertexPool += line.end.y
    vertexPool += zLevel

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    indexPool += vertexCount
    indexPool += vertexCount + 1

    vertexCount += 2

  }
}
