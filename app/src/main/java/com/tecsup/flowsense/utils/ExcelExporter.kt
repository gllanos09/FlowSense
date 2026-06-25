package com.tecsup.flowsense.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.tecsup.flowsense.model.Negocio
import com.tecsup.flowsense.model.RegistroAforo
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun exportarReporteExcel(
    context: Context,
    negocio: Negocio,
    registros: List<RegistroAforo>
): Boolean {
    return try {
        val workbook = XSSFWorkbook()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val fecha = dateFormat.format(Date())

        val resumenSheet = workbook.createSheet("Resumen")
        val datos = listOf(
            "Negocio" to negocio.nombre,
            "Fecha" to fecha,
            "Total Entradas" to registros.count { it.tipo == "ENTRADA" }.toString(),
            "Total Salidas" to registros.count { it.tipo == "SALIDA" }.toString(),
            "Pico Maximo" to (registros.maxOfOrNull { it.aforoActual } ?: 0).toString()
        )
        datos.forEachIndexed { i, (label, value) ->
            val row = resumenSheet.createRow(i)
            row.createCell(0).setCellValue(label)
            row.createCell(1).setCellValue(value)
        }

        val detalleSheet = workbook.createSheet("Detalle")
        val headerRow = detalleSheet.createRow(0)
        headerRow.createCell(0).setCellValue("Hora")
        headerRow.createCell(1).setCellValue("Tipo")
        headerRow.createCell(2).setCellValue("Aforo")

        registros.sortedBy { it.timestamp }.forEachIndexed { i, registro ->
            val row = detalleSheet.createRow(i + 1)
            row.createCell(0).setCellValue(timeFormat.format(Date(registro.timestamp)))
            row.createCell(1).setCellValue(registro.tipo)
            row.createCell(2).setCellValue(registro.aforoActual.toDouble())
        }

        val fileName = "FlowSense_Reporte_${fecha.replace("/", "-")}.xlsx"
        val file = File(context.getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { workbook.write(it) }
        workbook.close()

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
