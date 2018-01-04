package org.voxelized.pixelgame.lib

import russoul.lib.common.math.geometry.simple.Square2Over
import russoul.lib.common._
import russoul.lib.common.Implicits._
import spire.algebra._
import spire.math._
import spire.implicits._
import spire.sp

import scala.reflect.ClassTag

class VoxelGrid2 (val a: Float, val sizeX: Int, val sizeY: Int){

  def verticesX = sizeX + 1
  def verticesY = sizeY + 1

  val grid = new Array[Float](verticesX * verticesY)


  def foreachVertex(f: (Vec2[Float], Float) => Unit): Unit ={
    for(y <- 0 until verticesY) {
      for (x <- 0 until verticesX) {
        f(getPoint(x,y), get(x,y))
      }
    }
  }


  def get(x: Int, y: Int) : Float = {
    grid(y * verticesX + x)
  }

  def set(x: Int, y: Int, value: Float) : Unit = {
    grid(y * verticesX + x) = value
  }

  /**
    *
    * @return local to voxel grid coordinates of the point
    */
  def getPoint(x: Int, y: Int) : Vec2[Float] = {
    Vec2[Float](a * x, a * y)
  }

  def square2(x: Int, y: Int) : Square2Over[Float] = {
    Square2Over[Float](Vec2[Float]((x + 0.5F)*a, (y + 0.5F)*a), a/2F)
  }

}
