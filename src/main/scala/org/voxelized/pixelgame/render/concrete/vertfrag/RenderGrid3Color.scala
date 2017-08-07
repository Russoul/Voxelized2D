package org.voxelized.pixelgame.render.concrete.vertfrag

import org.voxelized.pixelgame.render.definition.{AttribPointersVertFragColor, RendererVertFragDefault}
import org.lwjgl.opengl.GL11._
import russoul.lib.common._
import Implicits._

/**
  * Created by russoul on 20.07.2017.
  */
class RenderGrid3Color extends RendererVertFragDefault with AttribPointersVertFragColor{
  //override val vertexSize: Int = 6 already set in AttribPointersVertFragColor

  override def renderMode(): Int = GL_LINES

  override def getShaderName(): String = "color"

  def getVertexPool() = vertexPool


  /**
    *
    * @param extent extent of grid square
    * @param subdivnumber subdivison number for extent
    * @param color
    * @param transformation by default grid lies on XOZ plane, with center = O(0,0)
    */
  def add(extent:Float, subdivnumber:Int, color:Float3, transformation: Mat4F = Mat4F.identity())
  {
    vertexPool ++= transformf(Float3(-extent, 0, -extent), transformation).toArray
    vertexPool ++= color.toArray
    vertexPool ++= transformf(Float3(extent, 0, -extent) , transformation).toArray
    vertexPool ++= color.toArray
    vertexPool ++= transformf(Float3(extent, 0, extent) , transformation).toArray
    vertexPool ++= color.toArray
    vertexPool ++= transformf(Float3(-extent, 0, extent) , transformation).toArray
    vertexPool ++= color.toArray

    val a = extent/subdivnumber
    for(i <- 1 until 2*subdivnumber)
    {
      vertexPool ++= transformf(Float3(-extent + i * a, 0, -extent) , transformation).toArray
      vertexPool ++= color.toArray

    }

    for(i <- 1 until 2*subdivnumber)
    {
      vertexPool ++= transformf(Float3(extent, 0, -extent + i * a) , transformation).toArray
      vertexPool ++= color.toArray
    }

    for(i <- 1 until 2*subdivnumber)
    {
      vertexPool ++= transformf(Float3(extent - i * a, 0, extent) , transformation).toArray
      vertexPool ++= color.toArray
    }

    for(i <- 1 until 2*subdivnumber)
    {
      vertexPool ++= transformf(Float3(-extent, 0, extent - i * a) , transformation).toArray
      vertexPool ++= color.toArray
    }

    indexPool += 0 + vertexCount
    indexPool += 1 + vertexCount
    indexPool += 1 + vertexCount
    indexPool += 2 + vertexCount
    indexPool += 2 + vertexCount
    indexPool += 3 + vertexCount
    indexPool += 3 + vertexCount
    indexPool += 0 + vertexCount

    val off0 = 4
    val off1 = subdivnumber*2 - 1
    for(i <- 0 until off1)
    {
      this.indexPool += (off0 + off1 + i) + vertexCount
      this.indexPool += (off0 + 4*off1 - i - 1) + vertexCount
    }

    for(i <- 0 until off1)
    {
      this.indexPool += (off0 + i) + vertexCount
      this.indexPool += (off0 + 3*off1 - i - 1) + vertexCount
    }

    vertexCount += 4 + 4 * (2 * subdivnumber - 1) //TODO test

  }
}
