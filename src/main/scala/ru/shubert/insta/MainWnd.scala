package ru.shubert.insta

import javax.swing.UIManager

import com.alee.laf.WebLookAndFeel
import ru.shubert.insta.logic.Controller
import ru.shubert.insta.ui.View

import scala.swing.{Dimension, MainFrame, SimpleSwingApplication}

//http://www.javasoft.de/synthetica/screenshots/
//https://java.net/projects/substance/
//http://weblookandfeel.com/download/
//http://www.jwrapper.com/blog/6-great-look-and-feels-to-make-your-java-app-pretty
object MainWnd extends SimpleSwingApplication {
  UIManager.setLookAndFeel(new WebLookAndFeel())
  val view = new View
  val controller = new Controller(view)

  def top = new MainFrame {
    iconImage = readImg("app.png")
    title = "Сканер изображений Instagram v1.15"
    val startDim = new Dimension(640, 480)
    minimumSize = startDim
    preferredSize = startDim
    contents = view
    resizable = true
    centerOnScreen()
    view.init()

    override def closeOperation(): Unit = {
      controller.shutdown()
      super.closeOperation()
    }
  }
}
