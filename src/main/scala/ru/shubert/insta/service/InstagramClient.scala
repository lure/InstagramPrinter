package ru.shubert.insta.service

import java.awt.image.BufferedImage
import java.io._
import java.text.DateFormat
import java.util.Date
import java.util.logging.{Level, Logger}
import javax.imageio.ImageIO

import org.jinstagram.Instagram
import org.jinstagram.auth.model.Token
import org.jinstagram.entity.common.Pagination
import org.jinstagram.entity.users.feed.MediaFeedData
import ru.shubert.insta.logic.StrCons._
import ru.shubert.insta.logic.{Error, Message, NotificationListener, Photo, Progress}
import ru.shubert.insta.{InstaData, RichBoolean}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalaj.http.{Http, HttpOptions}

/**
 * What can be improved? Firs of all, there can be a map 'tag'->'worker' that allows to run
 * separate tag request simultaneously. Next, there may be different controllers, transmitted to worker
 * After all there is no real 'connection lost' handling
 */
class InstagramClient(val listener: NotificationListener) {
  private var worker: Option[InstagramWorker] = None
  val CacheDir = "cache"
  new File(CacheDir).mkdir()

  /**
   * @param tag to track
   * @param past how far go to past
   * @param token auth token
   * @param secret client secret
   * @param actionIndex what to do with photos
   */
  def start(tag: String, past: Int, token: String, secret: String, actionIndex: Int) {
    worker.getOrElse {
      new File(s"$CacheDir/$tag").mkdir()
      val client = new InstagramWorker(ClientConfig(token, secret, past, tag, actionIndex))
      client.setDaemon(true)
      client.start()
      worker = Some(client)
    }
  }

  def stop() {
    worker foreach (_.kill())
    worker = None
  }

  final case class ClientConfig(token: String, secret: String, past: Int, tag: String, action: Int)

  private class InstagramWorker(cfg: ClientConfig) extends Thread {
    val logger = Logger.getLogger(getClass.toString)
    val startDateTime = System.currentTimeMillis() / 1000 - cfg.past
    val pauseDuration = 5000
    val instagram = new Instagram(new Token(cfg.token, cfg.secret))
    var pagination: Option[Pagination] = None
    var received = 0
    val DF = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    val tagLogPath = s"$CacheDir/${cfg.tag}/${cfg.tag}.log"

    def photoPath(id: String, tag: String) = s"$CacheDir/$tag/$id.png"

    @volatile
    var isStopped: Boolean = false

    val options = List(HttpOptions.connTimeout(5000), HttpOptions.readTimeout(5000))

    var seen: Set[String] = Try {
      (for {line <- Source.fromFile(tagLogPath).getLines()} yield line.trim).toSet
    }.getOrElse(Set())

    def storeSeenLog() {
      val writer = new PrintWriter(new File(tagLogPath))
      seen foreach writer.println
      writer flush()
      writer close()
      seen = null
    }

    /**
     * получаем набор.
     * Заносим "свежее" и "новое" в коллекцию. Просматриваем назад, пока foreach("новый и свежий")
     * @return
     */
    @tailrec
    final def getUpdates(page: Option[Pagination], acc: List[MediaFeedData]): Try[List[MediaFeedData]] = {
      listener.notifyUI(Message(REQUEST))
      Try {
        page match {
          case None => instagram getRecentMediaTags cfg.tag
          case Some(p) => instagram.getRecentMediaTags(cfg.tag, p.getMinTagId, null) //назад идем
        }
      } match {
        case Success(m) =>
          val fullSize = m.getData.size()
          val newPage = (null != m.getPagination.getNextMaxId).option(m.getPagination)
          val newMedia = m.getData.toList filter (x => !seen.contains(x.getId) && startDateTime <= x.getCreatedTime.toLong)
          logger.log(Level.INFO, s"found $fullSize images, new ${newMedia.size}")

          if (!isStopped && newMedia.size > 0 && fullSize == newMedia.size) {
            listener.notifyUI(Message(RETROSPECTION))
            logger.log(Level.INFO, s"Retrospecting, last timedate:${formDateTime(newMedia.last.getCreatedTime.toLong)}")
            getUpdates(newPage, newMedia ++ acc)
          } else
            Success(newMedia)
        case Failure(x) => Failure(x)
      }
    }

    /**
     * Intention was to log successfully downloaded count but it's required more time than I have
     * Also it can be gracefully stopped on `stop` signal
     * @param lst media list to iterate
     * @param count Down counter of downloads error. 0 means everything was download.
     * @return photos that can't be downloaded
     */
    @tailrec
    final def download(lst: List[MediaFeedData], count: Int): Int = lst match {
      case Nil => count
      case imgData :: tail =>
        val id = imgData.getId
        val url = imgData.getImages.getStandardResolution.getImageUrl
        val user = imgData.getUser

        listener.notifyUI(Message(DOWNLOAD))
        Try {
          val reqImg: Http.Request = Http(url).options(options)
          val image = ImageIO.read(new ByteArrayInputStream(reqImg.asBytes))

          val reqAva: Http.Request = Http(user.getProfilePictureUrl).options(options)
          val avatar = ImageIO.read(new ByteArrayInputStream(reqAva.asBytes))

          processData(image, imgData, avatar)
          logger.log(Level.INFO, s"received  $url | $id | ${user.getUserName}")
          seen += id
          listener.notifyUI(Progress(received, seen.size + lst.size - 1))
        } recover {
          case e =>
            logger.log(Level.WARNING, s"failed to download or save image $url | $id | ${user.getUserName} with $e")
            listener.notifyUI(Message(ERROR.format(e.getMessage)))
        }
        if (!isStopped) download(tail, count - 1) else count
    }

    def processData(image: BufferedImage, imgData: MediaFeedData, avatar: BufferedImage) = {
      Template.drawInstaInfo(new InstaData(image, imgData.getUser.getUserName, avatar))
      listener.notifyUI(Photo)
      action(imgData.getId)
      received += 1
    }

    private val saveA = (id: String) => ImageIO.write(Template.bitmap, "PNG", new File(photoPath(id, cfg.tag)))
    private val printA = (id: String) => PrintingService.doPrint(Template.bitmap, id)
    private val printSaveA = (id: String) => {
      saveA(id)
      printA(id)
    }

    val action = Map[Int, (String) => Any](0 -> printA, 1 -> saveA, 2 -> printSaveA)(cfg.action)

    def kill() {
      isStopped = true
      this.interrupt()
    }

    def formDateTime(stamp: Long) = DF.format(new Date(stamp * 1000))

    override def run(): Unit = {
      logger.log(Level.INFO, s"Starting #${cfg.tag}")
      while (!isStopped) {
        getUpdates(None, Nil) match {
          case Success(lst) =>
            if (lst.nonEmpty && !isStopped) download(lst, lst.size)
            if (!isStopped) {
              listener.notifyUI(Message(PAUSE.format(pauseDuration / 1000)))
              logger.log(Level.INFO, s"Sleeping $pauseDuration ms")
              try {
                Thread.sleep(pauseDuration)
              } catch {
                case e: InterruptedException => isStopped = true
              }
            }
          case Failure(e) =>
            listener.notifyUI(Error(e.getMessage))
            logger.log(Level.SEVERE, "Exception occurred: " + e.toString)
        }
      }

      logger.log(Level.INFO, s"Stopping #${cfg.tag}")
      storeSeenLog()
    }
  }

}