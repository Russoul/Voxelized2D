package org.voxelized.pixelgame.render.concrete.vertfrag

import org.lwjgl.opengl.GL11._
import org.voxelized.pixelgame.render.definition.{AttribPointersVertFragColor, AttribPointersVertFragTextureColor, RendererVertFragDefault}
import russoul.lib.common._
import Implicits._

class RenderTriangleTextureColor extends RendererVertFragDefault with AttribPointersVertFragTextureColor{
  override def renderMode(): Int = GL_TRIANGLES
  override def getShaderName(): String = "texture_color"


  /**
    *
    * @param triangle
    * @param zLevel
    * @param textureCoord matching triangle points p1,p2,p3
    * @param color
    */
  def add(triangle: Triangle2F, zLevel: Float, textureCoord : Array[Float2], color: Float3): Unit ={
    vertexPool += triangle.p1.x
    vertexPool += triangle.p1.y
    vertexPool += zLevel

    vertexPool += textureCoord(0).x
    vertexPool += textureCoord(0).y

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    vertexPool += triangle.p2.x
    vertexPool += triangle.p2.y
    vertexPool += zLevel

    vertexPool += textureCoord(1).x
    vertexPool += textureCoord(1).y

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    vertexPool += triangle.p3.x
    vertexPool += triangle.p3.y
    vertexPool += zLevel

    vertexPool += textureCoord(2).x
    vertexPool += textureCoord(2).y

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    indexPool += 0 + vertexCount
    indexPool += 1 + vertexCount
    indexPool += 2 + vertexCount

    vertexCount += 3

  }
}
