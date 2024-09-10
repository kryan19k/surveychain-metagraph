package com.my.survey.shared_data.validations

import cats.syntax.all._
import cats.syntax.option.catsSyntaxOptionId
import com.my.survey.shared_data.errors.Errors._
import com.my.survey.shared_data.types.Types._
import com.my.survey.shared_data.validations.TypeValidators._
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.schema.address.Address

object Validations {
  def createSurveyValidations(
    update: CreateSurvey,
    maybeState: Option[DataState[SurveyUpdatesState, SurveyCalculatedState]]
  ): DataApplicationValidationErrorOr[Unit] = {
    validateStringMaxSize(update.title, 256, "title")
      .productR(validateStringMaxSize(update.description, 1000, "description"))
      .productR(validateListMaxSize(update.questions, 50, "questions"))
      .productR(validateStringMaxSize(update.tokenReward, 64, "tokenReward"))
      .productR(validateListMaxSize(update.tags, 10, "tags"))
      .productR(maybeState.fold(valid)(state => validateUniqueTitle(update.title, state)))
  }

  private def validateUniqueTitle(
    title: String,
    state: DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataApplicationValidationErrorOr[Unit] =
    TitleAlreadyExists.whenA(state.calculated.surveys.values.exists(_.title == title))

  def submitResponseValidations(
    update    : SubmitResponse,
    maybeState: Option[DataState[SurveyUpdatesState, SurveyCalculatedState]]
  ): DataApplicationValidationErrorOr[Unit] =
    maybeState match {
      case Some(state) =>
        validateIfSurveyExists(update.surveyId, state)
          .productR(validateIfSurveyIsOpen(update.surveyId, state))
          .productR(validateIfMaxResponsesNotReached(update.surveyId, state))
      case None => valid
    }

  def submitResponseValidationsWithSignature(
    update   : SubmitResponse,
    addresses: List[Address],
    state    : DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataApplicationValidationErrorOr[Unit] =
    submitResponseValidations(update, state.some)
      .productR(validateProvidedAddress(addresses, update.respondent))

  def decryptResponsesValidations(
    update: DecryptResponses,
    maybeState: Option[DataState[SurveyUpdatesState, SurveyCalculatedState]]
  ): DataApplicationValidationErrorOr[Unit] =
    maybeState.fold(valid) { state =>
      validateIfSurveyExists(update.surveyId, state)
        .productR(validateAccessKey(update.surveyId, update.accessKey, state))
    }

  private def validateAccessKey(
    surveyId : String,
    accessKey: String,
    state    : DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataApplicationValidationErrorOr[Unit] =
    state.calculated.surveys.get(surveyId).map { survey =>
      InvalidAccessKey.unlessA(survey.accessKey == accessKey)
    }.getOrElse(SurveyNotExists.invalid)

  private def validateIfSurveyExists(
    surveyId: String,
    state   : DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataApplicationValidationErrorOr[Unit] =
    SurveyNotExists.unlessA(state.calculated.surveys.contains(surveyId))

  private def validateIfSurveyIsOpen(
    surveyId: String,
    state   : DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataApplicationValidationErrorOr[Unit] =
    state.calculated.surveys.get(surveyId).map { survey =>
      SurveyClosed.unlessA(java.time.Instant.now().isBefore(survey.endTime))
    }.getOrElse(SurveyNotExists.invalid)

  private def validateIfMaxResponsesNotReached(
    surveyId: String,
    state   : DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataApplicationValidationErrorOr[Unit] =
    state.calculated.surveys.get(surveyId).map { survey =>
      MaxResponsesReached.unlessA(survey.responses.size < survey.maxResponses)
    }.getOrElse(SurveyNotExists.invalid)
}