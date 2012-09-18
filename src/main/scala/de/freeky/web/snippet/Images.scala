package de.freeky.web.snippet

import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.sql.Timestamp
import java.text.SimpleDateFormat

import org.squeryl.PrimitiveTypeMode._

import de.freeky.web.model._
import javax.imageio.ImageIO
import net.liftweb.common._
import net.liftweb.http.S._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.JsExp._
import net.liftweb.http.js._
import net.liftweb.http._
import net.liftweb.util.Helpers._
import net.liftweb.util._

/**
 * Paths:
 * - "/image/%s/%s"
 * - "/image/list"
 * - "/image/list/page/%d"
 * - "/image/detail/%d"
 */
class Images extends DispatchSnippet with Logger {
  def dispatch: DispatchIt = _ match {
    case "upload" => upload
    case "list" => list
    case "detail" => detail
  }

  def upload = {

    val currentUser = loggedInUser.map(_.id).openOr(0L)

    def processUpload() = {
      S.request.map(_.uploadedFiles.map(_ match {
        case FileParamHolder(_, null, _, _) => S.error("No file uploaded")
        case FileParamHolder(_, mime, fileName, data) if mime.startsWith("image/") => {
          var image = new Image(0L, fileName, currentUser, mime, randomString(16), new Timestamp(millis))
          transaction { image = FreekyDB.images.insert(image) }
          
          val file = image.file
          if (!file.createNewFile()) error("file: %s could not be created".format(file.getAbsoluteFile()))
          val fos = new FileOutputStream(file)
          fos.write(data)
          fos.close()
          
          S.notice("Uploaded image: %s" format image.name)
        }
        case _ => S.error("Invalid upload")
      }))

    }

    ".fileupload" #> SHtml.fileUpload(x => ()) &
      ".submit" #> SHtml.submit(S ? "upload", processUpload)
  }

  def list = {
    val page = S.param("page").openOr("1").toInt
    val pagesize = S.attr("pagesize").openOr("30").toInt
    val entries = transaction { FreekyDB.images.Count.toLong }

    var images: List[Image] = transaction {
      FreekyDB.images.page((page - 1) * pagesize, pagesize).toList
    }

    // make prev page link if required
    def prev =
      if (page <= 1) "*" #> ""
      else
        "* [href]" #> "/image/list/page/%d".format(page - 1)

    // make next page link if required
    def next =
      if (maxpages(entries, pagesize) <= page) "*" #> ""
      else "* [href]" #> "/image/list/page/%d".format(page + 1)

    // processes the given news entries to real HTML-entries 
    def bindImages = images.map(i => {
      ".name [href]" #> i.link &
        ".name *" #> i.name &
        ".uploader" #> transaction{i.uploader.headOption.map(_.name).getOrElse("unknown")} &
        ".id" #> i.id &
        ".detaillink [href]" #> i.detailLink
    })

    ".entry" #> bindImages &
      ".previsious" #> prev &
      ".next" #> next
  }

  /**
   * maxpages Method
   * calculates max amount of pages for given entries,
   * why there could be max $pagesize entries per page
   * @param entries
   * @param pagesize
   * @return
   */
  def maxpages(entries: Long, pagesize: Int): Long =
    if (entries % pagesize > 0)
      (entries / pagesize) + 1
    else
      entries / pagesize

  /*def delete = {
    val imageId = S.param("id").openOr("0").toLong
    val image = transaction{
      FreekyDB.images.lookup(imageId)
    }

    def processDelete() = {
      image.delete_!
      S.error("Image is deleted!")
      S.redirectTo("/admin/picture/list")
    }

    ".submit" #> SHtml.submit(S ? "delete", processDelete) &
      ".image" #> Image.toHTML(image)
  }*/

  def detail = {

    def timestamp = new SimpleDateFormat("dd MMMMM, yyyy", java.util.Locale.ENGLISH)

    transaction{FreekyDB.images.lookup(S.param("id").openOr("0").toLong).headOption} match {
      case Some(image) => {

        def deleteImage(): Unit = {
          val file = new File(image.path)
          if (!file.delete) error("image: %s could not be deleted".format(image.path))

          transaction { FreekyDB.images.delete(image.id) }
        }

        val imageData = ImageIO.read(new java.io.File(image.path))

        ".image" #> image.toHTML(300, 300) &
          ".id" #> image.id &
          ".name" #> image.name &
          ".height" #> imageData.getHeight &
          ".width" #> imageData.getWidth &
          ".uploader" #> transaction{image.uploader.head.name} &
          ".date" #> timestamp.format(image.uploaded) &
          ".delete" #> SHtml.submit(S ? "delete", deleteImage, "onclick" ->
            JsIf(JsRaw("confirm('Sind Sie sicher?')"),
              JsReturn(true), JsReturn(false)).toJsCmd)
      }
      case _ => error("given image was not found"); "*" #> ""
    }

  }
}

object Images extends Logger {
  def serveImage(secure: String, name: String): Box[LiftResponse] = {

    transaction {
      FreekyDB.images.where(i => (i.secure like secure) and (i.name like name)).headOption.map(image => {
        if (!image.file.canRead()) 
          error("file: %s could not be read" format (image.file.getAbsoluteFile()))
        val originalImage = ImageIO.read(image.file)

        val widthParam = S.param("width").openOr(Int.MaxValue.toString).toInt
        var width =
          if (widthParam.abs < originalImage.getWidth)
            widthParam.abs
          else
            originalImage.getWidth

        val heightParam = S.param("height").openOr(Int.MaxValue.toString).toInt
        var height =
          if (heightParam.abs < originalImage.getHeight)
            heightParam.abs
          else
            originalImage.getHeight

        val widthRatio = originalImage.getWidth.toDouble / width
        val heightRatio = originalImage.getHeight.toDouble / height

        if (widthRatio > heightRatio) {
          height = (originalImage.getHeight / widthRatio).toInt
        } else {
          width = (originalImage.getWidth / heightRatio).toInt
        }

        val byteStream = new ByteArrayOutputStream()

        ImageIO.write(resize(originalImage, width, height),
          image.mimeType.substring("image/".length), byteStream)

        InMemoryResponse(
          byteStream.toByteArray,
          ("Content-Type" -> image.mimeType) :: Nil,
          Nil,
          200)
      })
    }
  }

  private def resize(image: BufferedImage, width: Int, height: Int): BufferedImage = {
    val resizedImage = new BufferedImage(width, height,
      if (image.getType == 0) BufferedImage.TYPE_INT_ARGB else image.getType);
    val g = resizedImage.createGraphics();
    g.drawImage(image, 0, 0, width, height, null);
    g.dispose();
    return resizedImage;
  }
}