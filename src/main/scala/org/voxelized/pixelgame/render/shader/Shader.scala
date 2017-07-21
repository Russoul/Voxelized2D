package org.voxelized.pixelgame.render.shader

import java.util
import java.util.HashMap

import org.lwjgl.BufferUtils
import russoul.lib.common._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL11._
import russoul.lib.common.Implicits._

/**
  * Created by russoul on 28.01.17.
  */
class Shader(vertexSource: String, fragmentSource: String) {
  final val ID: Int = ShaderUtils.createProgram(vertexSource, fragmentSource)
  protected var locationCache: util.HashMap[String, Integer] = new HashMap[String, Integer]
  protected var enabled: Boolean = false


  def getUniform(name: String): Int =
  {
    if (locationCache.containsKey(name)) {
      return locationCache.get(name)
    }
    val result: Int = glGetUniformLocation(ID, name)
    if (result == -1) System.err.println("Could not find uniform variable'" + name + "'!")
    else {
      locationCache.put(name, Integer.valueOf(result))

    }
    result

  }

  def setBool(name: String, value: Boolean): Unit =
  {
    if (!enabled) enable()
    glUniform1i(getUniform(name), if (value) 1 else 0)
  }

  def setInt(name: String, value: Int)
  {
    if (!enabled) enable
    glUniform1i(getUniform(name), value)
  }

  def setFloat(name: String, value: Float)
  {
    if (!enabled) enable
    glUniform1f(getUniform(name), value)
  }


  def setVec2(name: String, x: Float, y: Float)
  {
    if (!enabled) enable
    glUniform2f(getUniform(name), x, y)
  }

  //Those functions convert double input to float
  def setVec3(name: String, vector: Float3)
  {
    if (!enabled) enable
    glUniform3f(getUniform(name), vector.x, vector.y, vector.z)
  }

  def setVec4(name: String, vector: Float4)
  {
    if (!enabled) enable
    glUniform4f(getUniform(name), vector.x, vector.y, vector.z, vector.w)
  }

  /**
    *
    * @param name
    * @param matrix
    * @param transpose is the input matrix going to be transposed internally by OpenGL
    */
  def setMat4(name: String, matrix: Mat4F, transpose: Boolean = false)
  {

    if (!enabled) enable
    glUniformMatrix4fv(getUniform(name), transpose, matrix.toArray) //in GLSL matrix-vector multiplication is column vector based !!!, transpose is needed !
  }
  //......................................................


  def isInUse(): Boolean =
  {
    var id = new Array[Int](1)
    glGetIntegerv(GL_CURRENT_PROGRAM,id)

    id(0) == ID
  }

  /**
    * will check if it is already enabled
    */
  def enable()
  {
    if(!isInUse())glUseProgram(ID)

  }

  /**
    * will check if it is already disabled
    */
  def disable()
  {
    if(isInUse())glUseProgram(0)
  }
}
