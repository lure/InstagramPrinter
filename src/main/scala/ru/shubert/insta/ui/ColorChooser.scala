package ru.shubert.insta.ui

import java.awt.event.{ActionEvent, ActionListener}
import java.awt.{Color, Insets}
import javax.swing.{JButton, SwingConstants}

import com.alee.laf.colorchooser.WebColorChooserDialog
import com.alee.utils.ImageUtils
import com.alee.utils.swing.DialogOptions
import ru.shubert.insta.ColorChooserEvent

import scala.swing.Publisher

class ColorChooser(color: Color = Color.BLUE) extends JButton()
with ActionListener with Publisher {
  setMargin(new Insets(0, 0, 0, 3))
  setHorizontalAlignment(SwingConstants.LEFT)
  addActionListener(this)

  var _value: Option[Color] = None

  def value = _value

  def value_=(c: Option[Color]) {
    _value = c
    _value map (x => {
      setIcon(ImageUtils.createColorIcon(x))
      setText(colorToText(x))
    })
  }

  private lazy val chooser = new WebColorChooserDialog()

  private def colorToText(color: Color): String = color.getRed + ", " + color.getGreen + ", " + color.getBlue

  override def actionPerformed(e: ActionEvent) {
    _value map chooser.setColor
    chooser.setVisible(true)
    if (chooser.getResult == DialogOptions.OK_OPTION) {
      value = Some(chooser.getColor)
      publish(ColorChooserEvent(this, _value))
    }
  }
}