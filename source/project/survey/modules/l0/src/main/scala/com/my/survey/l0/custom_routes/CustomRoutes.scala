package com.my.survey.l0.custom_routes

import cats.effect.Async
import cats.syntax.flatMap._
import cats.syntax.functor._
import com.my.survey.shared_data.calculated_state.CalculatedStateService
import com.my.survey.shared_data.types.Types._
import eu.timepit.refined.auto._
import org.http4s.{Response => Http4sResponse, _}
import org.http4s.circe.CirceEntityCodec._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.CORS
import org.tessellation.routes.internal.{InternalUrlPrefix, PublicRoutes}
import org.tessellation.schema.address.Address
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import org.tessellation.ext.http4s.AddressVar

case class CustomRoutes[F[_] : Async](calculatedStateService: CalculatedStateService[F]) extends Http4sDsl[F] with PublicRoutes[F] {

  implicit val decryptResponsesDecoder: Decoder[DecryptResponses] = deriveDecoder[DecryptResponses]
  implicit val decryptResponsesEncoder: Encoder[DecryptResponses] = deriveEncoder[DecryptResponses]

  private def getState: F[SurveyCalculatedState] =
    calculatedStateService.getCalculatedState.map(_.state)

  private def getAllSurveys: F[Http4sResponse[F]] = {
    getState.flatMap { state =>
      val allSurveysResponse = state.surveys.values.map(surveyToResponse).toList
      Ok(allSurveysResponse)
    }
  }

  private def getSurveyById(surveyId: String): F[Http4sResponse[F]] = {
    getState.flatMap { state =>
      state.surveys.get(surveyId).map { survey =>
        Ok(surveyToResponse(survey))
      }.getOrElse(NotFound())
    }
  }

  private def getSurveyResponses(surveyId: String): F[Http4sResponse[F]] = {
    getState.flatMap { state =>
      state.surveys.get(surveyId).map { survey =>
        Ok(EncryptedResponsesResponse(surveyId, survey.responses))
      }.getOrElse(NotFound())
    }
  }

  private def getAllSurveysOfAddress(address: Address): F[Http4sResponse[F]] = {
    getState.flatMap { state =>
      val addressSurveys = state.surveys.values.filter(_.creator == address).map(surveyToResponse)
      Ok(addressSurveys)
    }
  }

  private def surveyToResponse(survey: Survey): SurveyResponse =
    SurveyResponse(
      id = survey.id,
      creator = survey.creator,
      title = survey.title,
      description = survey.description,
      questions = survey.questions,
      tokenReward = survey.tokenReward,
      endTime = survey.endTime,
      maxResponses = survey.maxResponses,
      minimumResponseTime = survey.minimumResponseTime,
      tags = survey.tags,
      totalParticipants = survey.totalParticipants,
      averageCompletionTime = survey.averageCompletionTime
    )

  private def decryptResponses(surveyId: String, accessKey: String): F[Http4sResponse[F]] = {
    getState.flatMap { state =>
      state.surveys.get(surveyId) match {
        case Some(survey) if survey.accessKey == accessKey =>
          Ok(EncryptedResponsesResponse(surveyId, survey.responses))
        case Some(_) =>
          Forbidden("Invalid access key")
        case None =>
          NotFound("Survey not found")
      }
    }
  }

  private val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "surveys" => getAllSurveys
    case GET -> Root / "surveys" / surveyId => getSurveyById(surveyId)
    case GET -> Root / "surveys" / surveyId / "responses" => getSurveyResponses(surveyId)
    case GET -> Root / "addresses" / AddressVar(address) / "surveys" => getAllSurveysOfAddress(address)
    case req @ POST -> Root / "surveys" / surveyId / "decrypt" =>
      req.as[DecryptResponses].flatMap { decryptRequest =>
        decryptResponses(surveyId, decryptRequest.accessKey)
      }
  }

  val public: HttpRoutes[F] =
    CORS
      .policy
      .withAllowCredentials(false)
      .httpRoutes(routes)

  override protected def prefixPath: InternalUrlPrefix = "/"
}