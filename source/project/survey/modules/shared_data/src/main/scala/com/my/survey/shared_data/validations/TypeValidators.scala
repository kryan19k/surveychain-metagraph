package com.my.survey.shared_data.validations

import com.my.survey.shared_data.errors.Errors._
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.schema.address.Address

object TypeValidators {
  def validateStringMaxSize(
    value    : String,
    maxSize  : Long,
    fieldName: String
  ): DataApplicationValidationErrorOr[Unit] =
    InvalidFieldSize(fieldName, maxSize).whenA(value.length > maxSize)

  def validateListMaxSize[A](
    value    : List[A],
    maxSize  : Long,
    fieldName: String
  ): DataApplicationValidationErrorOr[Unit] =
    InvalidFieldSize(fieldName, maxSize).whenA(value.size > maxSize)

  def validateProvidedAddress(
    proofAddresses: List[Address],
    address       : Address
  ): DataApplicationValidationErrorOr[Unit] =
    InvalidAddress.unlessA(proofAddresses.contains(address))
}