package org.voxelized.pixelgame.render

import java.awt.Window
import java.io.File

import org.voxelized.pixelgame.component.{WindowInfo, WindowInfoConst}
import org.voxelized.pixelgame.config.Defaults
import org.voxelized.pixelgame.core.Voxelized2D
import org.voxelized.pixelgame.render.definition._
import org.voxelized.pixelgame.render.shader.{Shader, ShaderUtils}
import org.lwjgl.glfw.GLFW.glfwSwapBuffers
import russoul.lib.common.utils.{Arr, FileUtils, Utilities}
import org.lwjgl.opengl.GL15._
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL20._
import org.lwjgl.opengl.GL30._
import russoul.lib.common.Implicits._
import russoul.lib.common._
import RenderingEngine._
import org.voxelized.pixelgame.render.RenderingEngine.RenderDataProvider.{ApplyPostRenderState, ApplyPreRenderState, ProvideShaderData}

import scala.collection.{TraversableLike, mutable}

/**
  * Created by russoul on 14.07.2017.
  */
class RenderingEngine(private val game: Voxelized2D) {

  //not used currently
  //class RenderInfo[Life <: RenderLifetime, Trans <: RenderTransformation](val lifetime: Life, val transformation : Trans, renderer: RendererVertFrag)

  object System{ //TODO make it so only core game code can access this object, not the user code

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
      val lifetimeOneDrawRenderers = new mutable.HashMap[RenderID, (RendererVertFrag,RenderDataProvider)]
      val lifetimeManualRenderers = new mutable.HashMap[RenderID, (RendererVertFrag,RenderDataProvider)]

      def draw(windowInfo: WindowInfo): Unit ={
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT) // clear the framebuffer

        glClearColor(Defaults.initialBackgroundColor._0, Defaults.initialBackgroundColor._1, Defaults.initialBackgroundColor._2, 1)

        drawUI(windowInfo)


        glfwSwapBuffers(windowInfo.getID()) // swap the color buffers
      }

      private def drawUI(windowInfo: WindowInfo): Unit ={
        val ic = windowInfo.const()

        for(renderInfo <- lifetimeOneDrawRenderers.values){
          val render = renderInfo._1
          val shaderName = render.getShaderName()
          val shader = defaultShaders(shaderName)
          shader.enable()

          if(renderInfo._2.applyShaderData.nonEmpty) renderInfo._2.applyShaderData.get.apply(shader,ic)
          if(renderInfo._2.applyPreRenderState.nonEmpty) renderInfo._2.applyPreRenderState.get.apply()

          render.construct()
          render.draw()
          render.deconstruct()

          if(renderInfo._2.applyPostRenderState.nonEmpty) renderInfo._2.applyPostRenderState.get.apply()

          shader.disable()
        }
        lifetimeOneDrawRenderers.clear()

        for(renderInfo <- lifetimeManualRenderers.values){
          val render = renderInfo._1
          val shaderName = render.getShaderName()
          val shader = defaultShaders(shaderName)
          shader.enable()

          if(renderInfo._2.applyShaderData.nonEmpty) renderInfo._2.applyShaderData.get.apply(shader,ic)
          if(renderInfo._2.applyPreRenderState.nonEmpty) renderInfo._2.applyPreRenderState.get.apply()

          render.construct()
          render.draw()
          render.deconstruct()
          shader.disable()

          if(renderInfo._2.applyPostRenderState.nonEmpty) renderInfo._2.applyPostRenderState.get.apply()
        }
      }
    }


  }

  object User{

    //used to set shader uniforms by user and returns the shader back


    class UnsupportedRenderLifetimeException(lifetime: RenderLifetime) extends Exception("Unsupported render lifetime : " + lifetime.name())
    class UnsupportedRenderTransformationException(tr : RenderTransformation) extends Exception("Unsupported render transformation : " + tr.name())



    //multithread-unsafe
    /**
      *
      * @param lifetime how fast the data will be deleted
      * @param transformation default transformation for shader
      * @param renderer
      * @param dataProvider extra data for shader and state instructions for OpenGL
      * @tparam Life
      * @tparam Trans
      * @return
      */
    def push[Life <: RenderLifetime, Trans <: RenderTransformation](lifetime: Life, transformation: Trans, renderer: RendererVertFrag, dataProvider: Option[RenderDataProvider] = None): RenderID ={

      if(!System.defaultShaders.contains(renderer.getShaderName())) throw new System.NoSuchShaderException(renderer.getShaderName())

      lifetime match{
        case LifetimeOneDraw | LifetimeManual =>

          var applyPreRenderState  : Option[ApplyPreRenderState] = None
          var applyPostRenderState : Option[ApplyPostRenderState] = None

          //TODO deal with textures + extra values to be passed to the rendering pipeline
          val providedByUser : ProvideShaderData = dataProvider match{
            case Some(provider) =>

              applyPreRenderState = provider.applyPreRenderState
              applyPostRenderState = provider.applyPostRenderState

              provider.applyShaderData match{
                case Some(applicable) => applicable
                case None => RenderDataProvider.defaultShaderDataProvider.get
              }
            case None => RenderDataProvider.defaultShaderDataProvider.get
          }
          var combinedProvider : ProvideShaderData = null

          transformation match{
            case TransformationUI =>

              val transformationUI = (shader: Shader, windowInfo: WindowInfoConst) => {
                shader.setMat4("P",Mat4F.ortho(0, windowInfo.width, windowInfo.height, 0 , -1, 1) , transpose = false) //TODO cache
                shader.setMat4("V", Mat4F.identity(), transpose = true) //TODO all uniforms are persistent (all values are remembered after unbound), no need to setup each time if no change has been made

                shader
              }

              combinedProvider = (shader: Shader, info: WindowInfoConst) => {transformationUI(shader, info);providedByUser(shader, info)} //combining providers

            //case TransformationWorld =>
            case TransformationNone =>
              //no extra default transformation
              combinedProvider = (shader : Shader, info: WindowInfoConst) => providedByUser(shader, info)
            case unsupported => throw new UnsupportedRenderTransformationException(unsupported)
          }

          val id = System.Render.curRenderID
          val ret = new RenderID(id)
          System.Render.curRenderID += 1

          val newProvider = new RenderDataProvider(Some(combinedProvider), applyPreRenderState, applyPostRenderState)


          lifetime match{
            case LifetimeOneDraw => System.Render.lifetimeOneDrawRenderers += (ret -> (renderer -> newProvider))
            case LifetimeManual => System.Render.lifetimeManualRenderers += (ret -> (renderer -> newProvider))
          }


          ret

        case unsupported => throw new UnsupportedRenderLifetimeException(unsupported)
      }
    }

  }

}

object RenderingEngine{
  import RenderDataProvider._
  class RenderDataProvider(val applyShaderData: Option[ProvideShaderData] = defaultShaderDataProvider, val applyPreRenderState : Option[ApplyPreRenderState] = None, val applyPostRenderState: Option[ApplyPostRenderState] = None){
  }
  object RenderDataProvider{
    type ProvideShaderData = (Shader, WindowInfoConst) => Shader //this will be called on the shader with name specified by renderer
    type ApplyPreRenderState = () => Unit //this will be called before any rendering occurs, used to set state to OpenGL (ex: glEnable(GL_CULL_FACE))
    type ApplyPostRenderState = () => Unit //this will be called after all the rendering occurs, used to set state to OpenGL (ex: glDisable(GL_CULL_FACE))


    final val defaultShaderDataProvider : Option[ProvideShaderData] = Some( (shader, _) => shader ) //apply nothing to the shader, give the reference to it back
  }
}
