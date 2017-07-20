package org.aogde.pixelgame.pack

import org.aogde.pixelgame.component.{WindowInfo, WindowInfoConst}
import org.aogde.pixelgame.core.{GameRegistry, IPack}
import org.aogde.pixelgame.render.RenderingEngine
import org.aogde.pixelgame.render.concrete.vertfrag.{RenderGrid3Color, RenderLineColor, RenderRegularConvexPolygonColor, RenderTriangleColor}
import org.aogde.pixelgame.render.definition.{LifetimeManual, LifetimeOneDraw, TransformationNone, TransformationUI}
import org.lwjgl.glfw.GLFW._
import russoul.lib.common.{CircleF, FCircle, FShape2, Float2, Line2F, Triangle2F, _}
import russoul.lib.common.math.geometry.complex.RegularConvexPolygon2
import russoul.lib.common.utils.ColorUtils._
import shapeless.Nat
import Implicits._
import org.aogde.pixelgame.render.shader.Shader
import russoul.lib.common.Abstraction.Con
import russoul.lib.common.TypeClasses.{Field, Tensor1}
import russoul.lib.common.utils.{Arr, ColorUtils, Timer}
import ColorUtils._
import russoul.lib.common.math.algebra.Vec
import russoul.lib.common.math.geometry.simple.Square2Over
import Nat._
import russoul.lib.common.math.CollisionEngineF

import scala.reflect.ClassTag

/**
  * Created by russoul on 17.07.2017.
  */
class CorePack extends IPack {

  override def name: String = "Core"

  override def version: String = "0.0.1"


  var camWorldPos = Float2(0,0)

  val timer = new Timer

  val TIMER_KEY_INPUT = "CorePack_KeyInput"

  {
    timer.update(TIMER_KEY_INPUT)
  }

  def onGameUpdate(win: WindowInfoConst, render: RenderingEngine) : Unit = {
    val w = win.width
    val h = win.height

    val speed = 8F

    /*key match{
      case GLFW_KEY_W => camWorldPos += Float2(0,speed)
      case GLFW_KEY_S => camWorldPos += Float2(0,-speed)
      case GLFW_KEY_A => camWorldPos += Float2(-speed, 0)
      case GLFW_KEY_D => camWorldPos += Float2(speed, 0)
      case _ =>
    }*/

    val dt = timer.getDeltaSec(TIMER_KEY_INPUT).toFloat //TODO what if the game pauses ?

    if(glfwGetKey(win.id, GLFW_KEY_W) == GLFW_PRESS || glfwGetKey(win.id, GLFW_KEY_W) == GLFW_REPEAT){
      camWorldPos += Float2(0,speed * dt)
    }
    if(glfwGetKey(win.id, GLFW_KEY_S) == GLFW_PRESS || glfwGetKey(win.id, GLFW_KEY_S) == GLFW_REPEAT){
      camWorldPos += Float2(0,-speed * dt)
    }
    if(glfwGetKey(win.id, GLFW_KEY_A) == GLFW_PRESS || glfwGetKey(win.id, GLFW_KEY_A) == GLFW_REPEAT){
      camWorldPos += Float2(-speed * dt,0)
    }
    if(glfwGetKey(win.id, GLFW_KEY_D) == GLFW_PRESS || glfwGetKey(win.id, GLFW_KEY_D) == GLFW_REPEAT){
      camWorldPos += Float2(speed * dt,0)
    }

    timer.update(TIMER_KEY_INPUT)
  }

  def onKeyCallback(window: Long, key: Int, scancode: Int, action: Int, mods: Int) : Unit = {


  }



  class VoxelGrid2[@sp(Float, Double) A : ClassTag] (val a: A, val sizeX: Int, val sizeY: Int){

    def verticesX = sizeX + 1
    def verticesY = sizeY + 1

    val grid = new Array[A](verticesX * verticesY)
    val vertices = new Array[Vec2[A]](sizeX * sizeY)


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


  val point = Float2(0,0)
  var grid = new VoxelGrid2[Float](1,16,16)

  def fillInGrid(vg: VoxelGrid2[Float], shape: FShape2[Float]) : Unit = {
   for(y <- 0 until vg.verticesY){
     for(x <- 0 until vg.verticesX){
       vg.grid(y * vg.verticesX + x) = shape.density(point + Float2(vg.a * x, vg.a * y))
     }
   }
  }




  def vertexInterpolation(v1:Float2, v2:Float2, val1:Float, val2:Float):Float2 =
  {

    var min:Float = -1F
    var max:Float = -1F

    var minV = nil[Float2]
    var maxV = nil[Float2]

    if(val1 >= val2)
    {
      max = val1
      min = val2

      maxV = v1
      minV = v2
    }else
    {
      min = val1
      max = val2

      minV = v1
      maxV = v2
    }

    val dist = max - min
    val k = (-min)/(max-min)



    minV + (maxV - minV) * k
  }

  /*def edgeIntersectionPoint(sign0: Float, sign1: Float, x0: Float, x1: Float) : Float = {
    if(sign0 < sign1){
      if(x0 < x1){

      }
    }
  }*/



  def constSign(a: Float, b: Float) = if(a > 0) b >= 0 else if(a < 0) b < 0 else b >= 0

  /*double get_QEF(Point3d point, Voxel3d voxel)
{
    double QEF = 0.0;
    foreach(plane in voxel.planes)
    {
        double dist_to_plane = plane.distance(point);
        QEF += dist_to_plane*dist_to_plane;
    }
    return(QEF);
}*/

  def calcQEF(point: Float2, lines: Arr[Line2F]): Float = {
    var qef = 0F
    for(line <- lines){
      val dist = CollisionEngineF.distancePoint2Line2(point, line)
      qef += dist * dist
    }

    qef
  }



  //2 3
  //0 1

  def sampleQEFBrute(square: Square2F, n: Int, lines: Arr[Line2F]) : Float2 = {
    val ext = Float2(square.extent,square.extent)
    val min = square.center - ext

    var bestQEF = 100000000F//TODO placeholder
    var bestPoint = min

    for(i <- 0 until n){
      for(j <- 0 until n){
        val point = min + ext ⊗ Float2((2 * i + 1) / n.toFloat, (2 * j + 1)/n.toFloat)
        val qef = calcQEF(point, lines)

        if(qef < bestQEF){
          bestQEF = qef
          bestPoint = point
        }
      }
    }

    bestPoint
  }

  def sampleIntersectionBrute(line : Line2F, n: Int, shape: FShape2[Float]) : Float2 = {
    val ext = line.end - line.start

    var best = 100000000F
    var bestAbs = 100000000F //placeholder TODO
    var bestPoint = nil[Float2]

    for(i <- 0 to n){
      val point = line.start + ext * (i.toFloat / n.toFloat)
      val den = shape.density(point)
      val abs = Math.abs(den)

      if(abs < bestAbs){
        bestAbs = abs
        best = den
        bestPoint = point
      }

    }

    //line.start + ext / 2 //TODO

    bestPoint
  }

  def sampleTangent(square: Square2F, n: Int, shape: FShape2[Float]) : Float2 = {
    val ext = Float2(square.extent,square.extent)
    val min = square.center - ext

    val denAtCenter = shape.density(square.center)

    var closest = denAtCenter + 10000000F
    var closestPoint = square.center

    for(i <- 0 to n){
      for(j <- 0 to n){
        val point = min + ext ⊗ Float2((2F * i) / n.toFloat, (2F * j)/n.toFloat)
        val den = shape.density(point)
        val attempt = Math.abs(den - denAtCenter)
        if(attempt < closest && (point - square.center).squaredLength() != 0){
          closest = attempt
          closestPoint = point
        }

      }
    }

    (closestPoint - square.center)
    //val normalRaw = (closestPoint - square.center).⟂.normalize()
    //if(shape.density(square.center + normalRaw * extForNormal) > denAtCenter) normalRaw else -normalRaw
  }

  val extForNormal = 1F / 100F

  def makeVertex(vg: VoxelGrid2[Float], x: Int, y: Int, shape: FShape2[Float], accuracy: Int) : Float2 = {
    val p0 = vg.get(x, y)
    val p1 = vg.get(x + 1, y)
    val p2 = vg.get(x, y + 1)
    val p3 = vg.get(x + 1, y + 1)

    val v0 = vg.getPoint(x, y)
    val v1 = vg.getPoint(x + 1, y)
    val v2 = vg.getPoint(x, y + 1)
    val v3 = vg.getPoint(x + 1, y + 1)

    var sit = 0

    if(!constSign(p0,p1)) sit |= 1
    if(!constSign(p1,p3)) sit |= 2
    if(!constSign(p3,p2)) sit |= 4
    if(!constSign(p2,p0)) sit |= 8


    if(sit > 0){
      val tangents = new Arr[Line2F]()

      var vert1 = nil[Float2]
      var vert2 = nil[Float2]

      if( (sit & 1) > 0){
        val ip = sampleIntersectionBrute(Line2F(v0,v1), accuracy, shape)
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, shape)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line
      }
      if((sit & 2) > 0){

        val ip = sampleIntersectionBrute(Line2F(v1,v3), accuracy, shape)
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, shape)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line

      }
      if((sit & 4) > 0){

        val ip = sampleIntersectionBrute(Line2F(v3,v2), accuracy, shape)
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, shape)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line

      }
      if((sit & 8) > 0){
        val ip = sampleIntersectionBrute(Line2F(v2,v0), accuracy, shape)
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, shape)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line
      }

      //tangents.foreach(t => {linesRenderer.add(t, 0.5F, Magenta);})

      //val interpolatedVertex = vertices.toArray.foldRight(Float2(0,0)){_ + _} / vertices.size
      val interpolatedVertex = sampleQEFBrute(vg.square2(x,y), accuracy, tangents)



      vg.vertices(y * vg.sizeX + x) = interpolatedVertex

      return interpolatedVertex
    }

    null
  }


  def makeContour(vg: VoxelGrid2[Float], shape: FShape2[Float], accuracy: Int) : Arr[Line2F] = {
    val res = new Arr[Line2F]()

    for(y <- 0 until vg.sizeY) {
      for (x <- 0 until vg.sizeX) {
        val p0 = vg.get(x, y)
        val p1 = vg.get(x + 1, y)
        val p2 = vg.get(x, y + 1)
        val p3 = vg.get(x + 1, y + 1)

        val v0 = vg.getPoint(x, y)
        val v1 = vg.getPoint(x + 1, y)
        val v2 = vg.getPoint(x, y + 1)
        val v3 = vg.getPoint(x + 1, y + 1)

        var sit = 0

        if(!constSign(p0,p1)) sit |= 1
        if(!constSign(p1,p3)) sit |= 2
        if(!constSign(p3,p2)) sit |= 4
        if(!constSign(p2,p0)) sit |= 8


        if(sit > 0){

          val interpolatedVertex = makeVertex(vg, x,y, shape, accuracy)

          var vert1 = nil[Float2]
          var vert2 = nil[Float2]

          if( (sit & 1) > 0){
          }
          if((sit & 2) > 0){

            if(x + 1 < vg.sizeX) {
              vert1 = makeVertex(vg, x + 1, y, shape, accuracy)
            }
          }
          if((sit & 4) > 0){

            if(y + 1 < vg.sizeY) {
              vert2 = makeVertex(vg, x, y + 1, shape, accuracy)
            }

          }
          if((sit & 8) > 0){
          }


          //val interpolatedVertex = vertices.toArray.foldRight(Float2(0,0)){_ + _} / vertices.size


          circleRenderer.add(RegularConvexPolygon2(interpolatedVertex, 0.1F,Nat._16), 0F, Yellow)

          vg.vertices(y * vg.sizeX + x) = interpolatedVertex

          if(vert1 != null) res += Line2F(interpolatedVertex, vert1)
          if(vert2 != null) res += Line2F(interpolatedVertex, vert2)
        }
      }
    }

    res

  }


  val linesRenderer = new RenderLineColor
  val circleRenderer = new RenderRegularConvexPolygonColor

  val gridRenderer = new RenderGrid3Color

  {
    gridRenderer.add(8, 8, White, Mat4F.rotationDeg(Float3(1,0,0), 90) ⨯ Mat4F.translation(Float3(8,8,0)))
  }

  override def init(registry: GameRegistry): Unit = {
    registry.Pack.addGameUpdateCallback(onGameUpdate)
    registry.Pack.addKeyCallback(onKeyCallback)


    val circle1 = FCircle(Float2(4,8), 2F)
    val circle2 = FCircle(Float2(8,8), 5F)
    val circle3 = FCircle(Float2(8,8), 3F)//TODO 1 does not work, too small
    //val circle4 = FCircle(Float2(5,9),1F)

    val rec = FRectangle2(Float2(6,8), Float2(2F,2F))

    val shape = (circle1 | circle2) - circle3

    fillInGrid(grid, shape)

    val rad = 0.1F

    grid.foreachVertex( (v,s) => {
      //val t = (64+s)/64
      val t = 0
      circleRenderer.add(RegularConvexPolygon2(v, rad, Nat._16), 0, if(s > 0) White else if(s < 0) White ⊗ Float3(t,t,t) else Green)
    })

    val lines = makeContour(grid, shape, 64)

    println(s"generated ${lines.size} lines")

    lines.foreach(line => linesRenderer.add(line, 0, ColorUtils.Red))

    val trans = Some(new registry.Pack.renderer.User.ShaderDataProvider {
      override def provide(shader: Shader, windowInfo: WindowInfoConst): Shader = {
        shader.setMat4("V", Mat4F.translation(-Float3(camWorldPos, 0)), transpose = true)
        //shader.setMat4("P", Mat4F.ortho(16, 16, 16, 16, -16, 16))
        shader.setMat4("P", Mat4F.ortho(0, 16, 0, 16, -1, 1))
        //println("")

        shader
      }
    })

    registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, linesRenderer, trans)
    registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, gridRenderer, trans)
    registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, circleRenderer, trans)

    println("Core pack initialized")
  }
}
