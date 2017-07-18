package org.aogde.pixelgame.render.shader

import org.lwjgl.opengl.GL20._
/**
  * Created by russoul on 11.03.17.
  */
object ShaderUtils
{

  class ShaderMakeError(log: String) extends Exception(log)

  /**
    *
    * @param vert source
    * @param frag source
    */
  def createProgram(vert:String, frag:String): Int =
  {
    // creates a program object and assigns it to the
    // variable program.
    val program = glCreateProgram
    // glCreateShader specificies the type of shader
    // that we want created. For the vertex shader
    // we define it as GL_VERTEX_SHADER
    val vertID = glCreateShader(GL_VERTEX_SHADER)

    // Specificies that we want to create a
    // GL_FRAGMENT_SHADER
    val fragID = glCreateShader(GL_FRAGMENT_SHADER)
    // glShaderSource replaces the source code in a shader
    // object.
    // We've defined our vertex shader object and now
    // we want to pass in our vertex shader that we
    // managed to build as a string in our load
    // function.
    //
    glShaderSource(vertID, vert)
    // does the same for our fragment shader
    glShaderSource(fragID, frag)
    // This group of code tries to compile our shader object
    // it then gets the status of that compiled shader and
    // if it proves to be false then it prints an error to
    // the command line.
    glCompileShader(vertID)
    val compileStatus = new Array[Int](1)
    glGetShaderiv(vertID, GL_COMPILE_STATUS, compileStatus)
    if (compileStatus(0) == 0) {
      System.err.println("Failed to compile vertex shader!")
      println("source:")
      println("----------------------------------------------------------")
      println(vert)
      println("----------------------------------------------------------")
      throw new ShaderMakeError(glGetShaderInfoLog(vertID))
      //throw new Exception("Failed to compile vertex shader")
    }
    // This group of code tries to compile our shader object
    // it then gets the status of that compiled shader and
    // if it proves to be false then it prints an error to
    // the command line.
    glCompileShader(fragID)
    glGetShaderiv(fragID, GL_COMPILE_STATUS, compileStatus)
    if (compileStatus(0) == 0) {
      System.err.println("Failed to compile fragment shader!")
      println("source:")
      println("----------------------------------------------------------")
      println(frag)
      println("----------------------------------------------------------")
      throw new ShaderMakeError(glGetShaderInfoLog(fragID))
      //throw new Exception("Failed to compile vertex shader")
    }
    // This attaches our vertex and fragment shaders
    // to the program object that we defined at the
    // start of this tutorial.
    glAttachShader(program, vertID)
    glAttachShader(program, fragID)
    // this links our program object
    glLinkProgram(program)
    //
    glValidateProgram(program)
    // this then returns our created program
    // object.
    program
  }
}
