package io.bookwitz.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * @author Joseph Dessens
 * @since 2014-01-27
 * @todo make less verbose possibly with tuples etc
 */
case class Navigation(page: String, menus: Seq[NavigationMenu]) {
  implicit val navigationItemReads = Json.reads[NavigationItem]
  implicit val navigationItemWrites: Writes[NavigationItem] = (
    (__ \ "display").write[String] and
      (__ \ "route").write[String] and
      (__ \ "css").write[String]
    )(unlift(NavigationItem.unapply))

  //implicit val navigationMenuReads = Json.reads[NavigationMenu]
  implicit val navigationMenuWrites: Writes[NavigationMenu] = (
    (__ \ "items").lazyWrite(Writes.traversableWrites[NavigationItem](navigationItemWrites)) and
      (__ \ "position").write[String] and
      (__ \ "dropDown").write[Boolean]
    )(unlift(NavigationMenu.unapply))

  //implicit val navigationReads = Json.reads[Navigation]
  implicit val navigationWrites: Writes[Navigation] = (
    (__ \ "page").write[String] and
      (__ \ "menus").lazyWrite(Writes.traversableWrites[NavigationMenu](navigationMenuWrites))
    )(unlift(Navigation.unapply))

  def json: String = {
    Json.stringify(Json.toJson(this))
  }
}
case class NavigationMenu(items: Seq[NavigationItem], position: String, dropDown: Boolean = false)
case class NavigationItem(display: String, route: String, css: String = "")