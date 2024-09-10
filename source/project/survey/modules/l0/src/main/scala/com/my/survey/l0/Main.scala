package com.my.survey.l0


import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.syntax.option.catsSyntaxOptionId
import com.my.survey.l0.custom_routes.CustomRoutes
import com.my.survey.shared_data.LifecycleSharedFunctions
import com.my.survey.shared_data.calculated_state.CalculatedStateService
import com.my.survey.shared_data.deserializers.Deserializers
import com.my.survey.shared_data.serializers.Serializers
import com.my.survey.shared_data.types.Types._
import io.circe.{Decoder, Encoder}
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.{EntityDecoder, HttpRoutes}
import org.tessellation.BuildInfo
import org.tessellation.currency.dataApplication._
import org.tessellation.currency.dataApplication.dataApplication._
import org.tessellation.currency.l0.CurrencyL0App
import org.tessellation.ext.cats.effect.ResourceIO
import org.tessellation.schema.SnapshotOrdinal
import org.tessellation.schema.cluster.ClusterId
import org.tessellation.schema.semver.{MetagraphVersion, TessellationVersion}
import org.tessellation.security.hash.Hash
import org.tessellation.security.signature.Signed

import java.util.UUID

object Main
  extends CurrencyL0App(
    "survey-l0",
    "Survey L0 node",
    ClusterId(UUID.fromString("517c3a05-9219-471b-a54c-21b7d72f4ae5")),
    metagraphVersion = MetagraphVersion.unsafeFrom(BuildInfo.version),
    tessellationVersion = TessellationVersion.unsafeFrom(BuildInfo.version)
  ) {
  private def makeBaseDataApplicationL0Service(
    calculatedStateService: CalculatedStateService[IO]
  ): BaseDataApplicationL0Service[IO] = BaseDataApplicationL0Service(new DataApplicationL0Service[IO, SurveyUpdate, SurveyUpdatesState, SurveyCalculatedState] {
    override def genesis: DataState[SurveyUpdatesState, SurveyCalculatedState] =
      DataState(SurveyUpdatesState(List.empty), SurveyCalculatedState(Map.empty))

    override def validateData(
      state  : DataState[SurveyUpdatesState, SurveyCalculatedState],
      updates: NonEmptyList[Signed[SurveyUpdate]]
    )(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] =
      LifecycleSharedFunctions.validateData[IO](state, updates)

    override def validateUpdate(
      update: SurveyUpdate
    )(implicit context: L0NodeContext[IO]): IO[DataApplicationValidationErrorOr[Unit]] =
      LifecycleSharedFunctions.validateUpdate[IO](update)

    override def combine(
      state  : DataState[SurveyUpdatesState, SurveyCalculatedState],
      updates: List[Signed[SurveyUpdate]]
    )(implicit context: L0NodeContext[IO]): IO[DataState[SurveyUpdatesState, SurveyCalculatedState]] =
      LifecycleSharedFunctions.combine[IO](state, updates)

    override def serializeState(
      state: SurveyUpdatesState
    ): IO[Array[Byte]] =
      IO(Serializers.serializeState(state))

    override def serializeUpdate(
      update: SurveyUpdate
    ): IO[Array[Byte]] =
      IO(Serializers.serializeUpdate(update))

    override def serializeBlock(
      block: Signed[DataApplicationBlock]
    ): IO[Array[Byte]] = IO(Serializers.serializeBlock(block)(dataEncoder.asInstanceOf[Encoder[DataUpdate]]))

    override def deserializeState(
      bytes: Array[Byte]
    ): IO[Either[Throwable, SurveyUpdatesState]] =
      IO(Deserializers.deserializeState(bytes))

    override def deserializeUpdate(
      bytes: Array[Byte]
    ): IO[Either[Throwable, SurveyUpdate]] =
      IO(Deserializers.deserializeUpdate(bytes))

    override def deserializeBlock(
      bytes: Array[Byte]
    ): IO[Either[Throwable, Signed[DataApplicationBlock]]] =
      IO(Deserializers.deserializeBlock(bytes)(dataDecoder.asInstanceOf[Decoder[DataUpdate]]))

    override def dataEncoder: Encoder[SurveyUpdate] =
      implicitly[Encoder[SurveyUpdate]]

    override def dataDecoder: Decoder[SurveyUpdate] =
      implicitly[Decoder[SurveyUpdate]]

    override def calculatedStateEncoder: Encoder[SurveyCalculatedState] =
      implicitly[Encoder[SurveyCalculatedState]]

    override def calculatedStateDecoder: Decoder[SurveyCalculatedState] =
      implicitly[Decoder[SurveyCalculatedState]]

    override def routes(implicit context: L0NodeContext[IO]): HttpRoutes[IO] =
      CustomRoutes[IO](calculatedStateService).public

    override def signedDataEntityDecoder: EntityDecoder[IO, Signed[SurveyUpdate]] =
      circeEntityDecoder

    override def getCalculatedState(implicit context: L0NodeContext[IO]): IO[(SnapshotOrdinal, SurveyCalculatedState)] =
      calculatedStateService.getCalculatedState.map(calculatedState => (calculatedState.ordinal, calculatedState.state))

    override def setCalculatedState(
      ordinal: SnapshotOrdinal,
      state  : SurveyCalculatedState
    )(implicit context: L0NodeContext[IO]): IO[Boolean] =
      calculatedStateService.setCalculatedState(ordinal, state)

    override def hashCalculatedState(
      state: SurveyCalculatedState
    )(implicit context: L0NodeContext[IO]): IO[Hash] =
      calculatedStateService.hashCalculatedState(state)

    override def serializeCalculatedState(
      state: SurveyCalculatedState
    ): IO[Array[Byte]] =
      IO(Serializers.serializeCalculatedState(state))

    override def deserializeCalculatedState(
      bytes: Array[Byte]
    ): IO[Either[Throwable, SurveyCalculatedState]] =
      IO(Deserializers.deserializeCalculatedState(bytes))
  })

  private def makeL0Service: IO[BaseDataApplicationL0Service[IO]] =
    CalculatedStateService.make[IO].map(makeBaseDataApplicationL0Service)

  override def dataApplication: Option[Resource[IO, BaseDataApplicationL0Service[IO]]] =
    makeL0Service.asResource.some
}