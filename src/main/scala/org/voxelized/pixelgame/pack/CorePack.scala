package org.voxelized.pixelgame.pack

import java.nio.ByteBuffer

import org.voxelized.pixelgame.component.{WindowInfo, WindowInfoConst}
import org.voxelized.pixelgame.core.{GameRegistry, IPack}
import org.voxelized.pixelgame.render.RenderingEngine
import org.voxelized.pixelgame.render.concrete.vertfrag._
import org.voxelized.pixelgame.render.definition.{LifetimeManual, LifetimeOneDraw, TransformationNone, TransformationUI}
import org.lwjgl.glfw.GLFW._
import russoul.lib.common.{CircleF, FCircle, FShape2, Float2, Line2F, Triangle2F, _}
import russoul.lib.common.math.geometry.complex.RegularConvexPolygon2
import russoul.lib.common.utils.ColorUtils._
import shapeless.Nat
import Implicits._
import org.voxelized.pixelgame.render.shader.Shader
import russoul.lib.common.utils.{Arr, ColorUtils, Timer}
import ColorUtils._
import russoul.lib.common.math.geometry.simple.Square2Over
import Nat._
import org.lwjgl.opengl.{GL11, GL13, GL30}
import org.lwjgl.stb.{STBImage, STBImageWrite}
import org.lwjgl.system.{MemoryStack, MemoryUtil}
import org.voxelized.pixelgame.lib.VoxelGrid2
import org.voxelized.pixelgame.render.RenderingEngine.RenderDataProvider
import russoul.lib.common.math.CollisionEngineF
import russoul.lib.common._
import spire.syntax.cfor._
import MemoryStack._
import russoul.lib.macros._


import spire.algebra._
import spire.math._
import spire.implicits._

import scala.reflect.ClassTag
import scala.util.Failure

/**
  * Created by russoul on 17.07.2017.
  */
class CorePack extends IPack {

  override def name: String = "Core"

  override def version: String = "0.0.1"


  var camWorldPos = Float2(0,0)

  val timer = new Timer

  val TIMER_KEY_INPUT = "CorePack_KeyInput"
  

  final val BLOCK_SIZE: Float = 0.125F
  final val CHUNK_SIZE: Int = 128

  {
    timer.update(TIMER_KEY_INPUT)
  }



  def onGameUpdate(win: WindowInfoConst, render: RenderingEngine) : Unit = {
    val w = win.width
    val h = win.height

    val speed = 8F

    if(glfwGetKey(win.id, GLFW_KEY_TAB) == GLFW_PRESS ||glfwGetKey(win.id, GLFW_KEY_TAB) == GLFW_REPEAT){
      render.User.push(LifetimeOneDraw, TransformationNone, tTriangleRenderer, renderData)
    }

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



  //def constSign(a: Float, b: Float) = if(a > 0) b >= 0 else if(a < 0) b < 0 else b >= 0
  def constSign(a: Float, b: Float): Boolean = if(a > 0) b > 0 else b <= 0 //TODO ??

  //definition of the QEF, used to find feature vertices (helps to preserve features of the surface)
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
        val point = min + Float2(ext(0) * (2 * i + 1) / n.toFloat, ext(1) * (2 * j + 1) / n.toFloat)
        val qef = calcQEF(point, lines)

        if(qef < bestQEF){
          bestQEF = qef
          bestPoint = point
        }
      }
    }

    bestPoint
  }

  def sampleIntersectionBrute(line : Line2F, n: Int, f : FShape2) : Float2 = {
    val ext = line.end - line.start

    var bestAbs = 100000000F //placeholder TODO
    var bestPoint = nil[Float2]

    for(i <- 0 to n){
      val point = line.start + (i.toFloat / n.toFloat) *: ext
      val den = f.density(point)
      val abs = Math.abs(den)

      if(abs < bestAbs){
        bestAbs = abs
        bestPoint = point
      }

    }

    bestPoint
  }

  def sampleTangent(square: Square2F, n: Int, f : FShape2) : Float2 = {
    val ext = Float2(square.extent,square.extent)
    val min = square.center - ext

    val denAtCenter = f.density(square.center)

    var closest = denAtCenter + 10000000F //TODO placeholder
    var closestPoint = square.center

    for(i <- 0 to n){
      for(j <- 0 to n){
        val point = min + Float2(ext(0) * (2 * i) / n.toFloat, ext(1) * (2 * j) / n.toFloat)
        val den = f.density(point)
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

  def extForNormal(blockSize : Float): Float = blockSize / 100F //TODO is this value correct, why this ?


  def makeLines(vg: VoxelGrid2, features : Array[Float2]) : Arr[Line2F] = {
    val ret = new Arr[Line2F]()

    for(y <- 0 until vg.sizeY - 1) {
      for (x <- 0 until vg.sizeX - 1) {
        val feature = features(y * vg.sizeX + x)
        if(feature != null){
          val p1 = vg.get(x + 1, y)
          val p2 = vg.get(x, y + 1)
          val p3 = vg.get(x + 1, y + 1) //we use get here instead of f(..) because get is cached and the same sample points are used more then once throughout the algorithm


          var vert1, vert2 : Float2 = null

          if(!constSign(p1,p3))
            vert1 = features(y * vg.sizeX + (x+1))

          if(!constSign(p3,p2))
            vert2 = features((y+1) * vg.sizeX + x)

          if(vert1 != null) ret += Line2F(feature, vert1)
          if(vert2 != null) ret += Line2F(feature, vert2)
        }
      }
    }

    ret
  }

  def makeTriangles(vg : VoxelGrid2, features : Array[Float2], intersections : Array[Arr[Float2]], extra : Array[Arr[Float2]]) : Arr[Triangle2F] = {

    val ret = new Arr[Triangle2F]()

    for(y <- 0 until vg.sizeY) {
      for (x <- 0 until vg.sizeX) {

        val t = y * vg.sizeX + x
        val curIntersections = intersections(t)
        val curExtras = extra(t)


        // for each block of the grid
        val p0 = vg.get(x, y)
        val p1 = vg.get(x + 1, y)
        val p2 = vg.get(x, y + 1)
        val p3 = vg.get(x + 1, y + 1) //we use get here instead of f(..) because get is cached and the same sample points are used more then once throughout the algorithm

        val v0 = vg.getPoint(x, y)
        val v1 = vg.getPoint(x + 1, y)
        val v2 = vg.getPoint(x, y + 1)
        val v3 = vg.getPoint(x + 1, y + 1)

        var sit = 0

        if (!constSign(p0, p1)) sit |= 1 //sample its corner points using (samples are already stored in the grid)
        if (!constSign(p1, p3)) sit |= 2
        if (!constSign(p3, p2)) sit |= 4
        if (!constSign(p2, p0)) sit |= 8

        if(sit == 0){ //fully inside or fully outside
          val negative = p0 < 0

          if(negative){ //render if it is inside
            val tr1 = Triangle2F(v0,v1,v3)
            val tr2 = Triangle2F(v0,v3,v2)

            ret += tr1
            ret += tr2
          }
        }else{ //contains surface
          if(curIntersections != null && features(t) != null){
            for(i <- 0 until curIntersections.size){
              ret += Triangle2F(features(t), curIntersections(i), curExtras(i))//generating triangles
            }
          }
        }

      }
    }



    ret
  }

  def makeVertex(vg: VoxelGrid2, tr: Arr[Triangle2F], x: Int, y: Int, f : FShape2, accuracy: Int, features : Array[Float2], outIntersections : Arr[Float2], outExtra : Arr[Float2]) : Float2 = {
    val epsilon = vg.a/accuracy

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

    val extForNormal = this.extForNormal(grid.a) //this defines the box within which the samples will be taken to find tangent to the surface at intersection point
    //if the surface equation is given explicitly tangent can be found differentiating the equation

    if(sit > 0){ //if this failes we dont calculate main features because the block is completely inside or outside
      val tangents = new Arr[Line2F]()


      var vert1 = nil[Float2]
      var vert2 = nil[Float2]


      @inline def worker(and : Int, v_a : Float2, v_b : Float2, p_a : Float, p_b : Float): Unit ={
        if( (sit & and) > 0){

          val ip = sampleIntersectionBrute(Line2F(v_a,v_b), accuracy, f) //find point of intersection of the surface with this edge with given precision (sampling algorith is used)
          val full = if(p_a <= 0) v_a else v_b //choose the end point of the edge to draw a triangle with
          circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,16), 0F, Blue) //debug render
          val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, f) //find tangent line to the surface at found point of intersection
          val line = Line2F(ip - dir * (1 / extForNormal) , ip + dir * (1 / extForNormal) )//^
          tangents += line //put it to the list of all found tangents

          outIntersections += ip //add intersection point to the list of all intersection points
          outExtra += full //for triangle drawing
        }else{
          val negative = p_a < 0
          if(negative){ //if the edge is inside the surface then render a triangle using end points and resulting feature vertex
            outIntersections += v_a
            outExtra += v_b
          }
        }
      }

      //for each edge of intersection with the surface (surface intersects the edge <=> edge's end points have different signs) :
      worker(1, v0, v1, p0, p1)
      worker(2, v1, v3, p1, p3)
      worker(4, v3, v2, p3, p2)
      worker(8, v2, v0, p2, p0)

      //the main and the most interesting part :
      //calculate the feature vertex using quadratic error function minimization algorithm
      //(we basically take the point, where quadratic error function takes the least value, as a feature point)
      //QEF has the form : "Float2 -> Float" and is defined above
      //we use the most naive algorithm to find the minimum of the QEF : sampling

      //for clarity : we could actually use linear interpolation to find the feature vertex and it would work, but the features are not preserved in this case
      val interpolatedVertex = sampleQEFBrute(vg.square2(x,y), accuracy, tangents) //feature vertex

      for(i <- 0 until outIntersections.size){
        tr += Triangle2F(interpolatedVertex, outIntersections(i), outExtra(i))//generating triangles
      }

      features(y * vg.sizeX + x) = interpolatedVertex


      return interpolatedVertex
    }

    null
  }



  case class ContourData(lines : Arr[Line2F], triangles : Arr[Triangle2F], features : Array[Float2], intersections: Array[Arr[Float2]], extras : Array[Arr[Float2]])

  /**
    *
    * @param vg
    * @param f
    * @param accuracy
    * @return outline and triangle mesh for rendering + generated features
    */
  def makeContour(vg: VoxelGrid2, f : FShape2, accuracy: Int) : ContourData = {
    val res1 = new Arr[Line2F]()
    val res2 = new Arr[Triangle2F]()

    val features = new Array[Float2](vg.sizeX * vg.sizeY)//generated feature vertices
    val intersections, extra  = new Array[Arr[Float2]](vg.sizeX * vg.sizeY)

    @inline def cachedMake(x : Int, y : Int): Float2 ={
      val t = y * vg.sizeX + x
      val possible = features(t)
      if(possible == null) //features vertices are cached in an array for future access
      {
        intersections(t) = new Arr[Float2](4) //TODO extra mem usage here, REWORK
        extra(t) = new Arr[Float2](4)
        val ret = makeVertex(vg, res2, x,y, f, accuracy, features, intersections(t), extra(t)) //find feature vertex at current block
        if(ret == null){ //empty blocks have null feature,intersections and extras
          intersections(t) == null
          extra(t) = null
        }

        ret
      }
      else
        possible
    }

    for(y <- 0 until vg.sizeY) {
      for (x <- 0 until vg.sizeX) {// for each block of the grid
        val p0 = vg.get(x, y)
        val p1 = vg.get(x + 1, y)
        val p2 = vg.get(x, y + 1)
        val p3 = vg.get(x + 1, y + 1) //we use get here instead of f(..) because get is cached and the same sample points are used more then once throughout the algorithm

        val v0 = vg.getPoint(x, y)
        val v1 = vg.getPoint(x + 1, y)
        val v2 = vg.getPoint(x, y + 1)
        val v3 = vg.getPoint(x + 1, y + 1)

        var sit = 0

        if(!constSign(p0,p1)) sit |= 1 //sample its corner points using (samples are already stored in the grid)
        if(!constSign(p1,p3)) sit |= 2
        if(!constSign(p3,p2)) sit |= 4
        if(!constSign(p2,p0)) sit |= 8


        if(sit > 0){ //surface goes through this block

          val interpolatedVertex = cachedMake(x, y)

          var vert1 = nil[Float2]
          var vert2 = nil[Float2]

          //if( (sit & 1) > 0){}
          if((sit & 2) > 0){
            if(x + 1 < vg.sizeX) {//check if the current block is not the last one in the row
              vert1 = cachedMake(x + 1, y) //find main feature vertex at right block, used only for lines
            }
          }
          if((sit & 4) > 0){
            if(y + 1 < vg.sizeY) {//check if the current block is not the last one in the column
              vert2 = cachedMake(x, y + 1) //find main feature vertex at top block, used only for lines
            }

          }
          //if((sit & 8) > 0){}



          circleRenderer.add(RegularConvexPolygon2(interpolatedVertex, 0.1F, 16), 0F, Yellow)//debug render of feature vertex


          if(vert1 != null) res1 += Line2F(interpolatedVertex, vert1) //generated lines
          if(vert2 != null) res1 += Line2F(interpolatedVertex, vert2)
        }else{ //this block is completely inside or outside of the surface

          val negative = p0 < 0

          if(negative){ //render if it is inside
            val tr1 = Triangle2F(v0,v1,v3)
            val tr2 = Triangle2F(v0,v3,v2)

            res2 += tr1
            res2 += tr2
          }
        }
      }
    }

    ContourData(res1, res2, features, intersections, extra)

  }


  val point = Float2(0,0)
  var grid = new VoxelGrid2(BLOCK_SIZE,CHUNK_SIZE,CHUNK_SIZE)


  def gridRectangle2(min : Float2, grid : VoxelGrid2): Rectangle2F = {
    val extent = Float2(grid.a * grid.sizeX/2, grid.a * grid.sizeY/2)
    Rectangle2F(min + extent, extent)
  }

  /**
    *
    * @param circle local to the grid ! (grid coordinate system start from its bound min coordinate (bottom left corner))
    * @return (x,y) pairs of block coordinates (local) in the grid, DOES NOT CHECK GRID BOUNDS !
    */
  def gridIntersections(a: Float, circle : CircleF) : Arr[Int2] = {
    val circleMinX = circle.center.x - circle.rad
    val circleMaxX = circle.center.x + circle.rad

    val circleMinY = circle.center.y - circle.rad
    val circleMaxY = circle.center.y + circle.rad

    val minx = (circleMinX / a).toInt
    val miny = (circleMinY / a).toInt
    val maxx = (circleMaxX / a + 1).toInt
    val maxy = (circleMaxY / a + 1).toInt

    val ret = new Arr[Int2]( (maxx-minx + 1) * (maxy - miny + 1) )

    for(x <- minx to maxx){
      for(y <- miny to maxy){
        ret += Int2(x,y)
      }
    }

    ret
  }

  def fillInGrid(vg: VoxelGrid2, f : FShape2) : Unit = {
    for(y <- 0 until vg.verticesY){
      for(x <- 0 until vg.verticesX){
        vg.grid(y * vg.verticesX + x) = f.density(point + Float2(vg.a * x, vg.a * y))
      }
    }
  }

  val linesRenderer = new RenderLineColor
  val circleRenderer = new RenderRegularConvexPolygonColor
  val tTriangleRenderer = new RenderTriangleTextureColor
  //val tTriangleRenderer = new RenderTriangleTextureColor

  val gridRenderer = new RenderGrid3Color

  {
    gridRenderer.add(8, CHUNK_SIZE/2, White, Mat4F.rotationDeg(Float3(1,0,0), 90) ⨯ Mat4F.translation(Float3(8,8,0)))
  }

  var registry: GameRegistry    = _
  var renderer: RenderingEngine = _


  var contourData : ContourData = _





  def nonEmptyBlock(vg : VoxelGrid2, x : Int, y : Int) : Boolean = {
    val p0 = vg.get(x, y)
    val p1 = vg.get(x + 1, y)
    val p2 = vg.get(x, y + 1)
    val p3 = vg.get(x + 1, y + 1) //we use get here instead of f(..) because get is cached and the same sample points are used more then once throughout the algorithm

    p0 <= 0 || p1 <= 0 || p2 <= 0 || p3 <= 0
  }

  def emptyBlock(vg : VoxelGrid2, x : Int, y : Int) = !nonEmptyBlock(vg,x,y)

  def fullBlock(vg : VoxelGrid2, x : Int, y : Int) : Boolean = {
    val p0 = vg.get(x, y)
    val p1 = vg.get(x + 1, y)
    val p2 = vg.get(x, y + 1)
    val p3 = vg.get(x + 1, y + 1) //we use get here instead of f(..) because get is cached and the same sample points are used more then once throughout the algorithm

    p0 <= 0 && p1 <= 0 && p2 <= 0 && p3 <= 0
  }

  @inline def asFloat(a : Int2) : Float2 = Float2(a.x, a.y)
  @inline def truncate(a : Float2) : Int2 = Int2(a.x.toInt, a.y.toInt)

  /**
    * triangle mapped on texture rect resulting in texture coords
    * @param tr must be contained within `rect`
    * @param rect
    * @param texMin minimum (x,y) pos on texture (starting from top left !)
    * @param texMax maximum (x,y) pos on texture
    * @return
    */
  def mapped(tr : Triangle2F, rect : Rectangle2F, texMin : Int2, texMax : Int2, texWidth : Int, texHeight : Int) : Array[Float2] = {

    val minF = Float2(texMin.x.toFloat / texWidth, texMin.y.toFloat / texHeight)
    val maxF = Float2(texMax.x.toFloat / texWidth, texMax.y.toFloat / texHeight)

    val origin = rect.center - rect.extent

    val width = rect.extent.x * 2
    val height = rect.extent.y * 2

    val invDim = Float2(1F/width, 1F/height)

    val dp1 = tr.p1 - origin
    val dp2 = tr.p2 - origin
    val dp3 = tr.p3 - origin

    val ddp1 = dp1 ⊗ invDim
    val ddp2 = dp2 ⊗ invDim
    val ddp3 = dp3 ⊗ invDim

    val delta = maxF - minF

    val p1 = delta ⊗ ddp1 + minF //TODO implement repeat pattern
    val p2 = delta ⊗ ddp2 + minF
    val p3 = delta ⊗ ddp3 + minF

    array!(p1,p2,p3)
  }


  @inline def rect(origin : Float2, grid: VoxelGrid2) : Rectangle2F = {
    val extent = Float2(grid.sizeX, grid.sizeY) * (grid.a / 2F)

    Rectangle2F(origin + extent, extent)
  }

  def mouseCallback(window : Long, button : Int, action : Int, mods : Int) : Unit = { //TODO exceptions are suppressed ??
    button match {
      case GLFW_MOUSE_BUTTON_2 =>
        val win = registry.Pack.windowInfoConst()

        auto(MemoryStack.stackPush()) { stack =>
          val x = stack.mallocDouble(1)
          val iy = stack.mallocDouble(1)

          glfwGetCursorPos(win.id, x, iy)

          val rx = x.get()
          val ry = win.height - iy.get()

          val aspect = win.width / win.height

          val height = 16
          val width = height * aspect

          val a = BLOCK_SIZE

          val px = rx / win.width * width / a toInt
          val py = ry / win.height * height / a toInt

          if(px < grid.sizeX && py < grid.sizeY && px >= 0 && py >= 0){
            println(s"isEmpty = ${emptyBlock(grid, px, py)}")
          }
        }
      case GLFW_MOUSE_BUTTON_1 =>
        if(action == GLFW_PRESS){

          val win = registry.Pack.windowInfoConst()

          auto(MemoryStack.stackPush()){ stack =>
            val x = stack.mallocDouble(1)
            val iy = stack.mallocDouble(1)

            glfwGetCursorPos(win.id, x, iy)

            val rx = x.get()
            val ry = win.height - iy.get()

            val aspect = win.width / win.height

            val height = 16
            val width = height * aspect


            val px = rx / win.width * width   toFloat
            val py = ry / win.height * height toFloat

            val center = Float2(px,py) + camWorldPos

            println(s"gonna spawn a circle at ${center}")

            val rad = 1.5F
            val circle = FCircle(center, rad)
            val circleGeo = CircleF(center, rad)
            val gridRec = gridRectangle2(point, grid)



            //circle inside chunk bounds
            val inside = CollisionEngineF.checkCircleInsideRectangle2(circleGeo, gridRec)
            if(!inside){
              println("circle not inside bounds !")
            }else{
              val newGrid = new VoxelGrid2(BLOCK_SIZE, CHUNK_SIZE, CHUNK_SIZE)
              fillInGrid(newGrid, circle.density) //TODO we actually don't need this
              val circleData = makeContour(newGrid, circle.density , 32)

              //process only those blocks that are common to both volume(density <= 0) we gonna add and the old volume
              val blocks = gridIntersections(BLOCK_SIZE, circleGeo)

              println(s"changed ${blocks.size} blocks")

              var correct = true //TODO remove

              /*for(coord <- blocks){
                val x = coord.x
                val y = coord.y
                val t = y * CHUNK_SIZE + x


                if(nonEmptyBlock(grid, x, y) && !fullBlock(grid,x,y)){
                  if (nonEmptyBlock(newGrid, x, y) && !fullBlock(newGrid,x,y)){ //both are partial
                    correct = false
                  }
                }
              }*/


              if(!correct){
                println("incorrect placement")
              }else{


                for(coord <- blocks){
                  val x = coord.x
                  val y = coord.y


                  val t = y * CHUNK_SIZE + x

                  /*val feature = contourData.features(t) //may be null
                  if(feature != null && CollisionEngineF.checkPoint2Circle(feature, circleGeo)){
                    contourData.features(t) = null
                  }

                  val dataF = circleData.features(t)
                  if(dataF != null){
                    contourData.features(t) = dataF
                  }*/

                  val dataF = circleData.features(t)
                  if(emptyBlock(grid, x, y)){
                    contourData.features(t) = dataF
                    contourData.intersections(t) = circleData.intersections(t)
                    contourData.extras(t) = circleData.extras(t)
                  }else if(!fullBlock(grid, x, y)){ //partial
                    if(fullBlock(newGrid, x, y)){
                      contourData.features(t) = dataF
                      contourData.intersections(t) = circleData.intersections(t)
                      contourData.extras(t) = circleData.extras(t)
                    }else if (nonEmptyBlock(newGrid, x, y)){ //both are partial

                      contourData.features(t) = (contourData.features(t) + circleData.features(t)) * 0.5F
                      contourData.intersections(t) ++= circleData.intersections(t)
                      contourData.extras(t) ++= circleData.extras(t)

                    }
                  }

                }


                //change the grid at last (we use it before)
                for(coord <- blocks){
                  val x = coord.x
                  val y = coord.y

                  val g0 = newGrid.get(x,y)
                  val g1 = newGrid.get(x + 1,y)
                  val g2 = newGrid.get(x,y + 1)
                  val g3 = newGrid.get(x + 1,y + 1)

                  grid.set(x,y, Math.min(g0, grid.get(x,y))) //operation OR
                  grid.set(x + 1,y, Math.min(g1, grid.get(x + 1,y)))
                  grid.set(x,y + 1, Math.min(g2, grid.get(x,y + 1)))
                  grid.set(x + 1,y + 1, Math.min(g3, grid.get(x + 1,y + 1)))
                }



                val lines = makeLines(grid, contourData.features)
                println(s"generated ${lines.size} lines")
                val triangles = makeTriangles(grid, contourData.features, contourData.intersections, contourData.extras)
                println(s"generated ${triangles.size} triangles") //TODO generated triangles only on changed blocks

                contourData = contourData.copy(lines = lines, triangles = triangles)

                linesRenderer.clearPools()
                linesRenderer.deconstruct()
                lines.foreach(line => linesRenderer.add(line, 0, Red))
                linesRenderer.construct()

                tTriangleRenderer.clearPools()
                tTriangleRenderer.deconstruct()
                triangles.foreach(tr => tTriangleRenderer.add(tr, 0, mapped(tr, rect(point, grid), texture_tiles_grass_min, texture_tiles_grass_max, 256, 256), White))
                tTriangleRenderer.construct()
              }

            }
          }
        }
    }
  }


  def shaderData(shader: Shader, windowInfo: WindowInfoConst): Shader = {
    val aspect = windowInfo.width / windowInfo.height

    val height = 16
    val width = height * aspect

    shader.setMat4("V", Mat4F.translation(-Float3(camWorldPos, 0)), transpose = true)
    //shader.setMat4("P", Mat4F.ortho(16, 16, 16, 16, -16, 16))
    shader.setMat4("P", Mat4F.ortho(0, width, 0, height, -1, 1))
    //println("")

    shader
  }

  def preRenderState() : Unit = {
    GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
    GL11.glEnable(GL11.GL_TEXTURE_2D)
    GL11.glDisable(GL11.GL_CULL_FACE)

    GL13.glActiveTexture(GL13.GL_TEXTURE0)
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture_tiles)

  }
  def postRenderState() : Unit = {
    GL11.glPopAttrib()
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0)
  }

  val renderData = Some(new RenderDataProvider(Some(shaderData), Some(preRenderState), Some(postRenderState)))

  def run(registry: GameRegistry): Unit ={

    //Thread.sleep(30000)

    this.registry = registry
    this.renderer = registry.Pack.renderer

    registry.Pack.addGameUpdateCallback(onGameUpdate)
    registry.Pack.addKeyCallback(onKeyCallback)


    val offset = Float2(0.1F, 0.1F)
    val circle1 = FCircle(Float2(4,8) + offset, 2F)
    val circle2 = FCircle(Float2(8,8) + offset, 5F)
    val circle3 = FCircle(Float2(4,4) + offset, 2F)
    val circle4 = FCircle(Float2(8,12) + offset,4F)
    val circle5 = FCircle(Float2(8,6) + offset,1.1F)

    val debugCircle = FCircle(Float2(2.8000002F, 11.203231F), 1.5F)

    val rec = FRectangle2(Float2(8,10.8F) + offset, Float2(1F,3F))

    val shape = ((((circle1 | circle2) | rec ) - circle3 - circle4 - circle5) | rec)


    //val generator : Float2 => Float = shape.density


    for(i <- 0 until 10){
      fillInGrid(grid, shape)
      makeContour(grid, shape, 32) //accuracy < 32 introduces visual artifacts
      grid = new VoxelGrid2(BLOCK_SIZE,CHUNK_SIZE,CHUNK_SIZE)
    }



    val data = Timer.timed(dt => s"dual contouring (with grid filling) took $dt ms"){
      fillInGrid(grid, shape)
      makeContour(grid, shape, 32) //accuracy < 32 introduces visual artifacts
    }


    println(s"generated ${data.lines.size} lines and ${data.triangles.size} triangles")


    val rad = 0.02F

    grid.foreachVertex( (v,s) => {
      val t = 0
      circleRenderer.add(RegularConvexPolygon2(v, rad, 16), 0, if(s > 0) White else if(s < 0) White ⊗ Float3(t,t,t) else Green)
    })

    data.lines.foreach(line => linesRenderer.add(line, 0, Red))
    data.triangles.foreach(tr => tTriangleRenderer.add(tr, 0, mapped(tr, rect(point, grid), texture_tiles_grass_min, texture_tiles_grass_max, 256, 256), White))

    contourData = data

    //TODO debug
    linesRenderer.add(Line2F(Float2(0,0), Float2(1,0)), 0, Red)
    val t = 64F/256F
    tTriangleRenderer.add(Triangle2F(Float2(0,0), Float2(1,0), Float2(1,1)), 0, array!(Float2(0,0), Float2(t,0), Float2(t,t)), White)
    tTriangleRenderer.add(Triangle2F(Float2(0,0), Float2(1,1), Float2(0,1)), 0, array!(Float2(0,0), Float2(t,t), Float2(0,t)), White)

    tTriangleRenderer.construct()
    linesRenderer.construct()
    gridRenderer.construct()
    circleRenderer.construct()



    registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, linesRenderer, renderData)
    //registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, gridRenderer, renderData)
    //registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, circleRenderer, renderData)


    registry.Pack.addMouseCallback(mouseCallback)

    println("Core pack initialized")
  }


  /**
    *
    * @param img
    * @param width
    * @param height
    * @param comp
    * @return new image, don't forget to clean up
    */
  def flip(img: ByteBuffer, width: Int, height : Int, comp : Int) : ByteBuffer = {

    val res = MemoryUtil.memAlloc(comp * width * height)

    for(y <- 0 until height){
      for(x <- 0 until width){
        val r = img.get(y * width * comp + x * comp)
        val g = img.get(y * width * comp + x * comp + 1)
        val b = img.get(y * width * comp + x * comp + 2)
        val a = img.get(y * width * comp + x * comp + 3)

        res.put((height - y - 1) * width * comp + x * comp,     r)
        res.put((height - y - 1) * width * comp + x * comp + 1, g)
        res.put((height - y - 1) * width * comp + x * comp + 2, b)
        res.put((height - y - 1) * width * comp + x * comp + 3, a)
      }
    }

    res
  }

  var texture_tiles : Int = 0 //TODO don't forget to delete textures !
  var texture_tiles_grass_min = Int2(0,0)
  var texture_tiles_grass_max = Int2(64,64)
  def load(registry: GameRegistry): Unit = {
    auto(stackPush){ stack =>

      val x,y, channels = stack.mallocInt(1)

      STBImage.stbi_set_flip_vertically_on_load(true)
      val img = STBImage.stbi_load("src/main/resources/assets/textures/material/tiles.png", x, y, channels, STBImage.STBI_rgb_alpha)
      //val b = STBImageWrite.stbi_write_png("src/main/resources/assets/textures/material/tiles1.png", x.get, y.get, 4, img, 4 * x.get)

      val width = x.get()
      val height = y.get() //dont forget that the pointer is set to the next int

      //println("test "+ b)
      println(s"loaded tiles.png: x = $width, y = $height, channels = ${channels.get()}, img = $img, direct = ${if(img != null) img.isDirect else false}")

      //val flipped = flip(img, width, height, 4)

      import org.lwjgl.opengl.GL11._


      val textureID = glGenTextures(); //Generate texture ID
      glBindTexture(GL_TEXTURE_2D, textureID); //Bind texture ID

      //Setup wrap mode
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT)
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT)

      //Setup texture scaling filtering
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
      glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

      //Send texel data to OpenGL
      glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, img)
      GL30.glGenerateMipmap(GL_TEXTURE_2D)

      STBImage.stbi_image_free(img)
      //MemoryUtil.memFree(flipped)

      texture_tiles = textureID
    }



    println("resources loaded")
  }

  override def init(registry: GameRegistry): Unit = {
    load(registry)
    run(registry)
  }

  override def deinit(registry: GameRegistry): Unit = {
    linesRenderer.deconstruct()
    gridRenderer.deconstruct()
    circleRenderer.deconstruct()

    GL11.glDeleteTextures(texture_tiles)

    println("Core pack deinitialized")
  }
}


