package com.finsim.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.finsim.domain.LoanResult
import kotlin.js.js

private fun jsDownloadCsv(content: String, fileName: String, onError: (String) -> Unit, onSuccess: (String) -> Unit): Unit = js("""{
    try {
        const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        onSuccess('Archivo CSV exportado.');
    } catch (e) {
        onError('No se pudo exportar el CSV: ' + (e.message || e));
    }
}""")

private fun jsDownloadArrow(rowsJson: String, fileName: String, onError: (String) -> Unit, onSuccess: (String) -> Unit): Unit = js("""{
    try {
        const rows = JSON.parse(rowsJson);
        const Arrow = window.Arrow;
        if (!Arrow) throw new Error('Motor Apache Arrow no disponible en la página.');
        const months = new Int32Array(rows.map(r => r.month));
        const dates = rows.map(r => r.date);
        const openingBalances = new Float64Array(rows.map(r => parseFloat(r.openingBalance) || 0));
        const regularCapitals = new Float64Array(rows.map(r => parseFloat(r.regularCapital) || 0));
        const extraCapitals = new Float64Array(rows.map(r => parseFloat(r.extraCapital) || 0));
        const interestChargeds = new Float64Array(rows.map(r => parseFloat(r.interestCharged) || 0));
        const insuranceChargeds = new Float64Array(rows.map(r => parseFloat(r.insuranceCharged) || 0));
        const interestPaids = new Float64Array(rows.map(r => parseFloat(r.interestPaid) || 0));
        const insurancePaids = new Float64Array(rows.map(r => parseFloat(r.insurancePaid) || 0));
        const otherChargesPaids = new Float64Array(rows.map(r => parseFloat(r.otherChargesPaid) || 0));
        const cashPaids = new Float64Array(rows.map(r => parseFloat(r.cashPaid) || 0));
        const closingBalances = new Float64Array(rows.map(r => parseFloat(r.closingBalance) || 0));
        const pendingInterests = new Float64Array(rows.map(r => parseFloat(r.pendingInterest) || 0));
        const pendingInsurances = new Float64Array(rows.map(r => parseFloat(r.pendingInsurance) || 0));
        const overduePrincipals = new Float64Array(rows.map(r => parseFloat(r.overduePrincipal) || 0));
        const arrearss = new Float64Array(rows.map(r => parseFloat(r.arrears) || 0));
        const debts = new Float64Array(rows.map(r => parseFloat(r.debt) || 0));
        const unapplieds = new Float64Array(rows.map(r => parseFloat(r.unapplied) || 0));

        const table = Arrow.tableFromArrays({
            month: months,
            date: dates,
            opening_balance: openingBalances,
            scheduled_principal: regularCapitals,
            extra_principal: extraCapitals,
            interest_charged: interestChargeds,
            insurance_charged: insuranceChargeds,
            interest_paid: interestPaids,
            insurance_paid: insurancePaids,
            other_charges_paid: otherChargesPaids,
            total_paid: cashPaids,
            closing_balance: closingBalances,
            pending_interest: pendingInterests,
            pending_insurance: pendingInsurances,
            overdue_principal: overduePrincipals,
            arrears: arrearss,
            debt: debts,
            unapplied: unapplieds
        });

        const ipcBytes = Arrow.tableToIPC(table, 'file');
        const blob = new Blob([ipcBytes], { type: 'application/vnd.apache.arrow.file' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
        onSuccess('Archivo Apache Arrow (.arrow) exportado.');
    } catch (e) {
        onError('No se pudo exportar Apache Arrow: ' + (e.message || e));
    }
}""")

@Composable
actual fun rememberScheduleDownloader(): ScheduleDownloader {
    return remember {
        object : ScheduleDownloader {
            override fun downloadCsv(fileName: String, csvContent: String, onMessage: (String) -> Unit) {
                jsDownloadCsv(csvContent, fileName, onMessage, onMessage)
            }

            override fun downloadArrow(fileName: String, result: LoanResult, onMessage: (String) -> Unit) {
                val json = serializeScheduleToJson(result)
                jsDownloadArrow(json, fileName, onMessage, onMessage)
            }
        }
    }
}
