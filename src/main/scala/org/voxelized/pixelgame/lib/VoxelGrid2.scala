package org.voxelized.pixelgame.lib

import russoul.lib.common.Abstraction.Con
import russoul.lib.common.TypeClasses.{Field, Tensor1}
import russoul.lib.common.math.algebra.Vec
import russoul.lib.common.math.geometry.simple.Square2Over
import russoul.lib.common.{Vec2, sp}
import shapeless.Nat._2

import russoul.lib.common.Implicits._

import scala.reflect.ClassTag

class VoxelGrid2[@sp(Float, Double) A : ClassTag] (val a: A, val sizeX: Int, val sizeY: Int){

  def verticesX = sizeX + 1
  def verticesY = sizeY + 1

  val grid = new Array[A](verticesX * verticesY)


  def foreachVertex(f: (Vec2[A], A) => Unit)(implicit field : Field[A], con: Con[A]): Unit ={
    for(y <- 0 until verticesY) {
      for (x <- 0 until verticesX) {
        f(getPoint(x,y), get(x,y))
      }
    }
  }


  def get(x: Int, y: Int) : A = {
    grid(y * verticesX + x)
  }

  def set(x: Int, y: Int, value: A) : Unit = {
    grid(y * verticesX + x) = value
  }

  /**
    *
    * @return local to voxel grid coordinates of the point
    */
  def getPoint(x: Int, y: Int)(implicit field : Field[A], con: Con[A]) : Vec2[A] = {
    Vec2[A](a * x.as[A], a * y.as[A])
  }

  def square2(x: Int, y: Int)(implicit con : Con[A], field: Field[A], t1: Tensor1[A,Vec,_2]) : Square2Over[Vec,A] = {
    Square2Over[Vec,A](t1.make((x.toDouble.as[A] + 0.5D.as[A])*a, (y.toDouble.as[A] + 0.5D.as[A])*a), a/2D.as[A])
  }

}
