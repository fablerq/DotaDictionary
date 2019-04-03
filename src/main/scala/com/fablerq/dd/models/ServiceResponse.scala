package com.fablerq.dd.models

case class ServiceResponse(bool: Boolean, message: String)

case class MainServiceResponse(
  isValid: Boolean,
  responseType: Option[String] = None,
  typeId: Option[String] = None
)
