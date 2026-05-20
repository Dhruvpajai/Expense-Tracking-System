package com.example.expensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.MonthlyGoalEntity
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.ui.TransactionAdapter
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.util.MoneyUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private lateinit var tvMonth: TextView
    private lateinit var etPlannedExpense: TextInputEditText
    private lateinit var etPlannedInvestment: TextInputEditText
    private lateinit var btnSaveGoals: com.google.android.material.button.MaterialButton
    private lateinit var tvExpensesTotal: TextView
    private lateinit var tvInvestmentsTotal: TextView
    private lateinit var tvRemaining: TextView
    private lateinit var progressExpense: LinearProgressIndicator
    private lateinit var btnAddExpense: com.google.android.material.button.MaterialButton
    private lateinit var btnAddInvestment: com.google.android.material.button.MaterialButton

    private lateinit var adapter: TransactionAdapter
    private var selectedMonthPrefix: String = DateUtils.currentMonthPrefix()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        db = AppDatabase.getInstance(this)

        tvMonth = findViewById(R.id.tvMonth)
        etPlannedExpense = findViewById(R.id.etPlannedExpense)
        etPlannedInvestment = findViewById(R.id.etPlannedInvestment)
        btnSaveGoals = findViewById(R.id.btnSaveGoals)
        tvExpensesTotal = findViewById(R.id.tvExpensesTotal)
        tvInvestmentsTotal = findViewById(R.id.tvInvestmentsTotal)
        tvRemaining = findViewById(R.id.tvRemaining)
        progressExpense = findViewById(R.id.progressExpense)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnAddInvestment = findViewById(R.id.btnAddInvestment)

        val recyclerRecent = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recyclerRecent)
        recyclerRecent.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(this)
        recyclerRecent.adapter = adapter

        tvMonth.text = DateUtils.monthLabelFromPrefix(selectedMonthPrefix)

        btnAddExpense.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra(AddTransactionActivity.EXTRA_TYPE, TransactionEntity.TYPE_EXPENSE)
            startActivity(intent)
        }

        btnAddInvestment.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            intent.putExtra(AddTransactionActivity.EXTRA_TYPE, TransactionEntity.TYPE_INVESTMENT)
            startActivity(intent)
        }

        btnSaveGoals.setOnClickListener {
            val plannedExpenseCents = MoneyUtils.parseToCents(etPlannedExpense.text?.toString())
            val plannedInvestmentCents = MoneyUtils.parseToCents(etPlannedInvestment.text?.toString())

            executor.execute {
                db.transactionDao().upsertGoal(
                    MonthlyGoalEntity(
                        month = selectedMonthPrefix,
                        plannedExpenseCents = plannedExpenseCents,
                        plannedInvestmentCents = plannedInvestmentCents
                    )
                )
                runOnUiThread { loadSummary() }
            }
        }

        loadSummary()
    }

    override fun onResume() {
        super.onResume()
        loadSummary()
    }

    private fun loadSummary() {
        executor.execute {
            val goal = db.transactionDao().getGoal(selectedMonthPrefix)
            val plannedExpense = goal?.plannedExpenseCents ?: 0L
            val plannedInvestment = goal?.plannedInvestmentCents ?: 0L

            val expenses = db.transactionDao().sumExpensesForMonth(selectedMonthPrefix)
            val investments = db.transactionDao().sumInvestmentsForMonth(selectedMonthPrefix)

            val recent = db.transactionDao().getRecentTransactionsForMonth(selectedMonthPrefix, 6)

            runOnUiThread {
                etPlannedExpense.setText(
                    if (plannedExpense == 0L) "" else MoneyUtils.centsToMajorString(plannedExpense)
                )
                etPlannedInvestment.setText(
                    if (plannedInvestment == 0L) "" else MoneyUtils.centsToMajorString(plannedInvestment)
                )

                tvExpensesTotal.text = "Expenses: ${MoneyUtils.formatCents(this, expenses)} / ${MoneyUtils.formatCents(this, plannedExpense)}"
                tvInvestmentsTotal.text = "Investments: ${MoneyUtils.formatCents(this, investments)}"
                tvRemaining.text = "Remaining: ${MoneyUtils.formatCents(this, plannedExpense - expenses)}"

                val rawProgress = if (plannedExpense <= 0L) 0 else ((expenses * 100L) / plannedExpense).toInt()
                val progressInt = when {
                    rawProgress < 0 -> 0
                    rawProgress > 100 -> 100
                    else -> rawProgress
                }
                progressExpense.progress = progressInt

                adapter.submitList(recent)
            }
        }
    }
}
