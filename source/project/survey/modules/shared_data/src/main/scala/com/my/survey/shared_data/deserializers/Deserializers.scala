package com.my.survey.shared_data.deserializers

import com.my.survey.shared_data.types.Types._
import io.circe.Decoder
import io.circe.jawn.decode
import org.tessellation.currency.dataApplication.DataUpdate
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationBlock
import org.tessellation.security.signature.Signed

import java.nio.charset.StandardCharsets

object Deserializers {
  private def deserializeA[A: Decoder](bytes: Array[Byte]): Either[Throwable, A] =
    decode[A](new String(bytes, StandardCharsets.UTF_8))

  def deserializeUpdate(
    bytes: Array[Byte]
  ): Either[Throwable, SurveyUpdate] =
    deserializeA[SurveyUpdate](bytes)

  def deserializeState(
    bytes: Array[Byte]
  ): Either[Throwable, SurveyUpdatesState] =
    deserializeA[SurveyUpdatesState](bytes)

  def deserializeBlock(
    bytes: Array[Byte]
  )(implicit e: Decoder[DataUpdate]): Either[Throwable, Signed[DataApplicationBlock]] =
    deserializeA[Signed[DataApplicationBlock]](bytes)

  def deserializeCalculatedState(
    bytes: Array[Byte]
  ): Either[Throwable, SurveyCalculatedState] =
    deserializeA[SurveyCalculatedState](bytes)
}
