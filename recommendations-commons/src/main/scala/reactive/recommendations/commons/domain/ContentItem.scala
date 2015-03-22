package reactive.recommendations.commons.domain

import scala.collection

/**
 * Created by denik on 14.03.2015.
 */
case class ContentItem(id: String,
                       createdTs: Option[String] = None,
                       tags: Option[Set[String]] = None,
                       categories: Option[Set[String]] = None,
                       terms: Option[Set[String]] = None,
                       author: Option[String] = None,
                       l10n: Option[Set[String]] = None)