/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
//#relative-controller
//###replace: package controllers
package scalaguide.http.routing.reverse.controllers

import javax.inject._
import play.api.mvc._

@Singleton
class Relative @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def helloview() = Action { implicit request =>
    Ok(views.html.hello("Bob"))
  }

  def hello(name: String) = Action {
    Ok(s"Hello $name!")
  }
}
