/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.upload.fileupload {

  import play.api.inject.guice.GuiceApplicationBuilder
  import play.api.test._
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import java.io.FileWriter

  import controllers._
  import play.api.libs.Files.TemporaryFile

  import java.io.File
  import java.nio.file.attribute.PosixFilePermission._
  import java.nio.file.attribute.PosixFilePermissions
  import java.nio.file.Files

  import akka.stream.IOResult
  import akka.stream.scaladsl._
  import akka.util.ByteString
  import play.api._
  import play.api.libs.streams._
  import play.api.mvc.MultipartFormData.FilePart
  import play.api.mvc._
  import play.core.parsers.Multipart.FileInfo

  @RunWith(classOf[JUnitRunner])
  class ScalaFileUploadSpec extends PlaySpecification with Controller {

    "A scala file upload" should {

      "upload file" in {
        val tmpFile = new File("/tmp/picture/tmpformuploaded")
        writeFile(tmpFile, "hello")

        new File("/tmp/picture").mkdirs()
        val uploaded = new File("/tmp/picture/formuploaded")
        uploaded.delete()

        //#upload-file-action
        def upload = Action(parse.multipartFormData) { request =>
          request.body.file("picture").map { picture =>
            
            // only get the last part of the filename
            // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
            val filename = Paths.get(picture.filename).getFileName
            
            picture.ref.moveTo(Paths.get(s"/tmp/picture/$filename"), replace = true)
            Ok("File uploaded")
          }.getOrElse {
            Redirect(routes.Application.index).flashing(
              "error" -> "Missing file")
          }
        }
        //#upload-file-action

        val request = FakeRequest().withBody(
          MultipartFormData(Map.empty, Seq(FilePart("picture", "formuploaded", None, TemporaryFile(tmpFile))), Nil)
        )
        testAction(upload, request)

        uploaded.delete()
        success
      }

      "upload file directly" in {
        val tmpFile = new File("/tmp/picture/tmpuploaded")
        writeFile(tmpFile, "hello")

        new File("/tmp/picture").mkdirs()
        val uploaded = new File("/tmp/picture/uploaded")
        uploaded.delete()

        val request = FakeRequest().withBody(TemporaryFile(tmpFile))
        testAction(new controllers.Application().upload, request)

        uploaded.delete()
        success
      }
    }

    def testAction[A](action: Action[A], request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
      running(GuiceApplicationBuilder().build()) {

        val result = action(request)

        status(result) must_== expectedResponse
      }
    }

    def writeFile(file: File, content: String) = {
      file.getParentFile.mkdirs()
      val out = new FileWriter(file)
      try {
        out.write(content)
      } finally {
        out.close()
      }
    }

  }
  package controllers {
    class Application extends Controller {

      //#upload-file-directly-action
        def upload = Action(parse.temporaryFile) { request =>
          request.body.moveTo(new File("/tmp/picture/uploaded"))
          Ok("File uploaded")
        }
        //#upload-file-directly-action

      def index = Action { request =>
        Ok("Upload failed")
      }

      //#upload-file-customparser
      type FilePartHandler[A] = FileInfo => Accumulator[ByteString, FilePart[A]]

      def handleFilePartAsFile: FilePartHandler[File] = {
        case FileInfo(partName, filename, contentType) =>
          val perms = java.util.EnumSet.of(OWNER_READ, OWNER_WRITE)
          val attr = PosixFilePermissions.asFileAttribute(perms)
          val path = Files.createTempFile("multipartBody", "tempFile", attr)
          val file = path.toFile
          val fileSink = FileIO.toFile(file)
          val accumulator = Accumulator(fileSink)
          accumulator.map { case IOResult(count, status) =>
            FilePart(partName, filename, contentType, file)
          }(play.api.libs.concurrent.Execution.defaultContext)
      }

      def uploadCustom = Action(parse.multipartFormData(handleFilePartAsFile)) { request =>
        val fileOption = request.body.file("name").map {
          case FilePart(key, filename, contentType, file) =>
            file.toPath
        }

        Ok(s"File uploaded: $fileOption")
      }
      //#upload-file-customparser

    }
  }
}

