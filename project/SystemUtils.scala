/**
  * Created by russoul on 15.07.2017.
  */
object SystemUtils {
  case class OS(name: OS.Name.Name, arch: OS.Arch.Arch)

  object OS {

    object Name extends Enumeration {
      type Name = Value
      val Windows, Linux, Mac = Value
    }

    object Arch extends Enumeration {
      type Arch = Value
      val X86, AMD64, PPC = Value
    }

    val here = {
      val name = sys.props.get("os.name").flatMap(n =>
        OS.Name.values.find(v => n.contains(v.toString))
      ).getOrElse(sys.error("Unknown OS name!"))

      val arch = sys.props.get("os.arch").flatMap(a =>
        OS.Arch.values.find(_.toString.equalsIgnoreCase(a))
      ).getOrElse(sys.error("Unknown OS arch!"))

      OS(name, arch)
    }
  }

}
