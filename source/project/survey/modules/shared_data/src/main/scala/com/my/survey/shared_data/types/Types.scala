package com.my.survey.shared_data.types

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive
import org.tessellation.currency.dataApplication.{DataCalculatedState, DataOnChainState, DataUpdate}
import org.tessellation.schema.address.Address

import java.time.Instant

object Types {
  // Represents a single question in a survey
  @derive(decoder, encoder)
  case class Question(
    id: String,
    text: String,
    questionType: String, // "text", "radio", "checkbox", or "scale"
    options: Option[List[String]], // For radio and checkbox questions
    min: Option[Int], // For scale questions
    max: Option[Int] // For scale questions
  )

  // Represents an encrypted response to a survey
  @derive(decoder, encoder)
  case class Response(
    respondent: Address,
    encryptedAnswers: String
  )

  // Represents a complete survey
  @derive(decoder, encoder)
  case class Survey(
    id: String,
    creator: Address,
    title: String,
    description: String,
    questions: List[Question],
    tokenReward: String,
    endTime: Instant,
    maxResponses: Int,
    minimumResponseTime: Int,
    tags: List[String],
    responses: List[Response],
    totalParticipants: Int,
    averageCompletionTime: Double,
    accessKey: String // New field for access key
  )

  // Represents updates to the survey system
  @derive(decoder, encoder)
  sealed trait SurveyUpdate extends DataUpdate

  // Update to create a new survey
  @derive(decoder, encoder)
  case class CreateSurvey(
    creator: Address,
    title: String,
    description: String,
    questions: List[Question],
    tokenReward: String,
    endTime: Instant,
    maxResponses: Int,
    minimumResponseTime: Int,
    tags: List[String]
  ) extends SurveyUpdate

  // Update to submit a response to a survey
  @derive(decoder, encoder)
  case class SubmitResponse(
    surveyId: String,
    respondent: Address,
    encryptedAnswers: String
  ) extends SurveyUpdate

  // Represents the on-chain state of survey updates
  @derive(decoder, encoder)
  case class SurveyUpdatesState(
    updates: List[SurveyUpdate]
  ) extends DataOnChainState

  // Represents the calculated state of surveys
  @derive(decoder, encoder)
  case class SurveyCalculatedState(
    surveys: Map[String, Survey]
  ) extends DataCalculatedState

  // Response object for survey queries
  @derive(decoder, encoder)
  case class SurveyResponse(
    id: String,
    creator: Address,
    title: String,
    description: String,
    questions: List[Question],
    tokenReward: String,
    endTime: Instant,
    maxResponses: Int,
    minimumResponseTime: Int,
    tags: List[String],
    totalParticipants: Int,
    averageCompletionTime: Double
  )

  // Response object for encrypted survey responses
  @derive(decoder, encoder)
  case class EncryptedResponsesResponse(
    surveyId: String,
    responses: List[Response]
  )

  // Update to decrypt responses
  @derive(decoder, encoder)
  case class DecryptResponses(
    surveyId: String,
    accessKey: String
  ) extends SurveyUpdate
}