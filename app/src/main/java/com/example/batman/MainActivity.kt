package com.example.batman

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.batman.ui.theme.BatmanTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query as RetrofitQuery
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================
// Retrofit API & Data Models
// ============================

data class AviationStackResponse(
    @SerializedName("data") val data: List<AviationFlightData>
)

data class AviationFlightData(
    @SerializedName("flight") val flight: FlightInfo,
    @SerializedName("live") val live: LiveInfo?,
    @SerializedName("flight_status") val flightStatus: String,
    @SerializedName("departure") val departure: AirportInfo?,
    @SerializedName("arrival") val arrival: AirportInfo?,
    @SerializedName("airline") val airline: AirlineInfo?,
    @SerializedName("aircraft") val aircraft: AircraftInfo?
)

data class FlightInfo(
    @SerializedName("iata") val iata: String?,
    @SerializedName("icao") val icao: String?,
    @SerializedName("number") val number: String?
)

data class LiveInfo(
    @SerializedName("latitude") val latitude: Double?,
    @SerializedName("longitude") val longitude: Double?,
    @SerializedName("altitude") val altitude: Double?,
    @SerializedName("direction") val direction: Double?,
    @SerializedName("speed_horizontal") val speed: Double?,
    @SerializedName("is_ground") val isGround: Boolean?
)

data class AirportInfo(
    @SerializedName("airport") val airport: String?,
    @SerializedName("iata") val iata: String?,
    @SerializedName("icao") val icao: String?,
    @SerializedName("terminal") val terminal: String?,
    @SerializedName("gate") val gate: String?,
    @SerializedName("delay") val delay: Int?,
    @SerializedName("scheduled") val scheduled: String?,
    @SerializedName("actual") val actual: String?
)

data class AirlineInfo(
    @SerializedName("name") val name: String?,
    @SerializedName("iata") val iata: String?,
    @SerializedName("icao") val icao: String?
)

data class AircraftInfo(
    @SerializedName("registration") val registration: String?,
    @SerializedName("iata") val iata: String?,
    @SerializedName("icao") val icao: String?
)

interface AviationStackApi {
    @GET("v1/flights")
    suspend fun getFlightByIATA(
        @RetrofitQuery("access_key") accessKey: String,
        @RetrofitQuery("flight_iata") flightIata: String
    ): AviationStackResponse

    @GET("v1/flights")
    suspend fun getFlightsByRoute(
        @RetrofitQuery("access_key") accessKey: String,
        @RetrofitQuery("dep_iata") departureIata: String,
        @RetrofitQuery("arr_iata") arrivalIata: String
    ): AviationStackResponse
}

object RetrofitInstance {
    private const val BASE_URL = "https://api.aviationstack.com/"

    val api: AviationStackApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AviationStackApi::class.java)
    }
}

// ============================
// ViewModel for Flight Tracking
// ============================

class FlightViewModel(application: Application) : AndroidViewModel(application) {
    var flightData by mutableStateOf<AviationFlightData?>(null)
    var error by mutableStateOf<String?>(null)
    var isTracking by mutableStateOf(false)
    private val apiKey = "47a4e80dcf8da59da398a40fca207017"

    fun fetchFlightByIATA(flightIata: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getFlightByIATA(apiKey, flightIata.trim())
                if (response.data.isNotEmpty()) {
                    flightData = response.data.first()
                    error = null
                } else {
                    flightData = null
                    error = "No flight data found."
                }
            } catch (e: Exception) {
                flightData = null
                error = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun startRealTimeTracking(flightIata: String) {
        if (isTracking) return
        isTracking = true
        viewModelScope.launch {
            while (isTracking) {
                fetchFlightByIATA(flightIata)
                delay(60000)
            }
        }
    }

    fun stopTracking() {
        isTracking = false
    }
}

// ============================
// UI Data Model for Display
// ============================
data class FlightDisplay(
    val flightIata: String,
    val airlineName: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val durationMinutes: Long
)

// ============================
// Room Database: Entity, DAO & Database
// ============================
@Entity(tableName = "flight_records")
data class FlightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flightIata: String,
    val departureIata: String,
    val arrivalIata: String,
    val flightDurationMinutes: Long,
    val recordedAt: Long
)

@Dao
interface FlightRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FlightRecordEntity)

    @Query("SELECT AVG(flightDurationMinutes) FROM flight_records WHERE departureIata = :depIata AND arrivalIata = :arrIata AND recordedAt >= :fromTimestamp")
    suspend fun getWeeklyAverageDurationForRoute(depIata: String, arrIata: String, fromTimestamp: Long): Double?

    // New query to fetch all records for a given route ordered by date (most recent first)
    @Query("SELECT * FROM flight_records WHERE departureIata = :depIata AND arrivalIata = :arrIata ORDER BY recordedAt DESC")
    suspend fun getFlightRecordsByRoute(depIata: String, arrIata: String): List<FlightRecordEntity>
}

@Database(entities = [FlightRecordEntity::class], version = 2, exportSchema = false)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun flightRecordDao(): FlightRecordDao

    companion object {
        @Volatile private var INSTANCE: FlightDatabase? = null

        fun getInstance(context: Context): FlightDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FlightDatabase::class.java,
                      "batman"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ============================
// Background Worker using WorkManager
// ============================
class FlightDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val apiKey = "47a4e80dcf8da59da398a40fca207017"
    private val departureRoute = "LAX"
    private val arrivalRoute = "JFK"

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("FlightDataWorker", "Worker started fetching data at ${System.currentTimeMillis()}")
                val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                val db = FlightDatabase.getInstance(applicationContext)
                val response = RetrofitInstance.api.getFlightsByRoute(apiKey, departureRoute, arrivalRoute)
                if (response.data.isNotEmpty()) {
                    val top3Flights = response.data.take(3)
                    for (flight in top3Flights) {
                        val depTimeStr = flight.departure?.actual ?: flight.departure?.scheduled
                        val arrTimeStr = flight.arrival?.actual ?: flight.arrival?.scheduled
                        if (depTimeStr != null && arrTimeStr != null) {
                            try {
                                val depTime = formatter.parse(depTimeStr)
                                val arrTime = formatter.parse(arrTimeStr)
                                if (depTime != null && arrTime != null) {
                                    val duration = (arrTime.time - depTime.time) / 60000
                                    val record = FlightRecordEntity(
                                        flightIata = flight.flight.iata ?: "N/A",
                                        departureIata = flight.departure?.iata?.uppercase() ?: "N/A",
                                        arrivalIata = flight.arrival?.iata?.uppercase() ?: "N/A",
                                        flightDurationMinutes = duration,
                                        recordedAt = System.currentTimeMillis()
                                    )
                                    db.flightRecordDao().insert(record)
                                    Log.d("FlightDataWorker", "Inserted record for flight: ${flight.flight.iata}")
                                }
                            } catch (e: Exception) {
                                Log.e("FlightDataWorker", "Time parsing error for flight ${flight.flight.iata}: ${e.localizedMessage}")
                            }
                        }
                    }
                } else {
                    Log.d("FlightDataWorker", "No flight data found for the route $departureRoute -> $arrivalRoute")
                }
                Result.success()
            } catch (e: Exception) {
                Log.e("FlightDataWorker", "Worker encountered an error: ${e.localizedMessage}")
                Result.failure()
            }
        }
    }
}

fun scheduleTestFlightDataWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = OneTimeWorkRequestBuilder<FlightDataWorker>()
        .setInitialDelay(20, TimeUnit.SECONDS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueue(request)
}

fun schedulePeriodicFlightDataWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val periodicRequest = PeriodicWorkRequestBuilder<FlightDataWorker>(24, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "PeriodicFlightDataWorker",
        ExistingPeriodicWorkPolicy.KEEP,
        periodicRequest
    )
}


// ============================
// Navigation & Composable Screens
// ============================

// Home Screen with navigation buttons.
@Composable
fun HomeScreen(navController: androidx.navigation.NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(colors = listOf(Color.Black, Color(0xFF1C1C2E)))
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Flight Tracker App", fontWeight = FontWeight.Bold, color = Color.Yellow, modifier = Modifier.padding(bottom = 16.dp))
        Button(
            onClick = { navController.navigate("tracking") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Track Flight", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { navController.navigate("topFlights") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Fetch Top 5 Flights", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Navigate to history screen for a default route (for example "LAX" to "JFK")
        Button(
            onClick = { navController.navigate("history/LAX/JFK") },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("View Flight History", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// Screen for tracking a flight (real-time updates).
@Composable
fun TrackFlightScreen(viewModel: FlightViewModel = viewModel(), onBack: () -> Unit) {
    var flightInput by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color.Black, Color(0xFF1C1C2E))))
            .padding(16.dp)
    ) {
        Text("Track a Friend's Flight", fontWeight = FontWeight.Bold, color = Color.Yellow)
        OutlinedTextField(
            value = flightInput,
            onValueChange = { flightInput = it },
            label = { Text("Enter Flight IATA Code", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.Gray,
                focusedLabelColor = Color.Yellow, unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Yellow, focusedBorderColor = Color.Yellow, unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!viewModel.isTracking) {
            Button(
                onClick = {
                    if (flightInput.isNotBlank()) viewModel.startRealTimeTracking(flightInput)
                    else viewModel.error = "Please enter a valid flight number"
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
            ) {
                Text("Start Tracking", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = { viewModel.stopTracking() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Stop Tracking", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        viewModel.error?.let { Text("Error: $it", color = Color.Red, fontStyle = FontStyle.Italic) }
        viewModel.flightData?.let { flight ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Flight IATA", flight.flight.iata ?: "N/A")
                    InfoRow("Status", flight.flightStatus)
                    InfoRow("Airline", flight.airline?.name ?: "N/A")
                    InfoRow("Aircraft Reg.", flight.aircraft?.registration ?: "N/A")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Departure", "${flight.departure?.airport ?: "N/A"} (${flight.departure?.iata ?: ""})")
                    InfoRow("Arrival", "${flight.arrival?.airport ?: "N/A"} (${flight.arrival?.iata ?: ""})")
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow("Latitude", flight.live?.latitude?.toString() ?: "N/A")
                    InfoRow("Longitude", flight.live?.longitude?.toString() ?: "N/A")
                    InfoRow("Altitude", "${flight.live?.altitude ?: "N/A"} m")
                    InfoRow("Speed", "${flight.live?.speed ?: "N/A"} km/h")
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Back", color = Color.White)
        }
    }
}

// Screen for fetching and showing Top 5 Flights for a route.
@Composable
fun TopFlightsScreen(onNavigateToHistory: (dep: String, arr: String) -> Unit, coroutineScope: kotlinx.coroutines.CoroutineScope = rememberCoroutineScope()) {
    var departureInput by remember { mutableStateOf("") }
    var arrivalInput by remember { mutableStateOf("") }
    var topFlights by remember { mutableStateOf<List<FlightDisplay>>(emptyList()) }
    var averageDuration by remember { mutableStateOf<Double?>(null) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val apiKey = "47a4e80dcf8da59da398a40fca207017"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color.Black, Color(0xFF1C1C2E))))
            .padding(16.dp)
    ) {
        Text("Enter Route for Top Flights", fontWeight = FontWeight.Bold, color = Color.Yellow)
        OutlinedTextField(
            value = departureInput,
            onValueChange = { departureInput = it },
            label = { Text("Enter Departure IATA Code", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.Gray,
                focusedLabelColor = Color.Yellow, unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Yellow, focusedBorderColor = Color.Yellow, unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = arrivalInput,
            onValueChange = { arrivalInput = it },
            label = { Text("Enter Arrival IATA Code", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White, unfocusedTextColor = Color.Gray,
                focusedLabelColor = Color.Yellow, unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Yellow, focusedBorderColor = Color.Yellow, unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Top 5 Flights & Average Duration for Route:", fontWeight = FontWeight.Bold, color = Color.Yellow)
        if (departureInput.isNotBlank() && arrivalInput.isNotBlank()) {
            Text("${departureInput.uppercase()} -> ${arrivalInput.uppercase()}", color = Color.Yellow, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (departureInput.isBlank() || arrivalInput.isBlank()) {
                    fetchError = "Please provide both departure and arrival IATA codes."
                } else {
                    fetchError = null
                    coroutineScope.launch {
                        try {
                            val response = RetrofitInstance.api.getFlightsByRoute(apiKey, departureInput.trim(), arrivalInput.trim())
                            if (response.data.isNotEmpty()) {
                                val flightDisplays = response.data.take(5).mapNotNull { flight ->
                                    val depTimeStr = flight.departure?.actual ?: flight.departure?.scheduled
                                    val arrTimeStr = flight.arrival?.actual ?: flight.arrival?.scheduled
                                    if (depTimeStr != null && arrTimeStr != null) {
                                        try {
                                            val depDate = formatter.parse(depTimeStr)
                                            val arrDate = formatter.parse(arrTimeStr)
                                            if (depDate != null && arrDate != null) {
                                                val duration = (arrDate.time - depDate.time) / 60000
                                                FlightDisplay(
                                                    flightIata = flight.flight.iata ?: "N/A",
                                                    airlineName = flight.airline?.name ?: "N/A",
                                                    departureAirport = flight.departure?.airport ?: "N/A",
                                                    arrivalAirport = flight.arrival?.airport ?: "N/A",
                                                    durationMinutes = duration
                                                )
                                            } else null
                                        } catch (e: Exception) {
                                            null
                                        }
                                    } else null
                                }
                                topFlights = flightDisplays
                                averageDuration = if (flightDisplays.isNotEmpty())
                                    flightDisplays.map { it.durationMinutes }.average() else null

                                if (flightDisplays.isEmpty()) {
                                    fetchError = "No valid flight data available."
                                }
                            } else {
                                fetchError = "No flight data found."
                            }
                        } catch (e: Exception) {
                            fetchError = "Error: ${e.localizedMessage}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Fetch Top 5 Flights", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        fetchError?.let { Text("Error: $it", color = Color.Red, fontStyle = FontStyle.Italic) }
        Spacer(modifier = Modifier.height(16.dp))
        if (topFlights.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                items(topFlights) { flight ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            InfoRow("Flight IATA", flight.flightIata)
                            InfoRow("Airline", flight.airlineName)
                            InfoRow("Departure", flight.departureAirport)
                            InfoRow("Arrival", flight.arrivalAirport)
                            InfoRow("Duration (min)", flight.durationMinutes.toString())
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            averageDuration?.let {
                Text("Average Flight Duration: ${"%.2f".format(it)} minutes",
                    fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (departureInput.isNotBlank() && arrivalInput.isNotBlank()) {
                Button(
                    onClick = { onNavigateToHistory(departureInput, arrivalInput) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                ) {
                    Text("View History for This Route", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Screen for displaying Flight History for a specified route.
@Composable
fun FlightHistoryScreen(depIata: String, arrIata: String, onBack: () -> Unit) {
    val context = LocalContext.current
    var flightRecords by remember { mutableStateOf<List<FlightRecordEntity>>(emptyList()) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    LaunchedEffect(depIata, arrIata) {
        val db = FlightDatabase.getInstance(context)
        flightRecords = db.flightRecordDao().getFlightRecordsByRoute(depIata.uppercase(), arrIata.uppercase())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(Color.Black, Color(0xFF1C1C2E))))
            .padding(16.dp)
    ) {
        Text("Flight History: ${depIata.uppercase()} -> ${arrIata.uppercase()}",
            fontWeight = FontWeight.Bold, color = Color.Yellow)
        Spacer(modifier = Modifier.height(8.dp))
        if (flightRecords.isEmpty()) {
            Text("No history available for this route.", color = Color.White)
        } else {
            LazyColumn {
                items(flightRecords) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            InfoRow("Flight IATA", record.flightIata)
                            InfoRow("Duration (min)", record.flightDurationMinutes.toString())
                            Text("Recorded at: ${dateFormatter.format(record.recordedAt)}",
                                color = Color.LightGray, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
        ) {
            Text("Back", color = Color.White)
        }
    }
}

// Reusable InfoRow Composable.
@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("• $label: ", fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = Color.White)
    }
}

// Preview for HomeScreen.
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BatmanTheme {
        // RememberNavController() must be called within a composable context.
        val navController = rememberNavController()
        HomeScreen(navController = navController)
    }
}

// ============================
// MainActivity with Navigation Setup
// ============================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Schedule background tasks.
        scheduleTestFlightDataWorker(this)
        schedulePeriodicFlightDataWorker(this)
        setContent {
            BatmanTheme {
                // Setup NavController here within the composable context.
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController = navController)
                    }
                    composable("tracking") {
                        TrackFlightScreen(onBack = { navController.navigate("home") })
                    }
                    composable("topFlights") {
                        TopFlightsScreen(onNavigateToHistory = { dep, arr ->
                            navController.navigate("history/${dep.uppercase()}/${arr.uppercase()}")
                        })
                    }
                    composable(
                        "history/{depIata}/{arrIata}",
                        arguments = listOf(
                            navArgument("depIata") { type = NavType.StringType },
                            navArgument("arrIata") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val depIata = backStackEntry.arguments?.getString("depIata") ?: "N/A"
                        val arrIata = backStackEntry.arguments?.getString("arrIata") ?: "N/A"
                        FlightHistoryScreen(depIata = depIata, arrIata = arrIata, onBack = { navController.navigate("home") })
                    }
                }
            }
        }
    }
}
