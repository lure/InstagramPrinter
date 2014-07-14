package ru.shubert.insta.logic

import java.io.File

import ru.shubert.insta.ui.View

import scala.swing.{Color, Publisher}

trait GUI extends Publisher{
  this: View =>

  def setControlsAvailable(flag: Boolean)

  def errorPopup(msg: String)

  def warnPopup(msg: String)

  /** ************* ACCESSOR ******************************/
  def tag_=(tag: String)

  def tag:String

  def slogan_=(tag: String)

  def slogan:String

  def past:Double

  def past_=(duration: Double)

  def secret:String

  def secret_=(pass: String)

  def token:String

  def token_=(token: String)

  def logo:Option[File]

  def logo_=(p: Option[File])

  def color:Option[Color]

  def color_=(c: Option[Color])

  def position:Int

  def position_=(pos: Int)

  def action:Int

  def action_=(a: Int)

  /** ************** UPDATE ****************************/
  def init()

  def updateStatusText(msg: String)

  def updateDownloaded(c: Int)

  def updateTotal(c: Int)

  def repaintPhoto()
}
