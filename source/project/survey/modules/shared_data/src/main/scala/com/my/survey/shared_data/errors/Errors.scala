package com.my.survey.shared_data.errors

import cats.syntax.validated._
import org.tessellation.currency.dataApplication.DataApplicationValidationError
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr

object Errors {
  type DataApplicationValidationType = DataApplicationValidationErrorOr[Unit]

  val valid: DataApplicationValidationType = ().validNec

  implicit class DataApplicationValidationTypeOps[E <: DataApplicationValidationError](err: E) {
    def invalid: DataApplicationValidationType = err.invalidNec[Unit]

    def unlessA(cond: Boolean): DataApplicationValidationType = if (cond) valid else invalid

    def whenA(cond: Boolean): DataApplicationValidationType = if (cond) invalid else valid
  }

  case object SurveyNotExists extends DataApplicationValidationError {
    val message = "Survey does not exist"
  }

  case object SurveyClosed extends DataApplicationValidationError {
    val message = "Survey is closed"
  }

  case object MaxResponsesReached extends DataApplicationValidationError {
    val message = "Maximum number of responses reached for this survey"
  }

  case object InvalidAddress extends DataApplicationValidationError {
    val message = "Provided address different than proof"
  }

  case class InvalidFieldSize(fieldName: String, maxSize: Long) extends DataApplicationValidationError {
    val message = s"Invalid field size: $fieldName, maxSize: $maxSize"
  }

  case object InvalidAccessKey extends DataApplicationValidationError {
    val message = "Invalid access key for the survey"
  }

  case object TitleAlreadyExists extends DataApplicationValidationError {
    val message = "A survey with this title already exists"
  }
}