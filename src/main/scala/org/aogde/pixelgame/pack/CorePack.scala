package org.aogde.pixelgame.pack

import org.aogde.pixelgame.component.{WindowInfo, WindowInfoConst}
import org.aogde.pixelgame.core.{GameRegistry, IPack}
import org.aogde.pixelgame.render.RenderingEngine
import org.aogde.pixelgame.render.concrete.vertfrag.{RenderRegularConvexPolygonColor, RenderTriangleColor}
import org.aogde.pixelgame.render.definition.{LifetimeOneDraw, TransformationUI}
import org.lwjgl.glfw.GLFW._
import russoul.lib.common.{Float2, Triangle2F}
import russoul.lib.common.math.geometry.complex.RegularConvexPolygon2
import russoul.lib.common.utils.ColorUtils._
import shapeless.Nat

/**
  * Created by russoul on 17.07.2017.
  */
class CorePack extends IPack {

  override def name: String = "Core"

  override def version: String = "0.0.1"


  def onGameUpdate(win: WindowInfoConst, render: RenderingEngine) : Unit = {
    val w = win.width
    val h = win.height

    val renderPoly = new RenderRegularConvexPolygonColor
    val poly = RegularConvexPolygon2(center = Float2(128, h/2), 128F, Nat(5))
    renderPoly.add(poly, 0, Green)



    val renderTriangle = new RenderTriangleColor
    renderTriangle.add(Triangle2F(Float2(0,0), Float2(0,50), Float2(50,50)), 0, Red)


    render.User.push(LifetimeOneDraw, TransformationUI, renderPoly)
    render.User.push(LifetimeOneDraw, TransformationUI, renderTriangle)
  }

  def onKeyCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int) : Unit = {
    println(glfwGetKeyName(key, scancode) + s" was ${if(action == GLFW_PRESS) "pressed" else if(action == GLFW_REPEAT) "repeated" else "released"} !")
  }

  override def init(registry: GameRegistry): Unit = {
    registry.Pack.addGameUpdateCallback(onGameUpdate)
    registry.Pack.addKeyCallback(onKeyCallback)

    println("Core pack initialized")
  }
}
