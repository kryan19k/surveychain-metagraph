package com.my.survey.shared_data.calculated_state

import com.my.survey.shared_data.types.Types._
import eu.timepit.refined.types.all.NonNegLong
import org.tessellation.schema.SnapshotOrdinal

case class CalculatedState(ordinal: SnapshotOrdinal, state: SurveyCalculatedState)

object CalculatedState {
  def empty: CalculatedState =
    CalculatedState(SnapshotOrdinal(NonNegLong(0L)), SurveyCalculatedState(Map.empty))
}
