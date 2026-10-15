package com.example.test.util

import android.content.Context
import cn.pedant.SweetAlert.SweetAlertDialog

object AlertUtils {

    fun showWarning(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "Entiendo",
        onConfirm: (() -> Unit)? = null
    ) {
        val dialog = SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(confirmText)
            .setConfirmClickListener { d ->
                d.dismissWithAnimation()
                onConfirm?.invoke()
            }
        dialog.setCancelable(true)
        dialog.show()
    }

    fun showSuccess(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "Aceptar",
        onConfirm: (() -> Unit)? = null
    ) {
        val dialog = SweetAlertDialog(context, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(confirmText)
            .setConfirmClickListener { d ->
                d.dismissWithAnimation()
                onConfirm?.invoke()
            }
        dialog.setCancelable(false)
        dialog.show()
    }

    fun showError(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "Cerrar",
        onConfirm: (() -> Unit)? = null
    ) {
        val dialog = SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(confirmText)
            .setConfirmClickListener { d ->
                d.dismissWithAnimation()
                onConfirm?.invoke()
            }
        dialog.setCancelable(true)
        dialog.show()
    }

    fun showConfirmation(
        context: Context,
        title: String,
        message: String,
        confirmText: String = "Sí",
        cancelText: String = "No",
        onConfirm: () -> Unit
    ) {
        SweetAlertDialog(context, SweetAlertDialog.WARNING_TYPE)
            .setTitleText(title)
            .setContentText(message)
            .setConfirmText(confirmText)
            .setCancelText(cancelText)
            .setConfirmClickListener { dialog ->
                dialog.dismissWithAnimation()
                onConfirm()
            }
            .setCancelClickListener { dialog ->
                dialog.dismissWithAnimation()
            }
            .show()
    }
}
