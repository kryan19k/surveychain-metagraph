package com.my.survey.shared_data.combiners

import com.my.survey.shared_data.types.Types._
import org.tessellation.currency.dataApplication.DataState
import org.tessellation.schema.address.Address
import org.tessellation.security.hash.Hash

import java.util.UUID

object Combiners {
  def combineCreateSurvey(
    update      : CreateSurvey,
    state       : DataState[SurveyUpdatesState, SurveyCalculatedState],
    surveyCreator: Address
  ): DataState[SurveyUpdatesState, SurveyCalculatedState] = {
    val surveyId = Hash.fromBytes(update.toString.getBytes).toString
    val accessKey = UUID.randomUUID().toString
    val newSurvey = Survey(
      id = surveyId,
      creator = surveyCreator,
      title = update.title,
      description = update.description,
      questions = update.questions,
      tokenReward = update.tokenReward,
      endTime = update.endTime,
      maxResponses = update.maxResponses,
      minimumResponseTime = update.minimumResponseTime,
      tags = update.tags,
      responses = List.empty,
      totalParticipants = 0,
      averageCompletionTime = 0.0,
      accessKey = accessKey
    )

    val newUpdatesList = state.onChain.updates :+ update
    val newCalculatedState = state.calculated.copy(
      surveys = state.calculated.surveys + (surveyId -> newSurvey)
    )

    DataState(SurveyUpdatesState(newUpdatesList), newCalculatedState)
  }

  def combineSubmitResponse(
    update: SubmitResponse,
    state : DataState[SurveyUpdatesState, SurveyCalculatedState]
  ): DataState[SurveyUpdatesState, SurveyCalculatedState] = {
    state.calculated.surveys.get(update.surveyId) match {
      case Some(survey) =>
        val newResponse = Response(update.respondent, update.encryptedAnswers)
        val updatedSurvey = survey.copy(
          responses = survey.responses :+ newResponse,
          totalParticipants = survey.totalParticipants + 1
          // Note: We should update averageCompletionTime here, but we need more information to do so
        )

        // TODO: Implement token reward distribution here

        val newUpdatesList = state.onChain.updates :+ update
        val newCalculatedState = state.calculated.copy(
          surveys = state.calculated.surveys + (update.surveyId -> updatedSurvey)
        )

        DataState(SurveyUpdatesState(newUpdatesList), newCalculatedState)
      case None =>
        // If the survey doesn't exist, we don't change the state
        state
    }
  }
}