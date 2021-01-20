package irio.alertingplatform.utils

import org.slf4j.{Logger, LoggerFactory}

trait LoggingSupport {
  def logger: Logger = LoggerFactory.getLogger(getClass.getSimpleName)
}
