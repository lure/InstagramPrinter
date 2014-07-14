/**
 * Technical description https://docs.google.com/document/d/1D-nFIL-Afqleas-8cD1-lr6dgd1LFO7F0g5_Q-4_l5I/edit#
 */
package ru.shubert.insta.ui

import java.awt.event._
import java.awt.{Color, Graphics}
import java.io.File
import javax.swing._
import javax.swing.border.EmptyBorder

import com.alee.extended.label.WebLinkLabel
import com.alee.extended.panel.GroupPanel
import com.alee.extended.statusbar.{WebStatusBar, WebStatusLabel}
import com.alee.laf.combobox.WebComboBoxCellRenderer
import com.alee.laf.spinner.WebSpinner
import com.alee.managers.notification.{NotificationIcon, NotificationManager}
import com.alee.managers.tooltip.{TooltipWay, TooltipManager}
import ru.shubert.insta._
import ru.shubert.insta.logic.{GUI, StrCons}

import scala.swing.BorderPanel._
import scala.swing.TabbedPane.Page
import scala.swing._

class View extends TabbedPane with GUI {
  private val clientPanel = new ClientPanel
  private val optionPanel = new OptionPanel
  pages += new Page(StrCons.TAB1, clientPanel)
  pages += new Page(StrCons.TAB2, optionPanel)

  override def setControlsAvailable(flag: Boolean) {
    clientPanel setControlsAvailable flag
    optionPanel setControlsAvailable flag
  }

  override def errorPopup(msg: String) = NotificationManager.showNotification(msg, NotificationIcon.cross.getIcon)

  override def warnPopup(msg: String) = NotificationManager.showNotification(msg, NotificationIcon.warning.getIcon)

  listenTo(clientPanel.logoChooser, clientPanel.colorChooser, clientPanel.startButton, clientPanel)
  deafTo(this)
  reactions += {
    case e => publish(e)
  }

  /** ************* ACCESSOR ******************************/
  override def tag_=(tag: String) = clientPanel.hashInput.setText(tag)

  override def tag = clientPanel.hashInput.getText

  override def slogan_=(tag: String) = clientPanel.sloganInput.setText(tag)

  override def slogan = clientPanel.sloganInput.getText

  override def past = optionPanel.pastInput.getValue.asInstanceOf[Double]

  override def past_=(duration: Double) = optionPanel.pastInput.setValue(duration)

  override def secret = new String(optionPanel.secretInput.getPassword)

  override def secret_=(pass: String) = optionPanel.secretInput.setText(pass)

  override def token = new String(optionPanel.tokenInput.getPassword)

  override def token_=(token: String) = optionPanel.tokenInput.setText(token)

  override def logo = clientPanel.logoChooser.value

  override def logo_=(p: Option[File]) = clientPanel.logoChooser.value = p

  override def color = clientPanel.colorChooser.value

  override def color_=(c: Option[Color]) = clientPanel.colorChooser.value = c

  override def position = clientPanel.positionCombo.getSelectedIndex

  override def position_=(pos: Int) = clientPanel.positionCombo.setSelectedIndex(pos)

  override def action = optionPanel.actionCombo.getSelectedIndex

  override def action_=(a: Int) = optionPanel.actionCombo.setSelectedIndex(a)

  /** ************** UPDATE ****************************/
  override def init() = clientPanel.hashInput.requestFocusInWindow

  override def updateStatusText(msg: String) = clientPanel.statusLabel.setText(msg)

  override def updateDownloaded(c: Int) = clientPanel.gotLabel setText c.toString

  override def updateTotal(c: Int) = clientPanel.seenLabel setText c.toString

  override def repaintPhoto() = clientPanel.centerPanel.repaint()
}

private class ClientPanel extends BorderPanel {
  _root: BorderPanel =>

  background = Color.lightGray
  val Ratio = 10.0 / 15.0
  val MinDimension = new Dimension(244, 366)
  // picked from logging :(
  val logoChooser = new FileChooser(StrCons.LOGO_IS_EMPTY)
  val colorChooser = new ColorChooser()

  val startButton = new Button(StrCons.STARTBTN(true))
  startButton.preferredSize = new Dimension(115, startButton.preferredSize.getHeight.toInt)
  startButton.minimumSize = startButton.preferredSize
  startButton.maximumSize = startButton.preferredSize

  val positionCombo = new JComboBox[String](StrCons.POSITIONS) with ActionListener {
    override def actionPerformed(e: ActionEvent): Unit = publish(PositionEvent(getSelectedIndex))

    addActionListener(this)
    val r = renderer.asInstanceOf[WebComboBoxCellRenderer]
    r.getElementRenderer.setHorizontalAlignment(SwingConstants.CENTER)
    r.getBoxRenderer.setHorizontalAlignment(SwingConstants.CENTER)
  }

  val hashInput = new JTextField(StrCons.STUB_TAG, 30) with FocusListener {
    addFocusListener(this)

    override def focusGained(e: FocusEvent): Unit = Nil

    override def focusLost(e: FocusEvent): Unit = publish(TagChanged(getText))
  }

  val sloganInput = new JTextField(StrCons.STUB_SLOGAN, 30) with FocusListener {
    addFocusListener(this)

    override def focusGained(e: FocusEvent): Unit = Nil

    override def focusLost(e: FocusEvent): Unit = publish(SloganChanged(getText))
  }

  val topPanel = new BorderPanel() {
    border = Swing.EmptyBorder(5, 10, 5, 10)
    val inputPanel = new GroupPanel(false,
      hashInput,
      Box.createRigidArea(new Dimension(10, hashInput.getHeight)),
      sloganInput,
      Box.createRigidArea(new Dimension(10, hashInput.getHeight)),
      logoChooser)

    val V_DIM = new Dimension(0, 10)
    val labelPanel = new BoxPanel(Orientation.Vertical)
    labelPanel.border = Swing.EmptyBorder(5, 0, 5, 5)
    labelPanel.contents ++= Seq(
      new Label(StrCons.TAG),
      Swing.RigidBox(V_DIM),
      new Label(StrCons.SLOGAN),
      Swing.RigidBox(V_DIM),
      new Label(StrCons.LOGO))

    layout(Component.wrap(inputPanel)) = Position.Center
    layout(labelPanel) = Position.West
    layout(Component.wrap(new GroupPanel(false, startButton.peer, colorChooser, positionCombo))) = Position.East
  }

  //# PHOTO PANE

  val centerPanel = new ScrollPane {
    _center: ScrollPane =>
    background = Color.lightGray
    border = new EmptyBorder(1, 0, 0, 0)

    contents = new FlowPanel() {
      val photoWrap = new JPanel with MouseListener with KeyListener with MouseWheelListener {
        TooltipManager.setTooltip(this, StrCons.ZOOM, TooltipWay.leading)
        setBackground(Color.white)
        setMinimumSize(MinDimension)
        setFocusable(true)
        addMouseListener(this)
        addKeyListener(this)
        addMouseWheelListener(this)

        override def keyPressed(e: KeyEvent): Unit = {
          val res = e.getKeyChar match {
            case '+' | '=' if zoom < 6 => Some(0.1)
            case '-' if zoom > 1.0 => Some(-0.1)
            case _ => None
          }
          res.map { x =>
            zoom += x
            previewDim = new Dimension((MinDimension.width * zoom).toInt, (MinDimension.height * zoom).toInt)
            revalidate()
            repaint()
          }
        }

        var zoom: Double = 1.0
        var previewDim = MinDimension

        // not used anymore
        def resizeToFillParent = {
          val h = ((getParent.getSize.height - 20) * zoom).toInt
          val w = ((h * Ratio) * zoom).toInt
          val sz = new Dimension(w, h)
          if (sz.width < MinDimension.width || sz.height < MinDimension.height) MinDimension else sz
        }

        override def getPreferredSize: Dimension = previewDim

        override def paint(g: Graphics): Unit = _root.publish(PhotoRepaintEvent(g, getSize))

        override def mouseClicked(e: MouseEvent): Unit = requestFocus()

        override def keyTyped(e: KeyEvent): Unit = Nil

        override def keyReleased(e: KeyEvent): Unit = Nil

        override def mouseExited(e: MouseEvent): Unit = Nil

        override def mouseEntered(e: MouseEvent): Unit = Nil

        override def mousePressed(e: MouseEvent): Unit = Nil

        override def mouseReleased(e: MouseEvent): Unit = Nil

        override def mouseWheelMoved(e: MouseWheelEvent): Unit = if (e.isControlDown) {
          keyPressed(new KeyEvent(this, KeyEvent.KEY_PRESSED, 0, 0,
            if (e.getWheelRotation > 0) KeyEvent.VK_MINUS else KeyEvent.VK_PLUS,
            if (e.getWheelRotation > 0) '-' else '+', 0))
        }

      }
      contents += Component.wrap(photoWrap)
      peer.addMouseListener(photoWrap)
      peer.addKeyListener(photoWrap)
      peer.addMouseWheelListener(photoWrap)
    }
  }

  //# STATUS BAR
  val statusLabel = new WebStatusLabel(StrCons.STOPPED)
  val gotLabel = new WebStatusLabel(StrCons.ZERO)
  val seenLabel = new WebStatusLabel(StrCons.ZERO)

  val statusPanel = new WebStatusBar
  statusPanel.add(new WebStatusLabel(StrCons.STATUS))
  statusPanel.add(statusLabel)
  statusPanel.addToEnd(new WebStatusLabel(StrCons.DOWNLOADED))
  statusPanel.addToEnd(gotLabel)
  statusPanel.addSeparatorToEnd()
  statusPanel.addToEnd(new WebStatusLabel(StrCons.SEEN))
  statusPanel.addToEnd(seenLabel)

  //# ADDING COMPONENTS
  layout(topPanel) = Position.North
  layout(centerPanel) = Position.Center
  layout(Component.wrap(statusPanel)) = Position.South

  def setControlsAvailable(flag: Boolean) {
    startButton.text = StrCons.STARTBTN(!hashInput.isEnabled)
    logoChooser setEnabled flag
    colorChooser setEnabled flag
    hashInput setEnabled flag
    sloganInput setEnabled flag
    positionCombo setEnabled flag
  }
}

private class OptionPanel extends BorderPanel {
  border = Swing.EmptyBorder(5, 10, 5, 10)
  val V_DIM = new Dimension(0, 10)
  val labelPanel = new BoxPanel(Orientation.Vertical)
  labelPanel.border = Swing.EmptyBorder(5, 0, 5, 5)
  labelPanel.contents ++= Seq(
    new Label(StrCons.SECRET),
    Swing.RigidBox(V_DIM),
    new Label(StrCons.TOKEN),
    Swing.RigidBox(V_DIM),
    new Label(StrCons.ACTION),
    Swing.RigidBox(V_DIM),
    new Label(StrCons.DURATION))

  val secretInput = new JPasswordField(StrCons.STUB_SECRET, 30)
  val tokenInput = new JPasswordField(StrCons.STUB_TOKEN, 30)
  val pastInput = new WebSpinner(new SpinnerNumberModel(3.0, 0.0, 600.0, 0.1))
  val actionCombo = new JComboBox[String](StrCons.ACTIONS)
  val renderer = actionCombo.getRenderer.asInstanceOf[WebComboBoxCellRenderer]
  renderer.getElementRenderer.setHorizontalAlignment(SwingConstants.CENTER)
  renderer.getBoxRenderer.setHorizontalAlignment(SwingConstants.CENTER)

  val T_DIM = new Dimension(10, tokenInput.getHeight)
  val inputPanel = new GroupPanel(false,
    secretInput, Box.createRigidArea(T_DIM),
    tokenInput, Box.createRigidArea(T_DIM),
    actionCombo, Box.createRigidArea(T_DIM),
    pastInput
  )

  val descrPanel = new JPanel()
  val webLink = new WebLinkLabel(StrCons.ILINK)
  webLink.setLink(StrCons.ILINK)
  descrPanel.add(webLink)

  layout(Component.wrap(inputPanel)) = Position.Center
  layout(labelPanel) = Position.West
  layout(Component.wrap(descrPanel)) = Position.South

  def setControlsAvailable(flag: Boolean) {
    secretInput setEnabled flag
    tokenInput setEnabled flag
    pastInput setEnabled flag
    actionCombo setEnabled flag
  }
}