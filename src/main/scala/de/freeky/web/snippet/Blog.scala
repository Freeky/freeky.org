package de.freeky.web.snippet

import de.freeky.web.lib._

class Blog extends ArticleSnippet[de.freeky.web.model.Blog] {
	val baseUrl = "/blog"
	val articles = de.freeky.web.model.FreekyDB.blogs
	val factory = new de.freeky.web.model.BlogFactory
}