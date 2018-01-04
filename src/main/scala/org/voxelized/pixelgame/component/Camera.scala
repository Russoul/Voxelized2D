package org.voxelized.pixelgame.component

import russoul.lib.common._
import russoul.lib.common.Implicits._
import russoul.lib.common.math.geometry.simple.Rectangle2Over

import spire.algebra._
import spire.math._
import spire.implicits._


/**
  * Created by russoul on 12.03.17.
  */

//all arguments are measured in pixels, so numbers should be integral !
//camera "seats" in the center of the drawing surface
//camera defines a rectangle area visible to the player
//defined in world space
class Camera(var view: Rectangle2F)
{
  //TODO do we need this ?, Rectangle2F fully defines scalable 2d rectangle
  private var zoom = 1F //should be in the interval: [1; MAX_ZOOM]

  def getZoom() = zoom
  def setZoom(zoom:Float):Boolean = if(zoom >= 1F && zoom <= Camera.MAX_ZOOM) {this.zoom = zoom;true} else false

  /**
    *
    * @param width of rendering area
    * @param height of rendering area
    * @return
    */
  def genTranslationMatrix(width:Float, height:Float): Mat4[Float] =
  {
    Mat4F.translation(-view.center(0) +width/2, -view.center(1) + height/2,0)
  }

}

object Camera
{
  final val MAX_ZOOM = 5F
}
