package com.my.survey.shared_data.calculated_state

import cats.effect.Ref
import cats.effect.kernel.Async
import cats.syntax.functor.toFunctorOps
import com.my.survey.shared_data.types.Types._
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.security.hash.Hash

import java.nio.charset.StandardCharsets

trait CalculatedStateService[F[_]] {
  def getCalculatedState: F[CalculatedState]

  def setCalculatedState(
    snapshotOrdinal: SnapshotOrdinal,
    state          : SurveyCalculatedState
  ): F[Boolean]

  def hashCalculatedState(
    state: SurveyCalculatedState
  ): F[Hash]
}

object CalculatedStateService {
  def make[F[_] : Async]: F[CalculatedStateService[F]] = {
    Ref.of[F, CalculatedState](CalculatedState.empty).map { stateRef =>
      new CalculatedStateService[F] {
        override def getCalculatedState: F[CalculatedState] = stateRef.get

        override def setCalculatedState(
          snapshotOrdinal: SnapshotOrdinal,
          state          : SurveyCalculatedState
        ): F[Boolean] =
          stateRef.update { currentState =>
            val currentSurveyCalculatedState = currentState.state
            val updatedSurveys = state.surveys.foldLeft(currentSurveyCalculatedState.surveys) {
              case (acc, (surveyId, survey)) =>
                acc.updated(surveyId, survey)
            }

            CalculatedState(snapshotOrdinal, SurveyCalculatedState(updatedSurveys))
          }.as(true)

        override def hashCalculatedState(
          state: SurveyCalculatedState
        ): F[Hash] = Async[F].delay {
          def removeKey(json: Json, keyToRemove: String): Json =
            json.mapObject { obj =>
              obj.filterKeys(_ != keyToRemove).mapValues {
                case objValue: Json => removeKey(objValue, keyToRemove)
                case other => other
              }
            }.mapArray { arr =>
              arr.map(removeKey(_, keyToRemove))
            }

          val stateAsString = removeKey(state.asJson, "creationDateTimestamp")
            .deepDropNullValues
            .noSpaces

          Hash.fromBytes(stateAsString.getBytes(StandardCharsets.UTF_8))
        }
      }
    }
  }
}