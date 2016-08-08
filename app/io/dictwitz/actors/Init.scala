package io.dictwitz.actors

import play.api.libs.iteratee.Concurrent
import play.api.libs.json.JsValue

case class Init(channel: Concurrent.Channel[JsValue])
