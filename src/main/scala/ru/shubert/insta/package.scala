package ru.shubert

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io._
import java.util.logging.LogManager
import javax.imageio.ImageIO

import scala.swing._
import scala.swing.event.Event
import scala.util.{Failure, Success, Try}

package object insta {
  LogManager.getLogManager.readConfiguration(getClass.getResourceAsStream("logging.properties"))

  sealed case class PhotoRepaintEvent(g: Graphics, d: Dimension) extends Event

  sealed case class PositionEvent(pos: Int) extends Event

  sealed case class ColorChooserEvent(source: AnyRef, color: Option[Color]) extends Event

  sealed case class FileChooserEvent(source: AnyRef, file: Option[File]) extends Event

  sealed case class TagChanged(value: String) extends Event

  sealed case class SloganChanged(value: String) extends Event

  /**
   * Transfer object for instagram data
   *
   * @param img photo, downloaded from instagram
   * @param user username, photo owner
   * @param avatar instagram user picture
   */
  case class InstaData(img: BufferedImage,
                       user: String,
                       avatar: BufferedImage)

  /**
   * Photo template transfer object.
   *
   * @param tag tag to be queryed
   * @param logo event logotip
   * @param slogan event slogan
   * @param color event color
   * @param position one of the three logo positions: left, right. mod
   */
  case class TemplateData(tag: String,
                          logo: Option[BufferedImage],
                          slogan: String,
                          color: Color,
                          position: Int)

  implicit class RichBoolean(val b: Boolean) extends AnyVal {
    final def option[A](a: => A): Option[A] = if (b) Some(a) else None
  }

  def readImg(path: String) = ImageIO.read(
    getClass.getResourceAsStream("img/" + path)
  )

  def loadScaled(path: String, maxW: Double, maxH: Double): Either[Throwable, BufferedImage] =
    loadScaled(new File(path), maxW, maxH)

  /**
   * Scales image to fit provided height and width. Takes into account that provided dimension may describe rectangle
   * and resizing the image accordingly. If image is less than required, will NOT stretch it.
   *
   * May throw [[java.lang.OutOfMemoryError]] or heap size exception if provided dimension takes too much memory.
   * @see http://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage
   * @param file Image to be load. Must be one of the formats Java can decode from-the-box.
   * @param maxW scale to fit this width
   * @param maxH scale to fit this height
   * @return new buffered image.
   */
  def loadScaled(file: File, maxW: Double, maxH: Double): Either[Throwable, BufferedImage] = {
    Try {
      ImageIO.read(file)
    } match {
      case Success(img) =>
        (img != null).option(img) map {
          src: BufferedImage =>
            val ratio = (maxH / src.getHeight) min (maxW / src.getWidth)
            if (ratio < 1) {
              val newH = (src.getHeight * ratio).toInt
              val newW = (src.getWidth * ratio).toInt
              val newImg = new BufferedImage(newW, newH, src.getType)

              val g = newImg.getGraphics
              g.drawImage(src, 0, 0, newW, newH, 0, 0, src.getWidth, src.getHeight, null)
              g.dispose()
              src.flush()
              Right(newImg)
            } else {
              Right(img)
            }
        } getOrElse Left(new Exception(s"Can't decode image ${file.getPath}. Is it image at all?"))
      case Failure(e) => Left(e)
    }
  }
}
