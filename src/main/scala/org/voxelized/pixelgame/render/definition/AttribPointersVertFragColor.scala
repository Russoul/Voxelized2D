package org.voxelized.pixelgame.render.definition

import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL20.{glEnableVertexAttribArray, glVertexAttribPointer}

/**
  * Created by russoul on 20.07.2017.
  */
trait AttribPointersVertFragColor {

  protected val vertexSize: Int = 6

  def setAttributePointers(): Unit = {
    glVertexAttribPointer(0, 3, GL_FLOAT, false, vertexSize * 4, 0)
    glEnableVertexAttribArray(0)

    glVertexAttribPointer(1, 3, GL_FLOAT, false, vertexSize * 4, 3 * 4)
    glEnableVertexAttribArray(1)
  }

}
