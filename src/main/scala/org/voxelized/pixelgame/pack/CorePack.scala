package org.voxelized.pixelgame.pack

import org.voxelized.pixelgame.component.{WindowInfo, WindowInfoConst}
import org.voxelized.pixelgame.core.{GameRegistry, IPack}
import org.voxelized.pixelgame.render.RenderingEngine
import org.voxelized.pixelgame.render.concrete.vertfrag.{RenderGrid3Color, RenderLineColor, RenderRegularConvexPolygonColor, RenderTriangleColor}
import org.voxelized.pixelgame.render.definition.{LifetimeManual, LifetimeOneDraw, TransformationNone, TransformationUI}
import org.lwjgl.glfw.GLFW._
import russoul.lib.common.{CircleF, FCircle, FShape2, Float2, Line2F, Triangle2F, _}
import russoul.lib.common.math.geometry.complex.RegularConvexPolygon2
import russoul.lib.common.utils.ColorUtils._
import shapeless.Nat
import Implicits._
import org.voxelized.pixelgame.render.shader.Shader
import russoul.lib.common.Abstraction.Con
import russoul.lib.common.TypeClasses.{Field, Tensor1}
import russoul.lib.common.utils.{Arr, ColorUtils, Timer}
import ColorUtils._
import russoul.lib.common.math.algebra.Vec
import russoul.lib.common.math.geometry.simple.Square2Over
import Nat._
import org.lwjgl.opengl.GL11
import org.voxelized.pixelgame.lib.VoxelGrid2
import org.voxelized.pixelgame.render.RenderingEngine.RenderDataProvider
import russoul.lib.common.math.CollisionEngineF
import russoul.lib.common._
import spire.syntax.cfor._

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




    if(glfwGetKey(win.id, GLFW_KEY_TAB) == GLFW_PRESS ||glfwGetKey(win.id, GLFW_KEY_TAB) == GLFW_REPEAT){
      render.User.push(LifetimeOneDraw, TransformationNone, triangleRenderer, renderData)
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



  def constSign(a: Float, b: Float) = if(a > 0) b >= 0 else if(a < 0) b < 0 else b >= 0


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

  def sampleIntersectionBrute(line : Line2F, n: Int, f : Float2 => Float) : Float2 = {
    val ext = line.end - line.start

    var best = 100000000F
    var bestAbs = 100000000F //placeholder TODO
    var bestPoint = nil[Float2]

    for(i <- 0 to n){
      val point = line.start + ext * (i.toFloat / n.toFloat)
      val den = f(point)
      val abs = Math.abs(den)

      if(abs < bestAbs){
        bestAbs = abs
        best = den
        bestPoint = point
      }

    }

    bestPoint
  }

  def sampleTangent(square: Square2F, n: Int, f : Float2 => Float) : Float2 = {
    val ext = Float2(square.extent,square.extent)
    val min = square.center - ext

    val denAtCenter = f(square.center)

    var closest = denAtCenter + 10000000F //TODO placeholder
    var closestPoint = square.center

    for(i <- 0 to n){
      for(j <- 0 to n){
        val point = min + ext ⊗ Float2((2F * i) / n.toFloat, (2F * j)/n.toFloat)
        val den = f(point)
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

  def extForNormal(blockSize : Float) = blockSize / 100F //TODO is this value correct, why this ?

  def makeVertex(vg: VoxelGrid2[Float], tr: Arr[Triangle2F], x: Int, y: Int, f : Float2 => Float, accuracy: Int, features : Array[Float2]) : Float2 = {
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

      val intersections = new Arr[Float2]()
      val vertices = new Arr[Float2]()

      var vert1 = nil[Float2]
      var vert2 = nil[Float2]

      //for each edge of intersection with the surface (surface intersects the edge <=> edge's end points have different signs) :
      if( (sit & 1) > 0){

        val ip = sampleIntersectionBrute(Line2F(v0,v1), accuracy, f) //find point of intersection of the surface with this edge with given precision (sampling algorith is used)
        val full = if(p0 <= 0 && (v0 - ip).squaredLength() > 0) v0 else v1 //choose the end point of the edge to draw a triangle with
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue) //debug render
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, f) //find tangent line to the surface at found point of intersection
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)//^
        tangents += line //put it to the list of all found tangents

        intersections += ip //add intersection point to the list of all intersection points
        vertices += full //for triangle drawing
      }else{
        val negative = p0 < 0
        if(negative){ //if the edge is inside the surface then render a triangle using end points and resulting feature vertex
          intersections += v0
          vertices += v1
        }
      }
      if((sit & 2) > 0){ //same for the other edges



        val ip = sampleIntersectionBrute(Line2F(v1,v3), accuracy, f)
        val full = if(p1 <= 0 && (v1 - ip).squaredLength() > 0) v1 else v3
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, f)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line

        intersections += ip
        vertices += full
      }else{
        val negative = p1 < 0
        if(negative){
          intersections += v1
          vertices += v3
        }
      }
      if((sit & 4) > 0){


        val ip = sampleIntersectionBrute(Line2F(v3,v2), accuracy, f)
        val full = if(p3 <= 0 && (v3 - ip).squaredLength() > 0) v3 else v2
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, f)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line

        intersections += ip
        vertices += full

      }else{
        val negative = p3 < 0
        if(negative){
          intersections += v3
          vertices += v2
        }
      }
      if((sit & 8) > 0){
        val ip = sampleIntersectionBrute(Line2F(v2,v0), accuracy, f)
        val full = if(p2 <= 0 && (v2 - ip).squaredLength() > 0) v2 else v0
        circleRenderer.add(RegularConvexPolygon2(ip, 0.1F,Nat._16), 0F, Blue)
        val dir = sampleTangent(Square2F(ip, extForNormal), accuracy, f)
        val line = Line2F(ip - dir / extForNormal, ip + dir / extForNormal)
        tangents += line

        intersections += ip
        vertices += full
      }else{
        val negative = p2 < 0
        if(negative){
          intersections += v2
          vertices += v0
        }
      }

      //the main and the most interesting part :
      //calculate the feature vertex using quadratic error function minimization algorithm
      //(we basically take the point, where quadratic error function takes the least value, as a feature point)
      //QEF has the form : "Float2 -> Float" and is defined above
      //we use the most naive algorithm to find the minimum of the QEF : sampling

      //for clarity : we could actually use linear interpolation to find the feature vertex and it would work, but the features are not preserved in this case
      val interpolatedVertex = sampleQEFBrute(vg.square2(x,y), accuracy, tangents) //feature vertex

      for(i <- 0 until intersections.size){
        tr += Triangle2F(interpolatedVertex, intersections(i), vertices(i))//generating triangles
      }

      features(y * vg.sizeX + x) = interpolatedVertex

      return interpolatedVertex
    }

    null
  }


  /**
    *
    * @param vg
    * @param f
    * @param accuracy
    * @return outline and triangle mesh for rendering
    */
  def makeContour(vg: VoxelGrid2[Float], f : Float2 => Float, accuracy: Int) : (Arr[Line2F], Arr[Triangle2F]) = {
    val res1 = new Arr[Line2F]()
    val res2 = new Arr[Triangle2F]()

    val features = new Array[Float2](vg.sizeX * vg.sizeY)//generated feature vertices


    @inline def cashedMake(x : Int, y : Int): Float2 ={
      val possible = features(y * vg.sizeX + x)
      if(possible == null) //features vertices are cashed in an array for future access
        makeVertex(vg, res2, x,y, f, accuracy, features) //find feature vertex at current block
      else
        possible
    }

    for(y <- 0 until vg.sizeY) {
      for (x <- 0 until vg.sizeX) {// for each block of the grid
        val p0 = vg.get(x, y)
        val p1 = vg.get(x + 1, y)
        val p2 = vg.get(x, y + 1)
        val p3 = vg.get(x + 1, y + 1)

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

          val interpolatedVertex = cashedMake(x, y)

          var vert1 = nil[Float2]
          var vert2 = nil[Float2]

          //if( (sit & 1) > 0){}
          if((sit & 2) > 0){
            if(x + 1 < vg.sizeX) {//check if the current block is not the last one in the row
              vert1 = cashedMake(x + 1, y) //find main feature vertex at right block, used only for lines
            }
          }
          if((sit & 4) > 0){
            if(y + 1 < vg.sizeY) {//check if the current block is not the last one in the column
              vert2 = cashedMake(x, y + 1) //find main feature vertex at top block, used only for lines
            }

          }
          //if((sit & 8) > 0){}



          circleRenderer.add(RegularConvexPolygon2(interpolatedVertex, 0.1F,Nat._16), 0F, Yellow)//debug render of feature vertex


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

    (res1,res2)

  }


  val point = Float2(0,0)
  var grid = new VoxelGrid2[Float](0.25F,64,64)

  def fillInGrid(vg: VoxelGrid2[Float], f : Float2 => Float) : Unit = {
    for(y <- 0 until vg.verticesY){
      for(x <- 0 until vg.verticesX){
        vg.grid(y * vg.verticesX + x) = f(point + Float2(vg.a * x, vg.a * y))
      }
    }
  }

  val linesRenderer = new RenderLineColor
  val circleRenderer = new RenderRegularConvexPolygonColor
  val triangleRenderer = new RenderTriangleColor

  val gridRenderer = new RenderGrid3Color

  {
    gridRenderer.add(8, 64, White, Mat4F.rotationDeg(Float3(1,0,0), 90) ⨯ Mat4F.translation(Float3(8,8,0)))
  }

  var registry: GameRegistry    = _
  var renderer: RenderingEngine = _


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
    GL11.glDisable(GL11.GL_CULL_FACE)
    GL11.glPopAttrib()
  }
  def postRenderState() : Unit = {
  }

  val renderData = Some(new RenderDataProvider(Some(shaderData), Some(preRenderState), Some(postRenderState)))


  def work(registry: GameRegistry): Unit ={
    this.registry = registry
    this.renderer = registry.Pack.renderer

    registry.Pack.addGameUpdateCallback(onGameUpdate)
    registry.Pack.addKeyCallback(onKeyCallback)


    val circle1 = FCircle(Float2(4,8), 2F)
    val circle2 = FCircle(Float2(8,8), 5F)
    val circle3 = FCircle(Float2(4,4), 2F)
    val circle4 = FCircle(Float2(8,12),4F)
    val circle5 = FCircle(Float2(8,6),1.1F)

    val rec = FRectangle2(Float2(8,12), Float2(1F,3F))

    val shape = (((circle1 | circle2) | rec ) - circle3 - circle4 - circle5) | rec

    fillInGrid(grid, shape.density)

    val rad = 0.02F

    grid.foreachVertex( (v,s) => {
      val t = 0
      circleRenderer.add(RegularConvexPolygon2(v, rad, Nat._16), 0, if(s > 0) White else if(s < 0) White ⊗ Float3(t,t,t) else Green)
    })

    val linesAndTrs = Timer.timed(dt => s"dual contouring took $dt ms"){
       makeContour(grid, shape.density, 16) //accuracy <= 8 introduces visual artifacts
    }

    println(s"generated ${linesAndTrs._1.size} lines and ${linesAndTrs._2.size} triangles")


    linesAndTrs._1.foreach(line => linesRenderer.add(line, 0, ColorUtils.Red))
    linesAndTrs._2.foreach(tr => triangleRenderer.add(tr, 0, ColorUtils.Black))


    registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, linesRenderer, renderData)
    //registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, gridRenderer, renderData)
    //registry.Pack.renderer.User.push(LifetimeManual, TransformationNone, circleRenderer, renderData)


    println("Core pack initialized")
  }

  override def init(registry: GameRegistry): Unit = {
    work(registry)
  }

  override def deinit(registry: GameRegistry): Unit = {
    linesRenderer.deconstruct()
    gridRenderer.deconstruct()
    circleRenderer.deconstruct()

    println("Core pack deinitialized")
  }
}


/*abstract sealed class QuadTree[T](var parent : QuadTree[T]){
    var children : Vec4[QuadTree[T]] = null //2 3  <- indices based on location
                                         //0 1
  }

  case class Node[T](c0 : QuadTree[T], c1 : QuadTree[T], c2 : QuadTree[T], c3 : QuadTree[T], par : QuadTree[T] = null) extends QuadTree[T](par){
    c0.parent = this
    c1.parent = this
    c2.parent = this
    c3.parent = this
    children = Vec4 (c0, c1, c2, c3)

  }

  case class Leaf[T](var value : T, par : QuadTree[T] = null) extends QuadTree[T](par)

  object LeafType extends Enumeration {
    type LeafType = Value
    val Surface, Full, Empty = Value
  }
  import LeafType._




  def treeExample(): Unit = {
    val tree = Node(
      Leaf(1),
      Leaf(2),
      Leaf(3),
        Node(
        Leaf(4),
        Leaf(5),
        Leaf(6),
        Leaf(7)
      )
    )

    printTree(tree)

  }


  case class LeafData(samples : Float4, leafType: LeafType)


  /**
    * grid sizeX = sizeY and sizeX is multiple of 2
    * @param rec
    * @param grid
    * @param f
    * @return
    */
  def buildTree(rec: Rectangle2F, grid : VoxelGrid2[Float], f : Float2 => Float) : QuadTree[Float4] = {
    val dx = rec.extent.x / grid.sizeX

    val min = rec.center - rec.extent

    //going through each leaf bundle (4 leaves that form a node)
    cfor(0)(_ < grid.sizeX/2, _ + 1){ y =>
      cfor(0)(_ < grid.sizeX/2, _ + 1){ x =>
        val b00 = grid.get(2 * x, 2 * y)
        val b01 = grid.get(2 * x + 1, 2 * y)
        val b02 = grid.get(2 * x, 2 * y + 1)
        val b03 = grid.get(2 * x + 1, 2 * y + 1) //block 0

        val b10 = grid.get(2 * (x+1), 2 * y)
        val b11 = grid.get(2 * (x+1) + 1, 2 * y)
        val b12 = grid.get(2 * (x+1), 2 * y + 1)
        val b13 = grid.get(2 * (x+1) + 1, 2 * y + 1)  //block 1

        val b20 = grid.get(2 * x, 2 * (y+1))
        val b21 = grid.get(2 * x + 1, 2 * (y+1))
        val b22 = grid.get(2 * x, 2 * (y+1) + 1)
        val b23 = grid.get(2 * x + 1, 2 * (y+1) + 1) //block 2

        val b30 = grid.get(2 * (x+1), 2 * (y+1))
        val b31 = grid.get(2 * (x+1) + 1, 2 * (y+1))
        val b32 = grid.get(2 * (x+1), 2 * (y+1) + 1)
        val b33 = grid.get(2 * (x+1) + 1, 2 * (y+1) + 1) //block 3

        val l0 = Leaf(LeafData(Vec4(b00, b01, b02, b03), checkTreeType(b00, b01, b02, b03)))
        val l1 = Leaf(LeafData(Vec4(b10, b11, b12, b13), checkTreeType(b00, b11, b12, b13)))
        val l2 = Leaf(LeafData(Vec4(b20, b21, b22, b23), checkTreeType(b20, b21, b22, b23)))
        val l3 = Leaf(LeafData(Vec4(b30, b31, b32, b33), checkTreeType(b30, b31, b32, b33)))

        val node = Node(l0,l1,l2,l3)
      }
    }
  }





  def checkTreeType(v1: Float, v2: Float, v3: Float, v4: Float) : LeafType = {
    if(v1 <= 0 && v2 <= 0 && v3 <= 0 && v4 <= 0){
      LeafType.Full
    }else if (v1 > 0 && v2 > 0 && v3 > 0 && v4 > 0){
      LeafType.Empty
    }else{
      LeafType.Surface
    }
  }

  def printTree[T](tree : QuadTree[T], level : Int = 0) : Unit = {

    val STR = "   "
    val RENDER = "-->"
    def spaces(level : Int) : String = {
      import spire.syntax.cfor._
      val builder = new StringBuilder

      cfor(0)(_ < level, _ + 1) { _ =>
        builder ++= STR
      }

      builder.result()
    }

    val str = spaces(level) + RENDER
    str |> print

    tree match{
      case node : Node[T] =>
        "\n" |> print
        for(child <- node.children) printTree(child, level + 1)
      case leaf : Leaf[T] =>
        (" " + leaf.value) |> println
    }
  }*/