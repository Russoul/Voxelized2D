package org.voxelized.pixelgame.render.definition

import org.lwjgl.opengl.GL11.GL_FLOAT
import org.lwjgl.opengl.GL20.{glEnableVertexAttribArray, glVertexAttribPointer}

trait AttribPointersVertFragTextureColor {
  protected val vertexSize: Int = 8

  def setAttributePointers(): Unit = {
    glVertexAttribPointer(0, 3, GL_FLOAT, false, vertexSize * 4, 0)
    glEnableVertexAttribArray(0)

    glVertexAttribPointer(1, 2, GL_FLOAT, false, vertexSize * 4, 3 * 4)
    glEnableVertexAttribArray(1)

    glVertexAttribPointer(2, 3, GL_FLOAT, false, vertexSize * 4, 5 * 4)
    glEnableVertexAttribArray(2)
  }

}
