/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import scala.util.control.NonFatal

import javax.inject.{ Singleton, Inject, Provider }
import play.api._
import play.api.http.HttpErrorHandler
import play.api.libs.{ CryptoConfig, Crypto, CryptoConfigParser }
import play.core.Router
import play.utils.Reflect

class BuiltinModule extends Module {
  def bindings(env: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[Environment] to env,
      bind[GlobalSettings].toProvider[GlobalSettingsProvider],
      bind[InitialConfiguration] to InitialConfiguration(configuration),
      bind[Configuration].toProvider[ConfigurationProvider],

      // Application lifecycle, bound both to the interface, and its implementation, so that Application can access it
      // to shut it down.
      bind[DefaultApplicationLifecycle].toSelf,
      bind[ApplicationLifecycle].to(bind[DefaultApplicationLifecycle]),

      bind[Application].to[DefaultApplication],
      bind[play.inject.Injector].to[play.inject.DelegateInjector],
      // bind Plugins - eager

      bind[Router.Routes].toProvider[RoutesProvider],
      bind[Plugins].toProvider[PluginsProvider],

      bind[CryptoConfig].toProvider[CryptoConfigParser],
      bind[Crypto].toSelf
    ) ++ HttpErrorHandler.bindingsFromConfiguration(env, configuration)
  }
}

case class InitialConfiguration(configuration: Configuration)

@Singleton
class ConfigurationProvider @Inject() (initialConfiguration: InitialConfiguration, env: Environment, global: GlobalSettings) extends Provider[Configuration] {
  lazy val conf = initialConfiguration.configuration

  private def loadClass(key: String, defaultClass: String): Option[Class[_]] = {
    conf.getString(key) map { className =>
      try env.classLoader.loadClass(className) catch {
        case NonFatal(e) =>
          throw new PlayException(s"Cannot load $key", s"$key [$className] was not loaded.", e)
      }
    } orElse {
      try Some(env.classLoader.loadClass(defaultClass)) catch {
        case e: ClassNotFoundException => None
        case NonFatal(e) =>
          throw new PlayException(s"Cannot load class $defaultClass", s"$defaultClass was not loaded", e)
      }
    }
  }

  lazy val configFilter = loadClass("play.application.configFilter", "ConfigFilter")
    .map(Reflect.createInstance[ApplicationConfigFilter])
    .getOrElse(new DefaultConfigFilter(global))

  lazy val get = configFilter(env, conf)
}

@Singleton
class GlobalSettingsProvider @Inject() (environment: Environment, initialConfiguration: InitialConfiguration) extends Provider[GlobalSettings] {
  lazy val get = GlobalSettings(initialConfiguration.configuration, environment)
}

@Singleton
class RoutesProvider @Inject() (injector: Injector, environment: Environment, configuration: Configuration) extends Provider[Router.Routes] {
  lazy val get = {
    val prefix = configuration.getString("application.context").map { prefix =>
      if (!prefix.startsWith("/")) {
        throw configuration.reportError("application.context", "Invalid application context")
      }
      prefix
    }

    val router = Router.load(environment, configuration)
      .fold[Router.Routes](Router.Null)(injector.instanceOf(_))
    prefix.fold(router)(router.withPrefix)
  }
}

@Singleton
class PluginsProvider @Inject() (environment: Environment, injector: Injector) extends Provider[Plugins] {
  lazy val get = Plugins(environment, injector)
}
