package de.freeky.web.lib

import net.liftweb.http._
import S._
import js._
import JsCmds._
import JE._
import net.liftmodules.textile._
import net.liftweb.common._
import net.liftweb.util._
import Helpers._
import scala.xml._
import net.liftweb.json._
import net.liftweb.json.JsonParser._
import net.liftweb.json.Serialization.{ read, write }
import net.liftweb.common.Loggable

/**
 * Case classes to be used for the messages between the client and server, very easy to
 * serialize and parse
 */
case class AutoCompleteOptions(val label: String, val value: String)

/**
 * Simple query for a search in the snippet
 */
case class SearchTerm(val term: String)

object AjaxFactory {
  def ajaxLiveText(value: String, func: String => JsCmd, attrs: (String, String)*): Elem = {
    S.fmapFunc(S.SFuncHolder(func)) { funcName =>
      (attrs.foldLeft(<input type="text" value={ value }/>)(_ % _)) %
        ("onchange" -> SHtml.makeAjaxCall(JsRaw("'" + funcName +
          "=' + encodeURIComponent(this.value)")))
    }
  }
  
  /*
   * Using onblur event because onkeyup is not fast enough in real world environment
   */
  def ajaxLiveTextarea(value: String, func: String => JsCmd, attrs: (String, String)*): Elem = {
    S.fmapFunc(S.SFuncHolder(func)) { funcName =>
      (attrs.foldLeft(<textarea type="text">{ value }</textarea>)(_ % _)) %
        ("onblur" -> SHtml.makeAjaxCall(JsRaw("'" + funcName +
          "=' + encodeURIComponent(this.value)")))
    }
  }
  
  def jQAutocomplete(value: String, optionsFunc: String => List[String], selectFunc: String => JsCmd, attrs: (String, String)*): Elem = {

    def concreteOptionsFunc: PartialFunction[JsonAST.JValue, JsCmd] = {
      case st: JsonAST.JValue => st.extractOpt[SearchTerm] match {
        case Some(SearchTerm(term)) => findTerms(term)
        case None =>
      }
      case any =>
    }

    def findTerms(term: String): JsCmd = {
      val items = optionsFunc(term).map(s => AutoCompleteOptions(s, s))
      val ast = net.liftweb.json.Extraction.decompose(items)
      JsRaw(write(ast))
    }

    val context = AjaxContext.json(Some("""function(items) {
    							items = items.replace(/[\n;]/g, "");
    							response(JSON.parse(items));
							}"""))
    val (call: JsonCall, jsCmd: JsCmd) = S.createJsonFunc(concreteOptionsFunc)

    val functionDef = JsRaw("function " +
      call.funcId + "(obj, onSuccess, onError) {liftAjax.lift_ajaxHandler('" +
      call.funcId + "='+ encodeURIComponent(JSON.stringify(obj)), onSuccess, onError);}")

    <span>
      {
        List((attrs.foldLeft(<input name={ call.funcId } type="text" value={ value }/>)(_ % _)),
          Script(functionDef & JsRaw(
            """$(function() {
				$( "[name=""" + call.funcId + """]" ).autocomplete({
					source: function( request, response ) {
						""" + call.funcId + """({'term': request.term},
						function(items) {
    						items = items.replace(/[\n;]/g, "");
    						response(JSON.parse(items));
						},
						function() {
							items = [];
							response(items)
						});
					},
					minLength: 2,
					select: function( event, ui ) {
						""" + SHtml.ajaxCall(JsRaw("ui.item.value"), selectFunc)._2.toJsCmd + """
					},
					open: function() {
						$( this ).removeClass( "ui-corner-all" ).addClass( "ui-corner-top" );
					},
					close: function() {
						$( this ).removeClass( "ui-corner-top" ).addClass( "ui-corner-all" );
					}
				});
			});""")))
      }
    </span>
  }
  
  /**
   * Add the implicit formats for the de/serialization
   */
  implicit val formats = DefaultFormats
}