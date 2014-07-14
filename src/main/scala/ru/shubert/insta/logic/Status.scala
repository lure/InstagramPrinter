package ru.shubert.insta.logic

sealed trait Status

case class Progress(downloaded: Int, total: Int) extends Status

case class Message(msg: String) extends Status

case class Error(msg: String) extends Status

case object Photo extends Status

trait NotificationListener {
  def notifyUI(status: Status*)
}
