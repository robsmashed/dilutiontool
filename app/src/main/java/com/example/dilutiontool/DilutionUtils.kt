package com.example.dilutiontool

import com.example.dilutiontool.entity.Dilution

object DilutionUtils {
    fun getDescription(dilution: Dilution): String {
        val valueDescription = if (dilution.value == 0) "puro" else "1:${dilution.value}"
        val minValueDescription = if (dilution.minValue == 0) "puro" else "1:${dilution.minValue}"
        val dilutionValues = if (dilution.minValue != dilution.value) "$minValueDescription - $valueDescription" else valueDescription
        return "${dilution.description} [$dilutionValues]"
    }
}
