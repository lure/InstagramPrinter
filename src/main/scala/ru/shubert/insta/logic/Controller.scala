package ru.shubert.insta.logic

import java.awt.image.BufferedImage
import java.awt.{Dimension, Graphics}
import java.io.File
import javax.swing.SwingUtilities

import ru.shubert.insta._
import ru.shubert.insta.service.{InstagramClient, Template}

import scala.swing.Reactor
import scala.swing.event.ButtonClicked

/**
 * May be totally merged into view.
 */
class Controller(val view: GUI) extends Reactor with NotificationListener {
  val settingsFile = "settings.cfg"
  var cachedLogo: Option[BufferedImage] = None
  var cachedLogoPath: File = _
  val model: Model = Model.loadSetting(settingsFile)
  syncViewWithModel()
  exampleTemplate()

  // lazy guarantees that semi-constructed controller will not escape
  // it must be rewritten if client call change
  lazy val client = new InstagramClient(this)

  listenTo(view)
  reactions += {
    case e: ButtonClicked => if (!model.scanning) startClient() else stopClient()
    case PhotoRepaintEvent(g, d) => onPreviewRepaint(g, d)
    case event => event match {
      case FileChooserEvent(_, f) => if (f != model.logo) {
        model.logo = f
        redrawLogo()
      }
      case PositionEvent(p) => if (model.pos != p) {
        model.pos = p
        redrawLogo()
      }
      case TagChanged(t) => if (model.tag != t) {
        model.tag = t
        Template.drawTag(model.tag)
      }
      case ColorChooserEvent(_, c) => if (model.color != c) {
        model.color = c
        Template.drawSlogan(model.slogan, model.color.get)
      }
      case SloganChanged(t) => if (model.slogan != t) {
        model.slogan = t
        Template.drawSlogan(model.slogan, model.color.get)
      }
      case _ =>
    }
      view.repaintPhoto()
  }

  def startClient() = {
    val vTag = model.cleanAndSetTag(view.tag)
    view.tag = model.tag
    model.secret = view.secret
    model.token = view.token
    model.action = view.action
    model.past = (view.past * 60).toInt

    if (model.tag.isEmpty) {
      updateStatus(Error(StrCons.ERROR_TAG), Progress(0, 0))
    } else {
      vTag.foreach(_ => view.warnPopup(StrCons.WARN_TAG))
      view.setControlsAvailable(flag = false)
      updateStatus(Message(StrCons.SCAN), Progress(0, 0))
      client.start(model.tag, model.past, model.token, model.secret, model.action)
      model.scanning = true
    }
  }

  private def stopClient(reason: String = StrCons.STOPPED) = {
    view.setControlsAvailable(flag = true)
    updateStatus(Message(reason))
    client.stop()
    model.scanning = false
  }

  private def updateStatus(status: Status*) {
    status foreach {
      case Message(m) => view.updateStatusText(m)
      case Progress(down, total) =>
        view.updateDownloaded(down)
        view.updateTotal(total)
      case Photo => view.repaintPhoto()
      case Error(m) =>
        stopClient(m)
        view.errorPopup(m)
    }
  }

  def notifyUI(status: Status*) {
    SwingUtilities.invokeLater(new Runnable {
      override def run(): Unit = updateStatus(status: _*)
    })
  }

  private def onPreviewRepaint(g: Graphics, d: Dimension) = {
    g.drawImage(Template.bitmap,
      0, 0, d.getWidth.toInt, d.getHeight.toInt,
      0, 0, Template.bitmap.getWidth, Template.bitmap.getHeight,
      null)
  }

  def shutdown() = {
    model.saveSettings(settingsFile)
    client.stop()
  }

  private def syncViewWithModel() {
    view.tag = model.tag
    view.position = model.pos
    view.logo = model.logo
    view.slogan = model.slogan
    view.color = model.color

    view.action = model.action
    view.past = model.past / 60.0
    view.secret = model.secret
    view.token = model.token
  }

  def redrawLogo() = {
    model.logo foreach {
      path => if (path != cachedLogoPath) {
        cachedLogo foreach (_.flush())
        cachedLogo = loadScaled(path, Template.logoMaxW, Template.logoMaxH) match {
          case Right(newLogo) => Some(newLogo)
          case Left(e) => updateStatus(Message(String.format(StrCons.ERROR_LOAD_LOGO, e.getMessage)))
            None
        }
      }
    }
    Template.drawLogo(cachedLogo, model.pos)
    cachedLogoPath = model.logo.orNull
  }

  private def exampleTemplate(): Unit = {
    Template.reset()
    redrawLogo()
    Template.drawTag(model.tag)
    Template.drawSlogan(model.slogan, model.color.get)
    Template.drawInstaInfo(new InstaData(readImg("photo.jpg"), StrCons.STUB_USER, readImg("avatar.jpg")))
    view.repaintPhoto()
  }
}

