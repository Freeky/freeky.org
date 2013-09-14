package de.freeky.web.snippet

import net.liftweb.http._
import net.liftweb.util._
import scala.xml._
import Helpers._
import org.squeryl.PrimitiveTypeMode._
import de.freeky.web._
import de.freeky.web.model._
import de.freeky.web.lib._
import net.liftweb.common._
import java.sql.Timestamp
import net.liftmodules.textile.TextileParser
import de.freeky.web.lib.AjaxFactory._
import net.liftweb.http.js.JE._
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.{ JsCmd, JsObj }

object Forum {
  def hasReadAccess(forumId: Long): Boolean = {
    User.rights.rAdministrateForums ||
      transaction {
        FreekyDB.forums.lookup(forumId).map(forum => {
          if (forum.readAccess.size < 1) true
          else forum.readAccess.exists(_.id == User.rights.id)
        }).getOrElse(false)
      }
  }

  def hasWriteAccess(forumId: Long): Boolean = {
    User.rights.rAdministrateForums || {
      if (User.loggedIn_?()) transaction {
        FreekyDB.forums.lookup(forumId).map(forum => {
          if (forum.writeAccess.size < 1) true
          else forum.writeAccess.exists(_.id == User.rights.id)
        }).getOrElse(false)
      }
      else false
    }
  }

  def isAllowedToEditPost(post: ForumPost): Boolean = {
    transaction {
      User.rights.rAdministrateForums ||
        post.forum.headOption.map(_.moderators.exists(_.id == loggedInUser.map(_.id).openOr(0L))).getOrElse(false) ||
        (post.topic.headOption.map(_.closed.isEmpty).getOrElse(true) && post.userid == loggedInUser.map(_.id).openOr(0L))
    }
  }

  def hasAdminRights(forumId: Long): Boolean = {
    transaction {
      User.rights.rAdministrateForums ||
        FreekyDB.forums.lookup(forumId).map(_.moderators.exists(_.id == loggedInUser.map(_.id).openOr(0L))).getOrElse(false)
    }
  }
}

class Forum extends StatefulSnippet {
  val pagesize = 15

  def dispatch = _ match {
    case "short" => short
    case "overview" => overview
    case "viewtopic" => viewTopic
    case "administrationoverview" => administrationOverview
    case "editforum" => editForum
    case "trace" => trace
  }

  def short = {
    if (User.rights.rAdministrateForums) {
      ".forums" #> inTransaction { from(FreekyDB.forums)(f => compute(count)).toLong } &
        ".topics" #> inTransaction { from(FreekyDB.forumTopics)(ft => compute(count)).toLong } &
        ".posts" #> inTransaction { from(FreekyDB.forumPosts)(fp => compute(count)).toLong }
    } else
      "*" #> ""
  }

  def editForum(in: NodeSeq): NodeSeq = {

    val moderatorTemplate = (".moderatorentry ^^" #> ((n: NodeSeq) => n)).apply(in)
    val readAccessTemplate = (".readaccessentry ^^" #> ((n: NodeSeq) => n)).apply(in)
    val writeAccessTemplate = (".writeaccessentry ^^" #> ((n: NodeSeq) => n)).apply(in)

    val forumId = S.param("forumid").map(_.toLong).openOr(0L)
    var forum = transaction {
      (
        FreekyDB.forums.lookup(forumId).getOrElse(new model.Forum("New")))
    }

    def buildParentValues(): List[(String, String)] = {
      ("a", "") :: transaction { FreekyDB.forums.map(f => (f.id.toString, f.name)).toList }
    }

    def buildAccountTypeValues(): List[(String, String)] = {
      ("a", "") :: transaction { FreekyDB.accountTypes.map(at => (at.id.toString, at.name)).toList }
    }

    def proccessSave() = {
      transaction {
        if (forumId == 0)
          forum = FreekyDB.forums.insert(forum)
        else
          FreekyDB.forums.update(forum)
      }
    }

    def buildAutoCompleteValues(current: String): List[String] = {
      transaction {
        from(FreekyDB.users)(u => where(u.name like "%s%%".format(current)) select (u.name)).toList
      }
    }

    def selectModerator(current: String): JsCmd = {
      transaction {
        try { FreekyDB.users.where(_.name like current).map(forum.moderators.associate(_)) }
        catch {
          case _: Throwable => S.error("Adding Moderator %s failed!".format(current))
        }
      }
      updateModeratorEntrys
    }

    def selectReadAccess(current: String): JsCmd = {
      transaction {
        try { FreekyDB.accountTypes.where(_.id === current.toLong).map(forum.readAccess.associate(_)) }
        catch {
          case _: Throwable => S.error("Adding ReadAccess failed!".format(current))
        }
      }
      updateReadAccessEntrys
    }

    def selectWriteAccess(current: String): JsCmd = {
      transaction {
        try { FreekyDB.accountTypes.where(_.id === current.toLong).map(forum.writeAccess.associate(_)) }
        catch {
          case _: Throwable => S.error("Adding WriteAccess failed!".format(current))
        }
      }
      updateWriteAccessEntrys
    }

    def deleteModerator(u: User): JsCmd = {
      transaction {
        forum.moderators.dissociate(u)
      }
      updateModeratorEntrys
    }

    def deleteReadAccess(at: AccountType): JsCmd = {
      transaction {
        forum.readAccess.dissociate(at)
      }
      updateReadAccessEntrys
    }

    def deleteWriteAccess(at: AccountType): JsCmd = {
      transaction {
        forum.writeAccess.dissociate(at)
      }
      updateWriteAccessEntrys
    }

    def updateModeratorEntrys: JsCmd = {
      SetHtml("moderatorlist", moderatorEntrys)
    }

    def updateReadAccessEntrys: JsCmd = {
      SetHtml("readaccesslist", readAccessEntrys)
    }

    def updateWriteAccessEntrys: JsCmd = {
      SetHtml("writeaccesslist", writeAccessEntrys)
    }

    def moderatorEntrys: NodeSeq = {
      transaction {
        forum.moderators.flatMap(u =>
          {
            ".moderatorname" #> u.name &
              ".removemoderator" #> SHtml.a(() => deleteModerator(u), Text("X"))
          }.apply(moderatorTemplate)).toList
      }
    }

    def readAccessEntrys: NodeSeq = {
      transaction {
        forum.readAccess.flatMap(at =>
          {
            ".readaccessname" #> at.name &
              ".removereadaccess" #> SHtml.a(() => deleteReadAccess(at), Text("X"))
          }.apply(readAccessTemplate)).toList
      }
    }

    def writeAccessEntrys: NodeSeq = {
      transaction {
        forum.writeAccess.flatMap(at =>
          {
            ".writeaccessname" #> at.name &
              ".removewriteaccess" #> SHtml.a(() => deleteWriteAccess(at), Text("X"))
          }.apply(writeAccessTemplate)).toList
      }
    }

    def deleteForum() = {
      transaction {
        FreekyDB.forums.delete(forum.id)
      }
    }

    {
      ".name" #> SHtml.text(forum.name, forum.name = _) &
        ".description" #> SHtml.textarea(forum.description.getOrElse(""), s => forum.description = if (s.length() > 0) Some(s) else None) &
        ".parent" #> SHtml.select(buildParentValues, forum.parentid.map(_.toString), s => forum.parentid = tryo { s.toLong }) &
        ".order" #> SHtml.text(forum.ordering.toString, s => forum.ordering = s.toInt) &
        ".submit" #> SHtml.button(S ? "save", proccessSave) &
        ".newmoderatorname" #> jQAutocomplete("", buildAutoCompleteValues, selectModerator) &
        ".moderatorentry" #> moderatorEntrys &
        ".newreadaccess" #> SHtml.ajaxSelect(buildAccountTypeValues, Empty, selectReadAccess) &
        ".readaccessentry" #> readAccessEntrys &
        ".newwriteaccess" #> SHtml.ajaxSelect(buildAccountTypeValues, Empty, selectWriteAccess) &
        ".writeaccessentry" #> writeAccessEntrys &
        ".delete" #> SHtml.submit(S ? "delete", deleteForum, "onclick" ->
          JsIf(JsRaw("confirm('Are you sure?')"),
            JsReturn(true), JsReturn(false)).toJsCmd)
    }.apply(in)

  }

  def administrationOverview(in: NodeSeq): NodeSeq = {
    var entrycount = 25
    var page = 0
    var nameFilter: Option[String] = None

    val forumEntryTemplate = (".entry ^^" #> ((n: NodeSeq) => n)).apply(in)

    def max = transaction { maxquery.toLong };

    def maxquery = from(FreekyDB.forums)(f => where(f.name like nameFilter.map("%%%s%%".format(_)).?) compute (count))
    def query = from(FreekyDB.forums)(f => where(f.name like nameFilter.map("%%%s%%".format(_)).?) select (f) orderBy (f.id))

    def buildForumsTable(entries: List[model.Forum]) = {
      entries.flatMap({ entry =>
        (".id" #> entry.id &
          ".name" #> entry.name &
          ".parent" #> entry.parent.map(_.name).mkString &
          ".order" #> entry.ordering &
          ".editlink [href]" #> "/administration/forums/%d".format(entry.id)).apply(forumEntryTemplate)
      })
    }

    def forumTable() = {
      transaction {
        val entries = query.page(page * entrycount, entrycount).toList
        buildForumsTable(entries)
      }
    }

    def updateForumsTable(): JsCmd = {
      List(SetHtml("forum_table", forumTable),
        SetHtml("current_page", Text("%d/%d".format(page + 1, (max / entrycount) + 1))))
    }

    def updateEntryCount(e: String) = {
      entrycount = Integer.parseInt(e)
      page = 0
      updateForumsTable
    }

    def prevPage = {
      if (page > 0) page = page - 1
      updateForumsTable
    }

    def nextPage = {
      if (((page + 1) * entrycount) < max) page = page + 1
      updateForumsTable
    }

    def updateNameFilter(n: String) = {
      nameFilter = if (n.length() > 0) Some(n) else None
      updateForumsTable
    }

    (".entrycount" #> SHtml.ajaxSelect(List(10, 25, 50, 100).map(i => (i.toString, i.toString)),
      Full(25.toString), v => updateEntryCount(v)) &
      ".page" #> Text("%d/%d".format(page + 1, (max / entrycount) + 1)) &
      ".prevpage" #> SHtml.ajaxButton(Text(S ? "previous"), () => prevPage) &
      ".nextpage" #> SHtml.ajaxButton(Text(S ? "next"), () => nextPage) &
      ".namefilter" #> ajaxLiveText(nameFilter.getOrElse(""), updateNameFilter(_)) &
      ".entry" #> forumTable).apply(in)
  }

  def overview = {
    var forumId = S.param("forumid").map(_.toLong)

    forumId.map(id => if (!Forum.hasReadAccess(id)) {
      S.error(S ? "no.permission")
      S.redirectTo("/forum")
    })

    "#forumlist" #> buildForumView(forumId) &
      "#topiclist" #> buildTopicView(forumId) &
      ".forumname *" #> transaction {
        FreekyDB.forums.lookup(forumId.getOrElse(0L)).map(_.name).getOrElse("Forum")
      } &
      forumId.map(id => if (Forum.hasWriteAccess(id)) { ".newtopic [href]" #> "/topic/new/%d".format(id) }
      else ".newtopic" #> "").getOrElse(".newtopic" #> "") &
      {
        if (User.rights.rAdministrateForums)
          ".editforum [href]" #> "/administration/forums/%d".format(forumId.openOr(0L))
        else
          ".editforum" #> ""
      }
  }

  def buildForumView(forumId: Option[Long]) = {
    val forums = transaction {
      from(FreekyDB.forums)(f => where(f.parentid === forumId) select (f) orderBy (f.ordering)).filter(f => Forum.hasReadAccess(f.id))
    }

    if (forums.size > 0) {
      ".forumentry" #> forums.map(forum =>
        ".forumname" #> forum.mkLink &
          ".forumdescription" #> forum.description &
          ".forumsubforums" #> buildSubForums(Some(forum.id)) &
          ".forummoderators" #> buildModerators(forum.id) &
          ".topiccount" #> buildTopicCount(forum.id) &
          ".postcount" #> buildPostCount(forum.id) &
          ".lastpost" #> buildLastPost(forum))
    } else {
      "*" #> ""
    }
  }

  def buildSubForums(forumid: Option[Long]) = {
    val subforums = inTransaction {
      from(FreekyDB.forums)(f => where(f.parentid === forumid) select (f)).map(_.mkLink)
    }

    if (subforums.size > 0)
      ".subforums" #> subforums
    else
      "*" #> ""
  }

  def buildModerators(forumid: Long) = {
    "*" #> "" // TODO: Mopderator ansicht implementieren
  }

  def buildTopicCount(forumid: Long) = {
    inTransaction {
      from(FreekyDB.forumTopics)(ft => where(ft.forumid === forumid) select (ft)).size
    }
  }

  def buildPostCount(forumid: Long) = {
    inTransaction {
      from(FreekyDB.forumPosts)(fp => where(fp.forumid === forumid) compute (count)).toLong
    }
  }

  def buildLastPost(forum: de.freeky.web.model.Forum) = {
    val lastPost = inTransaction { forum.lastPost.headOption }

    lastPost match {
      case Some(post) => {
        val userName = inTransaction { post.user.map(_.name) }
        ".lastpostname" #> post.subject &
          ".lastpostuser" #> userName &
          ".lastposttime" #> Formater.format(post.time) &
          ".lastpostlink [href]" #> post.linkAddress
      }
      case _ => "*" #> "No Posts"
    }
  }

  def buildTopicView(forumId: Option[Long]) = {
    val topics = inTransaction {
      from(FreekyDB.forumTopics)(ft => where(ft.forumid === forumId) select (ft) orderBy (ft.isSticky desc, ft.time desc)).toList
    }

    if (topics.size > 0) {
      ".topicentry" #> topics.map(topic => {
        ".topicname" #> <span>{ if (topic.isSticky) Text("Sticky: ") }{ ForumTopic.mkLink(topic) }</span> &
          ".topicauthor" #> inTransaction { topic.user.map(_.name) } &
          ".topictime" #> Formater.format(topic.time) &
          ".topicreplies" #> topic.replies &
          ".topicviews" #> topic.views &
          ".lastpost" #> buildLastPost(topic) &
          ".topicpages" #> {
            val postCount = inTransaction { topic.posts.Count.toInt }
            buildPagination((postCount / pagesize) + 1, postCount, topic.id, true)
          }
      })
    } else {
      "*" #> ""
    }
  }

  def buildLastPost(topic: ForumTopic) = {
    inTransaction { topic.posts.lastOption } match {
      case Some(post) => {
        ".lastpostuser" #> inTransaction { post.user.map(_.name) } &
          ".lastposttime" #> Formater.format(post.time) &
          ".lastpostlink [href]" #> post.linkAddress
      }
      case _ => "*" #> "No Posts"
    }
  }

  def viewTopic = {
    var topicId = S.param("topicid").map(_.toLong).openOr(0L)
    var postId = S.param("postid").map(_.toLong)
    var page = S.param("page").map(_.toInt).openOr(1)

    if (topicId < 1 && postId.map(_ < 1).openOr(true)) S.redirectTo("/forum")

    transaction {

      //TODO: PostId #-Link
      postId.map(pid => {
        topicId = FreekyDB.forumPosts.lookup(pid).map(_.topicid).getOrElse(topicId)
        page = (from(FreekyDB.forumPosts)(fp => where(fp.topicid === topicId and (fp.id lte pid)) compute (count - 1)).toInt / pagesize) + 1
      })

      FreekyDB.forumTopics.lookup(topicId).map(topic => {

        if (!Forum.hasReadAccess(topic.forumid)) {
          S.error(S ? "no.permission")
          S.redirectTo("/forum")
        }

        topic.views = topic.views + 1
        FreekyDB.forumTopics.update(topic)
        val topicCount = topic.posts.Count.toInt

        "#topictitle" #> ForumTopic.mkLink(topic) &
          ".postentry" #> inTransaction { topic.posts.page((page - 1) * pagesize, pagesize).map(buildPost(_, postId)) } &
          ".topicpages" #> buildPagination(page, topicCount, topicId) &
          { if (topic.closed.isEmpty && Forum.hasWriteAccess(topic.forumid)) ".answear [href]" #> topic.answearAddress else ".answear" #> "" }
      })
        .getOrElse("*" #> (S ? "topic.does.not.exist"))
    }
  }

  // somebody spoke there will be voodoo and there was voodoo ...
  def buildPagination(curPage: Int, postCount: Int, topicId: Long, showOnePage: Boolean = false) = {
    val maxpage = (postCount / pagesize) + 1
    var pages = { 1 :: maxpage :: ((curPage - 2) to (curPage + 2)).toList }.sortWith(_ < _).distinct.filter(n => !(n < 1 || n > maxpage))
      .foldLeft(List[Int]())((list, number) => {
        if (list.size < 1)
          list ::: number :: Nil
        else if (list.last < number - 1)
          list ::: 0 :: number :: Nil
        else
          list ::: number :: Nil
      })

    if (pages.size > 1 || showOnePage) { n: NodeSeq =>
      {
        {
          ".pagelink" #> pages.map(page =>
            if (page == 0) "*" #> "..."
            else {
              ".pagelink [href]" #> "/topic/%d/page/%d".format(topicId, page) &
                ".pagenumber" #> page
            })
        }.apply(n)
      }
    } else { n: NodeSeq => Text("") }
  }

  def buildPost(post: ForumPost, selectedPost: Option[Long] = None) = {
    ".postentry [id]" #> post.linkMarker &
      ".postentry [class+]" #> { if (selectedPost.map(id => post.id == id).getOrElse(false)) "selectedpost" else "" } &
      ".postsubject" #> ForumPost.mkLink(post) &
      ".postuser" #> inTransaction { post.user.map(_.name) } &
      ".posttime" #> Formater.format(post.time) &
      ".posttext" #> { if (post.textile) TextileParser.paraFixer(TextileParser.toHtml(post.text)) else Text(post.text) } &
      { if (Forum.isAllowedToEditPost(post)) ".postedit [href]" #> post.editLink else ".postedit" #> "" }
  }

  def trace(in: NodeSeq): NodeSeq = {
    var trace: NodeSeq = NodeSeq.Empty
    var curForum: Option[model.Forum] = Empty
    val spacer: Text = Text(" » ")

    // Check Post ID
    S.param("postid").map(_.toLong).map(pid => {
      curForum = transaction {
        from(FreekyDB.forums, FreekyDB.forumPosts)((f, fp) =>
          where(fp.id === pid and f.id === fp.forumid) select (f)).headOption
      }
    })

    // Check Topic ID
    if (curForum.isEmpty) {
      S.param("topicid").map(_.toLong).map(tid => {
        curForum = transaction {
          from(FreekyDB.forums, FreekyDB.forumTopics)((f, ft) =>
            where(ft.id === tid and f.id === ft.forumid) select (f)).headOption
        }
      })
    }

    // Check Forum ID
    if (curForum.isEmpty) {
      S.param("forumid").map(_.toLong).map(fid => {
        curForum = transaction {
          FreekyDB.forums.lookup(fid)
        }
      })
    }

    while (curForum.isDefined)
      curForum.map(forum => {
        if (trace.length > 0)
          trace = forum.mkLink ++ spacer ++ trace
        else
          trace = forum.mkLink

        curForum = transaction {
          from(FreekyDB.forums, FreekyDB.forums)((f1, f2) =>
            where(f1.id === f2.parentid and f2.id === forum.id) select (f1)).headOption
        }
      })

    if (trace.length > 0)
      <a href="/forum">Forum</a> ++ spacer ++ trace
    else
      <a href="/forum">Forum</a>
  }

}

class Posting extends StatefulSnippet {
  def dispatch = _ match {
    case "render" => render
  }

  var forum: model.Forum = new model.Forum()
  var topic: model.ForumTopic = new model.ForumTopic()
  var post: model.ForumPost = new model.ForumPost()

  def render = {

    // Setup
    if (!(S.param("forumid").map(_.toLong).map(setupNewTopic(_)).openOr(false) ||
      S.param("topicid").map(_.toLong).map(setupTopicReply(_)).openOr(false) ||
      S.param("postid").map(_.toLong).map(setupEditPost(_)).openOr(false))) {
      S.error(S ? "no.reference.was.set")
      S.redirectTo("/forum")
    }

    // Security Checks
    if (forum.id == 0) {
      S.error(S ? "forum.does.not.exist")
      S.redirectTo("/forum")
    }

    if (!Forum.hasWriteAccess(forum.id)) {
      S.error(S ? "no.permission")
      S.redirectTo("/forum")
    }

    if (!Forum.isAllowedToEditPost(post)) {
      S.error(S ? "no.permission")
      S.redirectTo("/forum")
    }

    // Save posting Actions
    def save(): Unit = {
      if (post.subject.isEmpty()) {
        S.error(S ? "title.is.empty")
        return
      }
      if (post.text.isEmpty) {
        S.error(S ? "text.is.empty")
        return
      }

      if (S.param("forumid").isDefined) saveNewTopic
      if (S.param("topicid").isDefined) saveTopicReply
      if (S.param("postid").isDefined) saveEditPost

      S.redirectTo(post.linkAddress)
    }

    ".topictitle" #> SHtml.text(post.subject, post.subject = _) &
      ".posttext" #> SHtml.ajaxTextarea(post.text, post.text = _) &
      ".showpreview" #> SHtml.ajaxButton(S ? "show.preview",
        () => SetHtml("previewarea", {
          if (post.textile) TextileParser.paraFixer(TextileParser.toHtml(post.text))
          else Text(post.text)
        })) &
        ".submit" #> SHtml.ajaxSubmit(S ? "submit", save) &
        "#previewarea *" #> "" &
        ".enabletextile" #> SHtml.ajaxCheckbox(post.textile, post.textile = _) &
        ".adminoptions" #> {
          if (Forum.hasAdminRights(forum.id)) {
            ".closed" #> SHtml.ajaxCheckbox(topic.closed.isDefined, b => {
              if (b) topic.closed = Some(new Timestamp(millis)) else topic.closed = None
              transaction { FreekyDB.forumTopics.update(topic) }
            }) &
              ".issticky" #> SHtml.ajaxCheckbox(topic.isSticky, b => {
                topic.isSticky = b
                transaction { FreekyDB.forumTopics.update(topic) }
              })
          } else
            "*" #> ""
        }

  }

  def setupNewTopic(forumid: Long): Boolean = {
    forum = transaction {
      FreekyDB.forums.lookup(forumid).getOrElse(new model.Forum)
    }
    topic = new ForumTopic(forum.id, "", loggedInUser.map(_.id).openOr(0L))
    post = new ForumPost(0L, forum.id, loggedInUser.map(_.id).openOr(0L), "", "")
    true
  }

  def setupTopicReply(topicid: Long): Boolean = {
    transaction {
      topic = FreekyDB.forumTopics.lookup(topicid).getOrElse(new model.ForumTopic)
      forum = topic.forum.head
    }
    post = new ForumPost(topic.id, forum.id, loggedInUser.map(_.id).openOr(0L), "", "")

    post.subject = if (S.param("topicid").isDefined) "Re: %s".format(topic.title) else topic.title

    true
  }

  def setupEditPost(postid: Long): Boolean = {
    transaction {
      post = FreekyDB.forumPosts.lookup(postid).getOrElse(new model.ForumPost)
      forum = post.forum.head
      topic = post.topic.head
    }
    true
  }

  def saveNewTopic() {
    topic.title = post.subject

    transaction {
      topic = FreekyDB.forumTopics.insert(topic)
      post.topicid = topic.id
      post = FreekyDB.forumPosts.insert(post)
      forum.last_post_id = Some(post.id)
      FreekyDB.forums.update(forum)
    }
  }

  def saveTopicReply() {
    transaction {
      topic.replies += 1
      FreekyDB.forumTopics.update(topic)
      post = FreekyDB.forumPosts.insert(post)
      forum.last_post_id = Some(post.id)
      FreekyDB.forums.update(forum)
    }
  }

  def saveEditPost() {
    transaction {
      post.edit_count = post.edit_count.map(_ + 1).orElse(Some(1))
      post.edit_time = Some(new Timestamp(millis))
      loggedInUser.map(u => post.edit_user_id = Some(u.id))

      FreekyDB.forumPosts.update(post)
    }
  }

}