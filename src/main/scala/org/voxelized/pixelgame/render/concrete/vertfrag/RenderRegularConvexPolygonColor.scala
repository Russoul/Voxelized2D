package org.voxelized.pixelgame.render.concrete.vertfrag

import org.voxelized.pixelgame.render.definition.{AttribPointersVertFrag, RendererVertFrag, RendererVertFragDefault}
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL11._
import russoul.lib.common._
import russoul.lib.common.Implicits._
import russoul.lib.common.math.geometry.complex.RegularConvexPolygon2
import shapeless.Nat
import shapeless.ops.nat.ToInt

/**
  * Created by russoul on 15.07.2017.
  */
class RenderRegularConvexPolygonColor extends RendererVertFragDefault with AttribPointersVertFrag{
  override val renderMode: Int = GL_TRIANGLES
  override val vertexSize: Int = 6 //vec3 as pos with depth + vec3 as color


  def add[N <: Nat : ToInt](shape: RegularConvexPolygon2[Float,N], zLevel: Float, color: Float3) : Unit = {
    val angle = 2*Math.PI/shape.getN()

    vertexPool += shape.center.x
    vertexPool += shape.center.y
    vertexPool += zLevel

    vertexPool += color.x
    vertexPool += color.y
    vertexPool += color.z

    for(i <- 0 until shape.getN()){
      val x = Math.cos(angle*i).toFloat
      val y = Math.sin(angle*i).toFloat

      val l = Float2(x,y)

      val vertex = l*shape.rad + shape.center

      vertexPool += vertex.x
      vertexPool += vertex.y
      vertexPool += zLevel

      vertexPool += color.x
      vertexPool += color.y
      vertexPool += color.z
    }


    for(i <- 1 until shape.getN()){
      indexPool += i + 1 + vertexCount
      indexPool += vertexCount
      indexPool += i + vertexCount
    }

    indexPool += 1 + vertexCount
    indexPool += vertexCount
    indexPool += shape.getN() + vertexCount

    vertexCount += shape.getN() + 1
  }


  override def getShaderName(): String = "color"
}
