
lazy val coreSettings = Seq(
  name := "voxelized2d",
  version := "0.0.1",
  scalaVersion := "2.12.2",
  organizationName := "org.voxelized"
)

val lwjglOrganization = "org.lwjgl"
val lwjglVersion = "3.1.2"
import SystemUtils.OS.Name._
val natives = SystemUtils.OS.here.name match{
  case Windows => "natives-windows"
  case Linux => "natives-linux"
  case Mac => "natives-macos"
  case _ => throw new Exception("LWJGL3 NATIVES: OS not supported !")
}

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val `voxelized2d` = (project in file(".")).settings(coreSettings)//.dependsOn(RootProject(uri("git://github.com/Russoul/UniScalaLib.git")))



//DEPENDECIES-----------------------------------------------------------------
//LWJGL 3
libraryDependencies += lwjglOrganization % "lwjgl"          % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl"          % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-assimp"   % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-assimp"   % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-bgfx"     % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-egl"      % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-glfw"     % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-glfw"     % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-jawt"     % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-jemalloc" % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-jemalloc" % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-lmdb"     % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-nanovg"   % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-nfd"      % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-nuklear"  % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-openal"   % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-openal"   % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-opengl"   % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-opengl"   % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-opencl"   % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-sse"      % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-par"      % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-stb"      % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-stb"   % lwjglVersion classifier natives
libraryDependencies += lwjglOrganization % "lwjgl-xxhash"   % lwjglVersion
libraryDependencies += lwjglOrganization % "lwjgl-yoga"     % lwjglVersion
//...

//UniScalaLibrary
libraryDependencies += "org.russoul" %% "uniscalalib" % "0.0.1"
libraryDependencies in Scope.GlobalScope += "org.typelevel" %% "spire" % "0.14.1"
//...




//-----------------------------------------------------------------------------------

/*lazy val reloadGitDeps = taskKey[Unit]("wipe out data from ~/.sbt/0.13/staging/")


reloadGitDeps := {
  val home = sys.env("HOMEPATH")
  println("\"" + home + "\\.sbt\\0.13\\staging\"")
  Process("rd /s /q \"" + home + "\\.sbt\\0.13\\staging\"" + " /s /q", file(".")) !


}*/
