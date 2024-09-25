package io.homeassistant.companion.android.sensors

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.homeassistant.companion.android.common.R as commonR
import io.homeassistant.companion.android.common.sensors.SensorManager
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking

class HealthConnectSensorManager : SensorManager {
    companion object {
        val activeCaloriesBurned = SensorManager.BasicSensor(
            id = "health_connect_active_calories_burned",
            type = "sensor",
            commonR.string.basic_sensor_name_active_calories_burned,
            commonR.string.sensor_description_active_calories_burned,
            "mdi:fire",
            unitOfMeasurement = "kcal",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER
        )

        val totalCaloriesBurned = SensorManager.BasicSensor(
            id = "health_connect_total_calories_burned",
            type = "sensor",
            commonR.string.basic_sensor_name_total_calories_burned,
            commonR.string.sensor_description_total_calories_burned,
            "mdi:fire",
            unitOfMeasurement = "kcal",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER
        )

        val weight = SensorManager.BasicSensor(
            id = "health_connect_weight",
            type = "sensor",
            commonR.string.basic_sensor_name_weight,
            commonR.string.sensor_description_weight,
            "mdi:weight",
            unitOfMeasurement = "kg",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
            deviceClass = "weight"
        )

        val heartRate = SensorManager.BasicSensor(
            id = "health_connect_heart_rate",
            type = "sensor",
            commonR.string.basic_sensor_name_heart_rate,
            commonR.string.sensor_description_heart_rate,
            "mdi:heart-pulse",
            unitOfMeasurement = "bpm",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )

        val oxygenSaturation = SensorManager.BasicSensor(
            id = "health_connect_oxygen_saturation",
            type = "sensor",
            commonR.string.basic_sensor_name_oxygen_saturation,
            commonR.string.sensor_description_oxygen_saturation,
            "mdi:heart-plus-outline",
            unitOfMeasurement = "%",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )
    }

    override val name: Int
        get() = commonR.string.sensor_name_health_connect

    override fun requiredPermissions(sensorId: String): Array<String> {
        return when (sensorId) {
            activeCaloriesBurned.id -> arrayOf(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
            totalCaloriesBurned.id -> arrayOf(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
            weight.id -> arrayOf(HealthPermission.getReadPermission(WeightRecord::class))
            heartRate.id -> arrayOf(HealthPermission.getReadPermission(HeartRateRecord::class))
            oxygenSaturation.id -> arrayOf(HealthPermission.getReadPermission(OxygenSaturationRecord::class))
            else -> arrayOf()
        }
    }

    override fun requestSensorUpdate(context: Context) {
        val healthConnectClient: HealthConnectClient = HealthConnectClient.getOrCreate(context)

        if (isEnabled(context, weight)) {
            updateWeightSensor(context, healthConnectClient)
        }
        if (isEnabled(context, activeCaloriesBurned)) {
            updateActiveCaloriesBurnedSensor(context, healthConnectClient)
        }
        if (isEnabled(context, totalCaloriesBurned)) {
            updateTotalCaloriesBurnedSensor(context, healthConnectClient)
        }
        if (isEnabled(context, heartRate)) {
            updateHeartRateSensor(context, healthConnectClient)
        }
        if (isEnabled(context, oxygenSaturation)) {
            updateOxygenSaturationSensor(context, healthConnectClient)
        }
    }

    private fun updateTotalCaloriesBurnedSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val totalCaloriesBurnedRequest = runBlocking {
            healthConnectClient.aggregate(
                AggregateRequest(
                    metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(
                        LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT),
                        LocalDateTime.of(LocalDate.now(), LocalTime.now())
                    )
                )
            )
        }
        totalCaloriesBurnedRequest[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.let {
            onSensorUpdated(
                context,
                totalCaloriesBurned,
                BigDecimal(it.inKilocalories).setScale(2, RoundingMode.HALF_EVEN),
                totalCaloriesBurned.statelessIcon,
                attributes = mapOf("endTime" to LocalDateTime.of(LocalDate.now(), LocalTime.now()).toInstant(ZoneOffset.UTC))
            )
        }
    }

    private fun updateWeightSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val weightRequest = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now()
            ),
            ascendingOrder = false,
            pageSize = 1
        )
        val response = runBlocking { healthConnectClient.readRecords(weightRequest) }
        onSensorUpdated(
            context,
            weight,
            BigDecimal(response.records.last().weight.inKilograms).setScale(3, RoundingMode.HALF_EVEN),
            weight.statelessIcon,
            attributes = mapOf("date" to response.records.last().time)
        )
    }

    private fun updateActiveCaloriesBurnedSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val activeCaloriesBurnedRequest = ReadRecordsRequest(
            recordType = ActiveCaloriesBurnedRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now()

            ),
            ascendingOrder = false,
            pageSize = 1
        )
        val response = runBlocking { healthConnectClient.readRecords(activeCaloriesBurnedRequest) }
        if (response.records.isEmpty()) {
            return
        }
        onSensorUpdated(
            context,
            activeCaloriesBurned,
            BigDecimal(response.records.last().energy.inKilocalories).setScale(2, RoundingMode.HALF_EVEN),
            activeCaloriesBurned.statelessIcon,
            attributes = mapOf("endTime" to response.records.last().endTime)
        )
    }

    private fun updateHeartRateSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val heartRateRequest = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now()
            ),
            ascendingOrder = false,
            pageSize = 1
        )
        val response = runBlocking { healthConnectClient.readRecords(heartRateRequest) }
        if (response.records.isEmpty()) {
            return
        }
        onSensorUpdated(
            context,
            heartRate,
            BigDecimal(response.records.last().samples.last().beatsPerMinute),
            heartRate.statelessIcon,
            attributes = mapOf("endTime" to response.records.last().endTime)
        )
    }

    private fun updateOxygenSaturationSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val oxygenSaturationRequest = ReadRecordsRequest(
            recordType = OxygenSaturationRecord::class,
            timeRangeFilter = TimeRangeFilter.between(
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now()
            ),
            ascendingOrder = false,
            pageSize = 1
        )
        val response = runBlocking { healthConnectClient.readRecords(oxygenSaturationRequest) }
        if (response.records.isEmpty()) {
            return
        }
        onSensorUpdated(
            context,
            oxygenSaturation,
            response.records.last().percentage,
            oxygenSaturation.statelessIcon,
            attributes = mapOf("endTime" to response.records.last().time)
        )
    }

    override suspend fun getAvailableSensors(context: Context): List<SensorManager.BasicSensor> {
        return if (hasSensor(context)) {
            listOf(weight, activeCaloriesBurned, totalCaloriesBurned)
        } else {
            emptyList()
        }
    }

    override fun hasSensor(context: Context): Boolean {
        return SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    override fun checkPermission(context: Context, sensorId: String): Boolean {
        val healthConnectClient = HealthConnectClient.getOrCreate(context)
        val result = runBlocking {
            healthConnectClient.permissionController.getGrantedPermissions().containsAll(requiredPermissions(sensorId).toSet())
        }
        return result
    }
}
