package org.voxelized.pixelgame.render.definition

import russoul.lib.common.utils.Arr

import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._

/**
  * Created by russoul on 15.07.2017.
  */
trait RendererVertFragDefault extends RendererVertFrag{

  protected val vertexSize:Int //not in bytes ! but in number of floating point values per vertex
  protected val vertexPool = new Arr[Float]
  protected val indexPool = new Arr[Int]
  protected var vertexCount:Int = 0
  protected var VBO,VAO,EBO:Int = 0
  protected var constructed = false

  override def construct(): Boolean = {

    if(constructed) return false

    VAO = glGenVertexArrays()
    VBO = glGenBuffers()
    EBO = glGenBuffers()

    glBindVertexArray(VAO)

    glBindBuffer(GL_ARRAY_BUFFER, VBO)
    glBufferData(GL_ARRAY_BUFFER, vertexPool.array, GL_STATIC_DRAW)


    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, EBO)
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexPool.array, GL_STATIC_DRAW)

    //should be provided by child
    setAttributePointers()

    glBindBuffer(GL_ARRAY_BUFFER, 0); // Note that this is allowed, the call to glVertexAttribPointer registered VBO as the currently bound vertex buffer object so afterwards we can safely unbind

    glBindVertexArray(0); // Unbind VAO (it's always a good thing to unbind any buffer/array to prevent strange bugs), remember: do NOT unbind the EBO, keep it bound to this VAO

    constructed = true

    true
  }

  override def deconstruct(): Boolean = {

    if(!constructed) return false

    glDeleteVertexArrays(VAO)
    glDeleteBuffers(VBO)
    glDeleteBuffers(EBO)

    constructed = false

    true
  }

  def clearPools(): Unit ={
    vertexPool.clear()
    indexPool.clear()
    vertexCount = 0 //always forgetting this :)
  }


  override def draw(): Boolean = {

    if(!constructed) return false

    glBindVertexArray(VAO)
    glDrawElements(renderMode(), indexPool.size, GL_UNSIGNED_INT, 0)
    glBindVertexArray(0)
    true
  }
}
