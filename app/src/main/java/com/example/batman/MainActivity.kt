package com.example.batman

import android.app.Application
import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.*
import androidx.work.*
import com.example.batman.ui.theme.BatmanTheme
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
// Alias Retrofit’s Query annotation to avoid conflict with Room’s Query.
import retrofit2.http.GET
import retrofit2.http.Query as RetrofitQuery
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// ============================
// Data Models & Retrofit API
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

    // New endpoint call to get flights by route.
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
// ViewModel for Flight Tracking (Q1)
// ============================

class FlightViewModel(application: Application) : AndroidViewModel(application) {
    var flightData by mutableStateOf<AviationFlightData?>(null)
    var error by mutableStateOf<String?>(null)
    var isTracking by mutableStateOf(false)
    private val apiKey = "e58a2c66b107d12edd139fb0d46e3fdf"

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
// Data model for UI display of flight details
// ============================
data class FlightDisplay(
    val flightIata: String,
    val airlineName: String,
    val departureAirport: String,
    val arrivalAirport: String,
    val durationMinutes: Long
)

// ============================
// UI for Flight Tracker & Top 5 Flight Data with Average Duration
// ============================

@Composable
fun FlightTrackerScreen(
    modifier: Modifier = Modifier,
    viewModel: FlightViewModel = viewModel()
) {
    var flightInput by remember { mutableStateOf("") }
    // --- Q1: Flight tracking remains unchanged ---
    // --- NEW: Top 5 Flight Data for a specific route (LAX -> JFK) ---
    val departureRoute = "LAX"
    val arrivalRoute = "JFK"
    // States for top flights and average duration from API call.
    var topFlights by remember { mutableStateOf<List<FlightDisplay>>(emptyList()) }
    var averageDuration by remember { mutableStateOf<Double?>(null) }
    var fetchError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    // API key for calls (reuse the same key)
    val apiKey = "e58a2c66b107d12edd139fb0d46e3fdf"
    // Formatter to parse the time strings.
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black, Color(0xFF1C1C2E))
                )
            )
            .padding(16.dp)
    ) {
        // --------------------- Q1: Flight Tracking UI ---------------------
        Text("Track a Friend's Flight", fontWeight = FontWeight.Bold, color = Color.Yellow)
        OutlinedTextField(
            value = flightInput,
            onValueChange = { flightInput = it },
            label = { Text("Enter Flight IATA Code", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.Gray,
                focusedLabelColor = Color.Yellow,
                unfocusedLabelColor = Color.Gray,
                cursorColor = Color.Yellow,
                focusedBorderColor = Color.Yellow,
                unfocusedBorderColor = Color.Gray
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (!viewModel.isTracking) {
            Button(
                onClick = {
                    if (flightInput.isBlank()) {
                        viewModel.error = "Please enter a valid flight number"
                    } else {
                        viewModel.startRealTimeTracking(flightInput)
                    }
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
                    InfoRow("Aircraft Registration", flight.aircraft?.registration ?: "N/A")
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
        Spacer(modifier = Modifier.height(32.dp))
        // --------------------- NEW: Top 5 Flight Data & Average Duration for Route ---------------------
        Text("Top 5 Flights & Average Duration for Route:", fontWeight = FontWeight.Bold, color = Color.Yellow)
        Text("$departureRoute -> $arrivalRoute", color = Color.Yellow, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                fetchError = null
                // Fetch flight data from the API.
                coroutineScope.launch {
                    try {
                        val response = RetrofitInstance.api.getFlightsByRoute(apiKey, departureRoute, arrivalRoute)
                        if (response.data.isNotEmpty()) {
                            // Process top 5 flights.
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
                                flightDisplays.map { it.durationMinutes }.average()
                            else null

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
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
        ) {
            Text("Fetch Top 5 Flights", color = Color.Black, fontWeight = FontWeight.Bold)
        }
        fetchError?.let { Text("Error: $it", color = Color.Red, fontStyle = FontStyle.Italic) }
        Spacer(modifier = Modifier.height(16.dp))
        // Display details of top flights in a scrollable LazyColumn.
        if (topFlights.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
            ) {
                items(topFlights) { flight ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            InfoRow("Flight IATA", flight.flightIata)
                            InfoRow("Airline", flight.airlineName)
                            InfoRow("Departure Airport", flight.departureAirport)
                            InfoRow("Arrival Airport", flight.arrivalAirport)
                            InfoRow("Duration (min)", flight.durationMinutes.toString())
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            averageDuration?.let {
                Text(
                    "Average Flight Duration: ${"%.2f".format(it)} minutes",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("• $label: ", fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.width(4.dp))
        Text(value, color = Color.White)
    }
}

// ============================
// (Existing Room/WorkManager Code Remains Unchanged)
// ============================

// --- Room Entity ---
@Entity(tableName = "flight_records")
data class FlightRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flightIata: String,
    val departureIata: String,
    val arrivalIata: String,
    // Flight duration in minutes (actual time including delays)
    val flightDurationMinutes: Long,
    val recordedAt: Long // Timestamp in milliseconds
)

// --- DAO ---
@Dao
interface FlightRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FlightRecordEntity)

    @Query("SELECT AVG(flightDurationMinutes) FROM flight_records WHERE departureIata = :depIata AND arrivalIata = :arrIata AND recordedAt >= :fromTimestamp")
    suspend fun getWeeklyAverageDurationForRoute(depIata: String, arrIata: String, fromTimestamp: Long): Double?
}

// --- Room Database ---
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
                    "fl_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// --- Background Worker using WorkManager ---
class FlightDataWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val apiKey = "e58a2c66b107d12edd139fb0d46e3fdf"
    private val departureRoute = "LAX"
    private val arrivalRoute = "JFK"

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
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
                                }
                            } catch (e: Exception) {
                                // Skip record if time parsing fails.
                            }
                        }
                    }
                }
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }
}

fun scheduleWeeklyFlightDataWorker(context: Context) {
    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = PeriodicWorkRequestBuilder<FlightDataWorker>(24, TimeUnit.HOURS)
        .setConstraints(constraints)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "WeeklyFlightDataWorker",
        ExistingPeriodicWorkPolicy.KEEP,
        request
    )
}

// ============================
// MainActivity with WorkManager scheduling
// ============================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        scheduleWeeklyFlightDataWorker(this)
        setContent {
            BatmanTheme {
                FlightTrackerScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ============================
// Preview
// ============================
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    BatmanTheme {
        FlightTrackerScreen(modifier = Modifier.fillMaxSize())
    }
}
