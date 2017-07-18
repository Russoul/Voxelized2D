package org.aogde.pixelgame.render

import java.awt.Window
import java.io.File

import org.aogde.pixelgame.component.WindowInfo
import org.aogde.pixelgame.config.Defaults
import org.aogde.pixelgame.core.PixelGame
import org.aogde.pixelgame.render.definition._
import org.aogde.pixelgame.render.shader.{Shader, ShaderUtils}
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import russoul.lib.common.utils.{Arr, FileUtils, Utilities}
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import russoul.lib.common.Implicits._
import russoul.lib.common._

import scala.collection.{TraversableLike, mutable}

/**
  * Created by russoul on 14.07.2017.
  */
class RenderingEngine(private val game: PixelGame) {

  //not used currently
  //class RenderInfo[Life <: RenderLifetime, Trans <: RenderTransformation](val lifetime: Life, val transformation : Trans, renderer: RendererVertFrag)

  object System{ //TODO make it so only game core game code can access this object, not the user code

    class ShaderNotFoundException(path: String) extends Exception("Shader not found in path: " + path)
    class ShaderDuplicateNameException(name: String) extends Exception(name)
    class NoSuchShaderException(name: String) extends Exception(name)

    private final val EXTENSION_VERTEX = "vert"
    private final val EXTENSION_FRAGMENT = "frag"

    //default in case we add support for custom shaders, loaded from game extensions
    val defaultShaders = new mutable.HashMap[String, Shader]



    object Init{ //TODO check for proper initialization before any rendering
      /**
        *
        * @param directory
        * @param names names of shaders to load, without file extension (names, newNames)
        */
      def loadDefaultShaders(directory: String, names: Traversable[(String,String)]): Unit ={

        for(name <- names){
          val filePathVert = directory + name._1 + "." + EXTENSION_VERTEX
          val filePathFrag = directory + name._1 + "." + EXTENSION_FRAGMENT
          val fileVert = new File(filePathVert)
          val fileFrag = new File(filePathFrag)
          if(!fileVert.exists()) throw new ShaderNotFoundException(filePathVert)
          if(!fileFrag.exists()) throw new ShaderNotFoundException(filePathFrag)

          val sourceVert = Utilities.loadAsString(filePathVert) //TODO new File(..) is called two times
          val sourceFrag = Utilities.loadAsString(filePathFrag)


          val shader = new Shader(sourceVert, sourceFrag)

          if(defaultShaders.contains(name._2)) throw new ShaderDuplicateNameException(name._2)

          defaultShaders += name._2 -> shader
        }

      }
    }

    /*
      object RenderMode extends Enumeration {
        type Name = Value
        val Wireframe, Full = Value
      }
    */

    //for now writing renderers and interfaces supposing that we have only two shader procedures available : vert and frag
    object Render{


      var curRenderID = Long.MinValue //TODO that is not enough, refresh it somehow





      //these renderers need to be updated each frame, fully cleared after each framebuffer swap
      //those match LifetimeOneDraw lifetime

      //those things always must contain checked and working info,
      //so no extra checking is required while performing actual drawing
      //TODO implement sorting renderers by shader as switching program(shader) is an expensive operation
      val lifetimeOneDrawRenderers = new mutable.HashMap[RenderID, (RendererVertFrag,User.ShaderDataProvider)]

      def draw(windowInfo: WindowInfo): Unit ={
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT) // clear the framebuffer

        glClearColor(Defaults.initialBackgroundColor._0, Defaults.initialBackgroundColor._1, Defaults.initialBackgroundColor._2, 1)

        drawUI(windowInfo)


        glfwSwapBuffers(windowInfo.getID()) // swap the color buffers
      }

      private def drawUI(windowInfo: WindowInfo): Unit ={
        for(renderInfo <- lifetimeOneDrawRenderers.values){
          val render = renderInfo._1
          val shaderName = render.getShaderName()
          val shader = defaultShaders(shaderName)
          shader.enable()

          renderInfo._2.provide(shader)

          render.construct()
          render.draw()
          render.deconstruct()
          shader.disable()
        }

        lifetimeOneDrawRenderers.clear()
      }
    }


  }

  object User{

    //used to set shader uniforms by user and returns the shader back
    trait ShaderDataProvider{
      def provide(shader: Shader) : Shader
    }

    class UnsupportedRenderLifetimeException(lifetime: RenderLifetime) extends Exception("Unsupported render lifetime : " + lifetime.name())
    class UnsupportedRenderTransformationException(tr : RenderTransformation) extends Exception("Unsupported render transformation : " + tr.name())



    //multithread-unsafe
    /**
      *
      * @param lifetime how fast the data will be deleted
      * @param transformation default transformation for shader
      * @param renderer
      * @param shaderDataProvider extra data for shader to be set by System renderer before drawing
      * @tparam Life
      * @tparam Trans
      * @return
      */
    def push[Life <: RenderLifetime, Trans <: RenderTransformation](lifetime: Life, transformation: Trans, renderer: RendererVertFrag, shaderDataProvider: Option[ShaderDataProvider] = None): RenderID ={

      if(!System.defaultShaders.contains(renderer.getShaderName())) throw new System.NoSuchShaderException(renderer.getShaderName())

      lifetime match{
        case LifetimeOneDraw =>

          //TODO deal with textures + extra values to be passed to the rendering pipeline
          val providedByUser : Shader => Shader = if(shaderDataProvider.nonEmpty) shaderDataProvider.get.provide else (x : Shader) => x
          var combinedProvider : ShaderDataProvider = null

          transformation match{
            case TransformationUI =>
              val windowInfo = game.getConstWindowInfo()

              val transformationUI = (shader: Shader) => {
                shader.setMat4("P",Mat4F.ortho(0, windowInfo.width, windowInfo.height, 0 , -1, 1) , transpose = false) //TODO cache
                shader.setMat4("V", Mat4F.identity(), transpose = true) //TODO all uniforms are persistent (all values are remembered after unbound), no need to setup each time if no change has been made

                shader
              }

              combinedProvider = (shader: Shader) => {transformationUI(shader);providedByUser(shader)} //combining providers

            //case TransformationWorld =>
            case TransformationNone =>
              //no extra default transformation
              combinedProvider = (shader : Shader) => providedByUser(shader)
            case unsupported => throw new UnsupportedRenderTransformationException(unsupported)
          }

          val id = System.Render.curRenderID
          val ret = new RenderID(id)
          System.Render.curRenderID += 1
          System.Render.lifetimeOneDrawRenderers += (ret -> (renderer -> combinedProvider))
          ret

        case unsupported => throw new UnsupportedRenderLifetimeException(unsupported)
      }
    }

    /*object EachFrame{ //when
      object UI{ //where
        object Wireframe{ //how
          object RegularConvexPolygon{ //what

          }
        }
      }
    }*/
  }

}
