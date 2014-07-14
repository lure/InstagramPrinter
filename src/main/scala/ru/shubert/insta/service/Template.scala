package ru.shubert.insta.service

import java.awt._
import java.awt.font._
import java.awt.image.BufferedImage
import java.text.AttributedString

import ru.shubert.insta.InstaData

import scala.collection.JavaConverters._

// Template canvas. It may be cached as far as there must be a few different dimensions
object Template {
  /* all measures are given in millimetres */
  val real_padding = 6
  val real_w = 100
  val real_h = 150
  val real_photoSide = real_w - real_padding * 2

  // Paper photoSide = 88mm, x = 100mm = > x = photoSide*100/88 => 727
  val photoSide = 640
  val width = photoSide * real_w / real_photoSide
  val height = photoSide * real_h / real_photoSide

  // Photo top left corner position
  val padding = (width - photoSide) / 2
  val photoX = padding
  val photoY = (height - photoSide) / 2

  /* real 150px*/
  val avaSide = 150
  val avaX = padding
  val avaY = (photoY - avaSide) / 2

  val logoY = photoY + photoSide + padding
  val logoMaxH = photoY - (padding * 2)
  val logoMaxW = width - (padding * 2)

  val FontSize = 18
  val FontName = "Calibri"
  val PrebuiltFont = new Font(FontName, Font.BOLD, FontSize)
  val userNameX = avaX + avaSide + 10
  val sloganX = width / 2 + padding

  private val canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

  def bitmap: BufferedImage = this.synchronized(canvas)

  private def withGraphisc(f: (Graphics) => Unit) = this.synchronized {
    val g = canvas.getGraphics
    f(g)
    g.dispose()
  }

  def drawTag(tag: String) = withGraphisc { g =>
    val tagBtn = photoY + photoSide
    cleanCanvas(g, 0, tagBtn + 1, canvas.getWidth, logoY - tagBtn)
    g.setFont(PrebuiltFont)
    g.setColor(Color.BLACK)
    g.drawString("#" + tag, padding, photoY + photoSide + 14)
  }

  def reset() = withGraphisc { g =>
    cleanCanvas(g, 0, 0, canvas.getWidth, canvas.getHeight)
  }

  private def cleanCanvas(g: Graphics, x: Int, y: Int, w: Int, h: Int) = {
    g.setColor(Color.white)
    g.fillRect(x, y, w, h)
  }

  /**
   * Resize log up to max H = logoMaxH, calculate new position in the center of the screen
   * and draw to template canvas
   * @param logo if none just clearing the canvas
   */
  def drawLogo(logo: Option[BufferedImage], pos: Int) = withGraphisc { g =>
    cleanCanvas(g, photoX, logoY, logoMaxW, logoMaxH)
    logo.map { img =>
      val x0 = pos match {
        case 0 => photoX
        case 1 => (width - img.getWidth) / 2
        case _ => width - img.getWidth - photoX
      }
      g.drawImage(img, x0, logoY, x0 + img.getWidth, logoY + logoMaxH, 0, 0, img.getWidth, img.getHeight, null)
    }
  }

  /**
   * Drawing a string on the canvas.<br />
   * Used the hints from [[http://docs.oracle.com/javase/tutorial/2d/text/drawmulstring.html ]] <br/>
   * @param str text to be drawn
   * @param color color that string must be painted
   * @return text bound rectangle for further cleaning.
   */
  def drawSlogan(str: String, color: Color) = withGraphisc { g =>
    cleanCanvas(g, sloganX, 0, canvas.getWidth, photoY)
    val maxWidth = photoSide / 2 - padding
    val attrMap = Map(TextAttribute.FONT -> PrebuiltFont, TextAttribute.FOREGROUND -> color)
    val text = new AttributedString(str, attrMap.asJava).getIterator
    val g2d = g.asInstanceOf[Graphics2D]
    val textEnd = text.getEndIndex
    val frc: FontRenderContext = g2d.getFontRenderContext

    val lineMeasurer = new LineBreakMeasurer(text, frc)
    lineMeasurer.setPosition(text.getBeginIndex)

    val list = (Iterator.continually({
      // trimming trailing spaces on line ends. Looks like a bicycle
      val offset = lineMeasurer.nextOffset(maxWidth)
      val withoutWhitespace = if (offset < textEnd && Character.isWhitespace(str.codePointAt(offset - 1)))
        offset - 1
      else
        offset
      lineMeasurer.nextLayout(maxWidth, withoutWhitespace, false)
    }) takeWhile (_ != null)).toList

    listRenderer(g2d, list, maxWidth, sloganX + maxWidth - _.getAdvance)
  }

  /**
   * Main entry point.
   * @param data changes betweem invocations
   */
  def drawInstaInfo(data: InstaData) = withGraphisc { g =>
    // photo
    g.drawImage(data.img, photoX, photoY, photoX + photoSide, photoY + photoSide,
      0, 0, data.img.getWidth, data.img.getHeight, null)
    // user avatar
    g.drawImage(data.avatar, avaX, avaY, avaX + avaSide, avaY + avaSide,
      0, 0, data.avatar.getWidth, data.avatar.getHeight, null)
    /* user */
    cleanCanvas(g, avaX + avaSide + 1, avaY, sloganX - userNameX - 1, avaSide)
    drawUserName(g, data.user, Color.BLACK)
    //Resource cleanup
    data.avatar.flush()
    data.img.flush()
  }

  private def drawUserName(g: Graphics, str: String, color: Color) {
    val maxWidth = photoSide / 2 - avaSide - 10
    val map = Map(TextAttribute.FONT -> PrebuiltFont, TextAttribute.FOREGROUND -> color)
    val text = new AttributedString(str, map.asJava).getIterator

    val g2d = g.asInstanceOf[Graphics2D]
    val frc: FontRenderContext = g2d.getFontRenderContext
    val measurer = new TextMeasurer(text, frc)

    var pos = 0
    val list = (Iterator.continually({
      if (pos < str.length) {
        val tmp = measurer.getLineBreakIndex(pos, maxWidth)
        val lay = measurer.getLayout(pos, tmp)
        pos = tmp
        (pos, lay)
      } else (str.length + 1, null)
    }) takeWhile (_._1 <= str.length)).toList map (_._2)

    listRenderer(g2d, list, maxWidth, _ => userNameX)
  }

  private type _GetX = TextLayout => Float

  private def listRenderer(g: Graphics2D, list: Seq[TextLayout], maxWidth: Int, getX: _GetX) {
    val h = list(0).getDescent + list(0).getLeading
    val startY = (photoY - list(0).getBounds.getHeight * list.size).toInt / 2 - list(0).getDescent

    (0 /: list) { case (acc, layout) =>
      layout.draw(g, getX(layout), startY + acc * (h + layout.getAscent))
      acc + 1
    }
  }
}
