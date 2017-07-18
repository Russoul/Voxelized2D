package org.aogde.pixelgame.render.concrete.vertfrag

import org.aogde.pixelgame.render.definition.RendererVertFragDefault
import org.lwjgl.opengl.GL11.{GL_FLOAT, GL_TRIANGLES}
import org.lwjgl.opengl.GL20.{glEnableVertexAttribArray, glVertexAttribPointer}
import russoul.lib.common._
import Implicits._


/**
  * Created by russoul on 17.07.2017.
  */
class RenderTriangleColor extends RendererVertFragDefault{
  override val renderMode: Int = GL_TRIANGLES
  override val vertexSize: Int = 6 //vec3 as pos with depth + vec3 as color


  def add(triangle: Triangle2F, zLevel: Float, color: Float3): Unit ={
    vertexPool += triangle.p1.x
    vertexPool += triangle.p1.y
    vertexPool += zLevel

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    vertexPool += triangle.p2.x
    vertexPool += triangle.p2.y
    vertexPool += zLevel

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z


    vertexPool += triangle.p3.x
    vertexPool += triangle.p3.y
    vertexPool += zLevel

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    indexPool += 0 + vertexCount
    indexPool += 1 + vertexCount
    indexPool += 2 + vertexCount

    vertexCount += 3

  }

  override def setAttributePointers(): Unit = {
    glVertexAttribPointer(0, 3, GL_FLOAT, false, vertexSize * 4, 0)
    glEnableVertexAttribArray(0)

    glVertexAttribPointer(1, 3, GL_FLOAT, false, vertexSize * 4, 3 * 4)
    glEnableVertexAttribArray(1)
  }

  override def getShaderName(): String = "color"
}
