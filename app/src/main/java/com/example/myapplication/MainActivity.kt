package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. DATA ENTITY & ROOM DATABASE SETUPS
// ==========================================

enum class TransactionType { PEMASUKAN, PENGELUARAN }

@Entity(tableName = "financial_records")
data class FinancialRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val date: String,
    val category: String,
    val type: TransactionType,
    val description: String = ""
)

@Dao
interface FinancialDao {
    @Query("SELECT * FROM financial_records ORDER BY id DESC")
    fun getAllRecords(): Flow<List<FinancialRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: FinancialRecord)

    @Delete
    suspend fun deleteRecord(record: FinancialRecord)
}

@Database(entities = [FinancialRecord::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financialDao(): FinancialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financial_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name
}

// ==========================================
// 2. VIEWMODEL
// ==========================================

class FinancialViewModel(private val dao: FinancialDao) : ViewModel() {
    val records: Flow<List<FinancialRecord>> = dao.getAllRecords()

    fun addRecord(amount: Double, date: String, category: String, type: TransactionType) {
        viewModelScope.launch {
            dao.insertRecord(
                FinancialRecord(
                    amount = amount,
                    date = date,
                    category = category,
                    type = type
                )
            )
        }
    }

    fun deleteRecord(record: FinancialRecord) {
        viewModelScope.launch {
            dao.deleteRecord(record)
        }
    }
}

class FinancialViewModelFactory(private val dao: FinancialDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FinancialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FinancialViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// ==========================================
// 3. MAIN ACTIVITY & UI (JETPACK COMPOSE)
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FinancialTrackerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialTrackerApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val viewModel: FinancialViewModel = viewModel(
        factory = FinancialViewModelFactory(database.financialDao())
    )

    val records by viewModel.records.collectAsState(initial = emptyList())
    var showDialog by remember { mutableStateOf(false) }

    // Hitung Ringkasan
    val totalIncome = records.filter { it.type == TransactionType.PEMASUKAN }.sumOf { it.amount }
    val totalExpense = records.filter { it.type == TransactionType.PENGELUARAN }.sumOf { it.amount }
    val totalBalance = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catatan Keuangan", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Catatan")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Summary Cards
            SummarySection(totalBalance, totalIncome, totalExpense)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Riwayat Transaksi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // List Transaksi
            if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Belum ada catatan keuangan.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        RecordItem(record = record, onDelete = { viewModel.deleteRecord(record) })
                    }
                }
            }
        }

        // Dialog Tambah Transaksi
        if (showDialog) {
            AddRecordDialog(
                onDismiss = { showDialog = false },
                onSave = { amount, date, category, type ->
                    viewModel.addRecord(amount, date, category, type)
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun SummarySection(balance: Double, income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Total Saldo", fontSize = 14.sp, color = Color.Gray)
            Text(
                text = formatRupiah(balance),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Pemasukan", fontSize = 12.sp, color = Color.Gray)
                        Text(formatRupiah(income), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Pengeluaran", fontSize = 12.sp, color = Color.Gray)
                        Text(formatRupiah(expense), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordItem(record: FinancialRecord, onDelete: () -> Unit) {
    val isIncome = record.type == TransactionType.PEMASUKAN
    val amountColor = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
    val icon = if (isIncome) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = amountColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = amountColor)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = record.category, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = record.date, fontSize = 12.sp, color = Color.Gray)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isIncome) "+" else "-"} ${formatRupiah(record.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    fontSize = 14.sp
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (amount: Double, date: String, category: String, type: TransactionType) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var categoryText by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.PENGELUARAN) }

    val currentDate = remember {
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
    }

    val incomeCategories = listOf("Gaji", "Bonus", "Investasi", "Penjualan", "Lainnya")
    val expenseCategories = listOf("Makanan", "Transportasi", "Belanja", "Tagihan", "Hiburan", "Lainnya")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Catatan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Selector Tipe Transaksi
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = selectedType == TransactionType.PENGELUARAN,
                        onClick = { selectedType = TransactionType.PENGELUARAN },
                        label = { Text("Pengeluaran") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilterChip(
                        selected = selectedType == TransactionType.PEMASUKAN,
                        onClick = { selectedType = TransactionType.PEMASUKAN },
                        label = { Text("Pemasukan") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Input Nominal dengan Format Titik Otomatis (misal: 20.000)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val clean = input.replace(Regex("[^\\d]"), "")
                        amountText = if (clean.isNotEmpty()) {
                            val parsed = clean.toLongOrNull() ?: 0L
                            NumberFormat.getNumberInstance(Locale("id", "ID")).format(parsed)
                        } else {
                            ""
                        }
                    },
                    label = { Text("Nominal (Rp)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Input / Pilih Kategori
                Text("Pilih/Ketik Jenis Category:", fontSize = 12.sp, color = Color.Gray)
                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Kategori") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Category Chips
                val currentCategories = if (selectedType == TransactionType.PEMASUKAN) incomeCategories else expenseCategories
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentCategories.take(3).forEach { cat ->
                        SuggestionChip(
                            onClick = { categoryText = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Membersihkan titik ribuan sebelum disimpan ke database
                    val cleanString = amountText.replace(Regex("[^\\d]"), "")
                    val amount = cleanString.toDoubleOrNull() ?: 0.0

                    if (amount > 0 && categoryText.isNotBlank()) {
                        onSave(amount, currentDate, categoryText, selectedType)
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}
fun formatRupiah(number: Double): String {
    val localeID = Locale("id", "ID")
    val format = NumberFormat.getCurrencyInstance(localeID)
    return format.format(number).replace(",00", "")
}
