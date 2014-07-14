package ru.shubert.insta.service

import java.awt.image.BufferedImage
import java.awt.print._
import java.awt.{Graphics, Graphics2D}
import java.util.logging.{Level, Logger}

object PrintingService{
  val logger = Logger.getLogger(getClass.toString)

  private class PrintTask(image: BufferedImage) extends Printable {

    override def print(graph: Graphics, pf: PageFormat, pageIndex: Int): Int = {
      if (pageIndex == 0) {
        val g = graph.asInstanceOf[Graphics2D]
        g.translate(pf.getImageableX, pf.getImageableY)
        graph.drawImage(image, 0, 0, pf.getImageableWidth.toInt, pf.getImageableHeight.toInt,
          0, 0, image.getWidth, image.getHeight, null)
        g.dispose()
        Printable.PAGE_EXISTS
      } else
        Printable.NO_SUCH_PAGE
    }
  }

  private def copyImage(photo: BufferedImage) = {
    val cm = photo.getColorModel
    val raster = photo.copyData(null)
    val rp = photo.isAlphaPremultiplied
    new BufferedImage(cm, raster, rp, null)
  }

  def doPrint(photo: BufferedImage, id: String, showPrintDlg: Boolean = false, showPageDlg: Boolean = false) {
    val job = PrinterJob.getPrinterJob
    val dp = job.defaultPage
    val paper = new Paper
    paper.setImageableArea(0, 0, paper.getWidth, paper.getHeight)
    dp.setPaper(paper)
    job.setJobName(id)

    job.setPrintable(new PrintTask(copyImage(photo)), dp)

    if (!showPrintDlg || job.printDialog()) {
      if (showPageDlg) job.pageDialog(dp)
      try {
        job.print()
      } catch {
        case e: PrinterException => logger.log(Level.SEVERE, e.getMessage, e)
      }
    }
  }
}