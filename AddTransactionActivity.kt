package com.example.expensetracker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.RadioButton
import android.widget.Toast
import androidx.core.view.ViewCompat
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.util.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.app.DatePickerDialog
import android.widget.RadioGroup
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var radioType: RadioGroup
    private lateinit var rExpense: RadioButton
    private lateinit var rInvestment: RadioButton

    private lateinit var etAmount: TextInputEditText
    private lateinit var etCategory: TextInputEditText
    private lateinit var etDate: TextInputEditText
    private lateinit var etNote: TextInputEditText

    private lateinit var btnSave: MaterialButton

    private var selectedType: String = TransactionEntity.TYPE_EXPENSE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_transaction)

        db = AppDatabase.getInstance(this)

        radioType = findViewById(R.id.radioType)
        rExpense = findViewById(R.id.rExpense)
        rInvestment = findViewById(R.id.rInvestment)

        etAmount = findViewById(R.id.etAmount)
        etCategory = findViewById(R.id.etCategory)
        etDate = findViewById(R.id.etDate)
        etNote = findViewById(R.id.etNote)
        btnSave = findViewById(R.id.btnSave)

        selectedType = intent.getStringExtra(EXTRA_TYPE) ?: TransactionEntity.TYPE_EXPENSE
        if (selectedType == TransactionEntity.TYPE_INVESTMENT) {
            rInvestment.isChecked = true
        } else {
            rExpense.isChecked = true
        }

        etDate.setText(DateUtils.nowDateString())
        etDate.setOnClickListener {
            showDatePicker()
        }

        // If the user changes the radio button, update selected type.
        radioType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.rInvestment -> TransactionEntity.TYPE_INVESTMENT
                else -> TransactionEntity.TYPE_EXPENSE
            }
        }

        btnSave.setOnClickListener {
            val cents = MoneyUtils.parseToCents(etAmount.text?.toString())
            val category = etCategory.text?.toString()?.trim().orEmpty()
            val note = etNote.text?.toString()?.trim().orEmpty()
            val date = etDate.text?.toString()?.trim().orEmpty()

            if (cents <= 0L) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (date.isEmpty()) {
                Toast.makeText(this, "Pick a date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categorySafe = if (category.isBlank()) "General" else category

            val tx = TransactionEntity(
                type = selectedType,
                amountCents = cents,
                category = categorySafe,
                date = date,
                note = note
            )

            executor.execute {
                db.transactionDao().insertTransaction(tx)
                runOnUiThread { finish() }
            }
        }
    }

    private fun showDatePicker() {
        val parts = etDate.text?.toString()?.trim()?.split("-") ?: emptyList()
        val cal = Calendar.getInstance()
        if (parts.size == 3) {
            cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
            cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1)
            cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH))
        }

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                etDate.setText(dateStr)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    companion object {
        const val EXTRA_TYPE = "EXTRA_TYPE"
    }
}
