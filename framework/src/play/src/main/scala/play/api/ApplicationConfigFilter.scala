package play.api

import javax.inject.Inject

/**
 * A filter that modifies the initial configuration after it is loaded
 *
 * Configure this by setting `play.application.configFilter`.
 */
trait ApplicationConfigFilter {
  def apply(env: Environment, conf: Configuration): Configuration
}

/**
 * Default configuration filter, which calls `global.onLoadConfig`
 */
class DefaultConfigFilter @Inject() (global: GlobalSettings) extends ApplicationConfigFilter {
  def apply(env: Environment, conf: Configuration) = {
    global.onLoadConfig(conf, env.rootPath, env.classLoader, env.mode)
  }
}
