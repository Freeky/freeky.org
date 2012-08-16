package de.freeky.web.snippet

import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.util._
import net.liftweb.textile._
import scala.xml._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web.model._
import de.freeky.web.lib.AjaxFactory._
import net.liftweb.http.js.JsCmds
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JE.Call

class Project extends DispatchSnippet with Logger {
  def dispatch() = _ match {
    case "show" => show
    case "edit" => edit
    case "new" => create
  }

  def show = {
    transaction {
      S.param("project") match {
        case Full(project) => {
          from(FreekyDB.projects)(p => where(p.name === project) select (p)).headOption match {
            case Some(project) => "*" #> TextileParser.toHtml(project.text)
            case _ => "*" #> <span>{ S ? "project.not.found" }</span>
          }
        }
        case _ => {

          "*" #> 
          <div>
            <h1>Projects</h1>
            {
              from(FreekyDB.projects)(p => select(p)).map(project => {
                <div>
                  <h2><a href={ "/projects/%s".format(project.name) }>{ project.name }</a></h2>
                  <span>{ project.description }</span><br/><hr/>
                </div>
              })
            }
          </div>
        }
      }
    }
  }

  def edit = {
    transaction {
      S.param("id") match {
        case Full(id) if id.matches("\\d+") => { // Check if Number
          from(FreekyDB.projects)(p => where(p.id === id.toLong) select (p)).headOption match {
            case Some(project) => {

              def processEdit() = {
                transaction {
                  if (project.name.length() > 3)
                    FreekyDB.projects.update(project)
                  else
                    S.error(S ? "projectname.too.short")
                }
              }

              def processDelete() = {
                transaction {
                  FreekyDB.projects.deleteWhere(p => p.id === project.id)
                }
              }

              def preview = {
                TextileParser.toHtml(project.text)
              }

              def updatePreview(text: String): JsCmd = {
                project.text = text
                JsCmds.SetHtml("previewarea", preview)
              }

              ".title" #> SHtml.text(project.name, project.name = _) &
                ".description" #> SHtml.textarea(project.description, project.description = _) &
                ".text" #> ajaxLiveTextarea(project.text, updatePreview _) &
                ".submit" #> SHtml.submit(S ? "submit", processEdit) &
                ".delete" #> SHtml.submit(S ? "delete", processDelete) &
                "#previewarea *" #> preview
            }

            case _ => "*" #> (S ? "project.not.found")
          }
        }
        case _ => {
          "*" #> <div>
            {
              from(FreekyDB.projects)(p => select(p)).map(project => {
                <a href={ "/edit/project/%d".format(project.id) }>{ project.name }</a><br/>
              })
            }
          </div>
        }
      }
    }
  }

  def create = {

    val project = new de.freeky.web.model.Project()

    def processEdit() = {
      transaction {
        if (project.name.length() > 3)
          FreekyDB.projects.insert(project)
        else
          S.error(S ? "projectname.too.short")
      }
    }

    def preview = {
      TextileParser.toHtml(project.text)
    }

    def updatePreview(text: String): JsCmd = {
      project.text = text
      JsCmds.SetHtml("previewarea", preview)
    }

    ".title" #> SHtml.text(project.name, project.name = _) &
      ".description" #> SHtml.textarea(project.description, project.description = _) &
      ".text" #> ajaxLiveTextarea(project.text, updatePreview _) &
      ".submit" #> SHtml.submit(S ? "submit", processEdit) &
      "#previewarea *" #> preview
  }
}