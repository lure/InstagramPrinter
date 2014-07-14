package ru.shubert.insta.logic

import java.awt.Color
import java.io.File
import javax.xml.bind.JAXB
import javax.xml.bind.annotation.adapters.{XmlAdapter, XmlJavaTypeAdapter}
import javax.xml.bind.annotation.{XmlElement, XmlRootElement}

import ru.shubert.insta.RichBoolean

import scala.util.Try

@XmlRootElement(name = "settings")
class Model private {
  var scanning = false

  @XmlElement(name = "tag") var tag = StrCons.STUB_TAG
  @XmlElement var slogan = StrCons.STUB_SLOGAN

  @XmlElement(name = "slogan-color")
  @XmlJavaTypeAdapter(classOf[Model.ColorOptionXmlAdapter])
  var color: Option[Color] = Some(Color.BLUE)

  @XmlElement
  @XmlJavaTypeAdapter(classOf[Model.FileOptionXmlAdapter])
  var logo: Option[File] = None
  @XmlElement(name = "logo-position") var pos = 0

  @XmlElement(name = "retrospection-sec") var past = 0

  @XmlElement var action = 0
  @XmlElement var token = StrCons.STUB_TOKEN
  @XmlElement var secret = StrCons.STUB_SECRET

  def saveSettings(path: String) {
    JAXB.marshal(this, new File(path))
  }

  /**
   * Cleans entered tag from special characters and whitespaces
   * @param newTag user input tag
   * @return true if tag was changed due clearance
   */
  def cleanAndSetTag(newTag: String): Option[String] = {
    tag = newTag.replaceAll("[\\s\\t#\\.\\!\\$\\%\\^\\&\\*\\+\\(\\)]+", "")
    (tag != newTag).option(tag)
  }
}

object Model {
  def loadSetting(path: String) = Try {
    JAXB.unmarshal(new File(path), classOf[Model])
  }.getOrElse(new Model())

  def isNotEmpty(s: String) = (s != null) && (!s.trim.isEmpty)

  //http://krasserm.blogspot.ru/2012/02/using-jaxb-for-xml-and-json-apis-in.html
  class ColorOptionXmlAdapter() extends XmlAdapter[String, Option[Color]] {
    override def unmarshal(v: String): Option[Color] = {
      isNotEmpty(v).option(Color.decode(v))
    }

    override def marshal(v: Option[Color]): String = v match {
      case Some(c) => "#" + Integer.toHexString(c.getRGB).drop(2)
      case None => ""
    }
  }

  class FileOptionXmlAdapter() extends XmlAdapter[String, Option[File]] {
    override def unmarshal(v: String): Option[File] = {
      isNotEmpty(v).option(new File(v))
    }


    override def marshal(v: Option[File]): String = v.getOrElse("").toString
  }

}

