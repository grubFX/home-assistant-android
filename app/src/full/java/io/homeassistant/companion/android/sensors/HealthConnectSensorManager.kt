package io.homeassistant.companion.android.sensors

import android.content.Context
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import io.homeassistant.companion.android.common.sensors.SensorManager
import kotlinx.coroutines.runBlocking
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass
import io.homeassistant.companion.android.common.R as commonR

class HealthConnectSensorManager : SensorManager {
    companion object {
        var previousSensorRequestTime: Instant = Instant.now().minus(30, ChronoUnit.DAYS);

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

        val heartRateVariability = SensorManager.BasicSensor(
            id = "health_connect_heart_rate_variability",
            type = "sensor",
            commonR.string.basic_sensor_name_heart_rate_variability,
            commonR.string.sensor_description_heart_rate_variability,
            "mdi:heart-pulse",
            unitOfMeasurement = "ms",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )

        val restingHeartRate = SensorManager.BasicSensor(
            id = "health_connect_resting_heart_rate",
            type = "sensor",
            commonR.string.basic_sensor_name_resting_heart_rate,
            commonR.string.sensor_description_resting_heart_rate,
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
            "mdi:water-opacity",
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
            heartRateVariability.id -> arrayOf(HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class))
            restingHeartRate.id -> arrayOf(HealthPermission.getReadPermission(RestingHeartRateRecord::class))
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
        if (isEnabled(context, heartRateVariability)) {
            updateHeartRateVariabilitySensor(context, healthConnectClient)
        }
        if (isEnabled(context, restingHeartRate)) {
            updateRestingHeartRateSensor(context, healthConnectClient)
        }
        if (isEnabled(context, oxygenSaturation)) {
            updateOxygenSaturationSensor(context, healthConnectClient)
        }

        previousSensorRequestTime = Instant.now()
    }

    private fun buildReadRecordsRequest(
        recordType: KClass<out Record>,
    ): ReadRecordsRequest<out Record> {
        return ReadRecordsRequest(
            recordType,
            timeRangeFilter = TimeRangeFilter.between(
                previousSensorRequestTime,
                Instant.now()
            ),
            ascendingOrder = false,
            pageSize = 1
        )
    }

    private fun <T : Any> buildAggregateRequest(
        aggregateMetric: AggregateMetric<T>,
        startTime: LocalTime = LocalTime.MIDNIGHT
    ): AggregateRequest {
        return AggregateRequest(
            metrics = setOf(aggregateMetric),
            timeRangeFilter = TimeRangeFilter.between(
                LocalDateTime.of(LocalDate.now(), startTime),
                LocalDateTime.of(LocalDate.now(), LocalTime.now())
            )
        )
    }

    private fun updateTotalCaloriesBurnedSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val totalCaloriesBurnedRequest = runBlocking {
            healthConnectClient.aggregate(buildAggregateRequest(TotalCaloriesBurnedRecord.ENERGY_TOTAL))
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
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(WeightRecord::class))
        }.records as List<WeightRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            weight,
            BigDecimal(lastRecord.weight.inKilograms).setScale(3, RoundingMode.HALF_EVEN),
            weight.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "oneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateActiveCaloriesBurnedSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(ActiveCaloriesBurnedRecord::class))
        }.records as List<ActiveCaloriesBurnedRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            activeCaloriesBurned,
            BigDecimal(lastRecord.energy.inKilocalories).setScale(2, RoundingMode.HALF_EVEN),
            activeCaloriesBurned.statelessIcon,
            attributes = mapOf(
                "startTime" to lastRecord.startTime,
                "startZoneOffset" to lastRecord.startZoneOffset,
                "endTime" to lastRecord.endTime,
                "endZoneOffset" to lastRecord.endZoneOffset
            )
        )
    }

    private fun updateHeartRateSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(HeartRateRecord::class))
        }.records as List<HeartRateRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            heartRate,
            BigDecimal(lastRecord.samples.last().beatsPerMinute),
            heartRate.statelessIcon,
            attributes = mapOf(
                "startTime" to lastRecord.startTime,
                "startZoneOffset" to lastRecord.startZoneOffset,
                "endTime" to lastRecord.endTime,
                "endZoneOffset" to lastRecord.endZoneOffset
            )
        )
    }

    private fun updateHeartRateVariabilitySensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(HeartRateVariabilityRmssdRecord::class))
        }.records as List<HeartRateVariabilityRmssdRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            heartRateVariability,
            BigDecimal(lastRecord.heartRateVariabilityMillis),
            heartRateVariability.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateRestingHeartRateSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(RestingHeartRateRecord::class))
        }.records as List<RestingHeartRateRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            restingHeartRate,
            BigDecimal(lastRecord.beatsPerMinute),
            restingHeartRate.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateOxygenSaturationSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(OxygenSaturationRecord::class))
        }.records as List<OxygenSaturationRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            oxygenSaturation,
            BigDecimal(lastRecord.percentage.value),
            oxygenSaturation.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    override suspend fun getAvailableSensors(context: Context): List<SensorManager.BasicSensor> {
        return if (hasSensor(context)) {
            listOf(
                activeCaloriesBurned,
                totalCaloriesBurned,
                weight,
                heartRate,
                heartRateVariability,
                restingHeartRate,
                oxygenSaturation
            )
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
