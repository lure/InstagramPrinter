package ru.shubert.insta.ui

import java.awt.event.{ActionEvent, ActionListener}
import java.io.File
import javax.swing.{JButton, JFileChooser, SwingConstants}

import com.alee.laf.GlobalConstants
import com.alee.utils.FileUtils
import ru.shubert.insta._

import scala.swing.Publisher

class FileChooser(caption: String) extends JButton(caption) with ActionListener with Publisher {
  //# LOGO CHOOSER
  private var _value: Option[File] = None
  setHorizontalTextPosition(SwingConstants.LEFT)
  addActionListener(this)

  def value = _value

  def value_=(v: Option[File]) {
    _value = v
    _value match {
      case Some(file) =>
        setIcon(FileUtils.getFileIcon(file))
        setText(FileUtils.getDisplayFileName(file))
      case None =>
        setIcon(null)
        setText("Логотип не выбран")
    }
  }

  private lazy val imageChooser = new JFileChooser() {
    //new WebFileChooser() {  WebLaf chooser is broken
    addChoosableFileFilter(GlobalConstants.IMAGES_FILTER)
    setMultiSelectionEnabled(false)
    setAcceptAllFileFilterUsed(false)
    setFileSelectionMode(JFileChooser.FILES_ONLY)
  }

  override def actionPerformed(e: ActionEvent) {
    _value.map(imageChooser.setSelectedFile)
    value = (imageChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION).option(imageChooser.getSelectedFile)
    publish(FileChooserEvent(this, _value))
  }
}
