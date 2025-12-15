package com.hotelsaidarshan.guestdocscanner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotelsaidarshan.guestdocscanner.data.GuestDocumentData
import com.hotelsaidarshan.guestdocscanner.ui.components.FieldInputRow
import com.hotelsaidarshan.guestdocscanner.ui.components.ValidationState
import com.hotelsaidarshan.guestdocscanner.utils.ClipboardHelper
import com.hotelsaidarshan.guestdocscanner.validation.FieldValidator

@Composable
fun VerificationScreen(
    data: GuestDocumentData,
    errorMessage: String?,
    onDataChange: (GuestDocumentData) -> Unit,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        Text(
            text = "Verify & Edit",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
        )

        if (!errorMessage.isNullOrBlank()) {
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                text = errorMessage,
                color = Color.Red,
                fontSize = 16.sp,
            )
        } else {
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                text = "Edit any field if needed. Use Copy to paste into Pathik.",
                fontSize = 16.sp,
            )
        }

        FieldInputRow(
            label = "Name",
            value = data.name,
            validationState = if (data.name.isBlank()) ValidationState.Neutral else ValidationState.Valid,
            onValueChange = { onDataChange(data.copy(name = it)) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Name", data.name) },
        )

        FieldInputRow(
            label = "Date of Birth",
            value = data.dob,
            validationState = if (data.dob.isBlank()) ValidationState.Neutral else ValidationState.Valid,
            onValueChange = { onDataChange(data.copy(dob = it)) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "DOB", data.dob) },
        )

        FieldInputRow(
            label = "Gender",
            value = data.gender,
            validationState = if (data.gender.isBlank()) ValidationState.Neutral else ValidationState.Valid,
            onValueChange = { onDataChange(data.copy(gender = it)) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Gender", data.gender) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        )

        FieldInputRow(
            label = "Address",
            value = data.address,
            validationState = if (data.address.isBlank()) ValidationState.Neutral else ValidationState.Valid,
            onValueChange = { onDataChange(data.copy(address = it)) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Address", data.address) },
            singleLine = false,
        )

        FieldInputRow(
            label = "Coming From",
            value = data.comingFrom,
            validationState = if (data.comingFrom.isBlank()) ValidationState.Neutral else ValidationState.Valid,
            onValueChange = { onDataChange(data.copy(comingFrom = it)) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Coming From", data.comingFrom) },
        )

        FieldInputRow(
            label = "Going To",
            value = data.goingTo,
            validationState = if (data.goingTo.isBlank()) ValidationState.Neutral else ValidationState.Valid,
            onValueChange = { onDataChange(data.copy(goingTo = it)) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Going To", data.goingTo) },
        )

        val normalizedMobile = data.mobileNumber
        val mobileState = when {
            normalizedMobile.isBlank() -> ValidationState.Neutral
            FieldValidator.isValidMobileNumber(normalizedMobile) -> ValidationState.Valid
            else -> ValidationState.Invalid
        }
        FieldInputRow(
            label = "Mobile Number",
            value = normalizedMobile,
            validationState = mobileState,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(10)
                onDataChange(data.copy(mobileNumber = digitsOnly))
            },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Mobile", normalizedMobile) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        val normalizedVehicle = data.vehicleNumber.uppercase().replace(" ", "")
        val vehicleState = when {
            normalizedVehicle.isBlank() -> ValidationState.Neutral
            FieldValidator.isValidVehicleNumber(normalizedVehicle) -> ValidationState.Valid
            else -> ValidationState.Invalid
        }
        FieldInputRow(
            label = "Vehicle Number",
            value = normalizedVehicle,
            validationState = vehicleState,
            onValueChange = { input ->
                onDataChange(data.copy(vehicleNumber = input.uppercase().replace(" ", "")))
            },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Vehicle", normalizedVehicle) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
            ),
        )

        val roomState = when {
            data.roomNumber.isBlank() -> ValidationState.Neutral
            FieldValidator.isValidRoomNumber(data.roomNumber) -> ValidationState.Valid
            else -> ValidationState.Invalid
        }
        FieldInputRow(
            label = "Room Number",
            value = data.roomNumber,
            validationState = roomState,
            onValueChange = { onDataChange(data.copy(roomNumber = it.replace(" ", ""))) },
            onCopyClick = { ClipboardHelper.copyToClipboard(context, "Room", data.roomNumber) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        )

        Button(
            modifier = Modifier.padding(top = 8.dp),
            onClick = onDone,
        ) {
            Text(text = "Done", fontSize = 18.sp)
        }

        Text(
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            text = "No data is saved. Press Done to clear all fields from memory.",
            fontSize = 14.sp,
            color = Color.DarkGray,
        )
    }
}
