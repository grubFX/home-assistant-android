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
        var previousSensorRequestTime: Instant = Instant.now().minus(30, ChronoUnit.DAYS)
        val roundingMode = RoundingMode.HALF_EVEN

        // caloric sensors
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

        // mass / weight / fat sensors
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
        val bodyFat = SensorManager.BasicSensor(
            id = "health_connect_body_fat",
            type = "sensor",
            commonR.string.basic_sensor_name_body_fat,
            commonR.string.sensor_description_body_fat,
            "mdi:weight",
            unitOfMeasurement = "%",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
            deviceClass = "weight"
        )
        val leanBodyMass = SensorManager.BasicSensor(
            id = "health_connect_lean_body_mass",
            type = "sensor",
            commonR.string.basic_sensor_name_lean_body_mass,
            commonR.string.sensor_description_lean_body_mass,
            "mdi:weight",
            unitOfMeasurement = "kg",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
            deviceClass = "weight"
        )
        val boneMass = SensorManager.BasicSensor(
            id = "health_connect_bone_mass",
            type = "sensor",
            commonR.string.basic_sensor_name_bone_mass,
            commonR.string.sensor_description_bone_mass,
            "mdi:weight",
            unitOfMeasurement = "kg",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
            deviceClass = "weight"
        )
        val bodyWaterMass = SensorManager.BasicSensor(
            id = "health_connect_body_water_mass",
            type = "sensor",
            commonR.string.basic_sensor_name_body_water_mass,
            commonR.string.sensor_description_body_water_mass,
            "mdi:weight",
            unitOfMeasurement = "kg",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
            deviceClass = "weight"
        )

        // heart sensors
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

        // blood sensors
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
        val bloodGlucose = SensorManager.BasicSensor(
            id = "health_connect_blood_glucose",
            type = "sensor",
            commonR.string.basic_sensor_name_blood_glucose,
            commonR.string.sensor_description_blood_glucose,
            "mdi:water",
            unitOfMeasurement = "mg/dL",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )
        val bloodPressure = SensorManager.BasicSensor(
            id = "health_connect_blood_pressure",
            type = "sensor",
            commonR.string.basic_sensor_name_blood_pressure,
            commonR.string.sensor_description_blood_pressure,
            "mdi:water",
            unitOfMeasurement = "mmHg",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )

        // temperature sensors
        val basalBodyTemperature = SensorManager.BasicSensor(
            id = "health_connect_basal_body_temperature",
            type = "sensor",
            commonR.string.basic_sensor_name_basal_body_temperature,
            commonR.string.sensor_description_basal_body_temperature,
            "mdi:thermometer",
            unitOfMeasurement = "°C",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )
        val bodyTemperature = SensorManager.BasicSensor(
            id = "health_connect_body_temperature",
            type = "sensor",
            commonR.string.basic_sensor_name_body_temperature,
            commonR.string.sensor_description_body_temperature,
            "mdi:thermometer",
            unitOfMeasurement = "°C",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )
        val skinTemperature = SensorManager.BasicSensor(
            id = "health_connect_skin_temperature",
            type = "sensor",
            commonR.string.basic_sensor_name_skin_temperature,
            commonR.string.sensor_description_skin_temperature,
            "mdi:thermometer",
            unitOfMeasurement = "°C",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )

        // exercise / step / activity sensors
        val steps = SensorManager.BasicSensor(
            id = "health_connect_steps",
            type = "sensor",
            commonR.string.basic_sensor_name_steps,
            commonR.string.sensor_description_steps,
            "mdi:walk",
            unitOfMeasurement = "steps",
            entityCategory = SensorManager.ENTITY_CATEGORY_DIAGNOSTIC,
            updateType = SensorManager.BasicSensor.UpdateType.WORKER,
        )

        val distance = SensorManager.BasicSensor(
            id = "health_connect_distance",
            type = "sensor",
            commonR.string.basic_sensor_name_distance,
            commonR.string.sensor_description_distance,
            "mdi:map-marker-distance",
            unitOfMeasurement = "m",
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
            bloodGlucose.id -> arrayOf(HealthPermission.getReadPermission(BloodGlucoseRecord::class))
            bloodPressure.id -> arrayOf(HealthPermission.getReadPermission(BloodPressureRecord::class))
            basalBodyTemperature.id -> arrayOf(HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class))
            bodyTemperature.id -> arrayOf(HealthPermission.getReadPermission(BodyTemperatureRecord::class))
            skinTemperature.id -> arrayOf(HealthPermission.getReadPermission(SkinTemperatureRecord::class))
            bodyFat.id -> arrayOf(HealthPermission.getReadPermission(BodyFatRecord::class))
            leanBodyMass.id -> arrayOf(HealthPermission.getReadPermission(LeanBodyMassRecord::class))
            boneMass.id -> arrayOf(HealthPermission.getReadPermission(BoneMassRecord::class))
            bodyWaterMass.id -> arrayOf(HealthPermission.getReadPermission(BodyWaterMassRecord::class))
            steps.id -> arrayOf(HealthPermission.getReadPermission(StepsRecord::class))
            distance.id -> arrayOf(HealthPermission.getReadPermission(DistanceRecord::class))
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
        if (isEnabled(context, bloodGlucose)) {
            updateBloodGlucoseSensor(context, healthConnectClient)
        }
        if (isEnabled(context, bloodPressure)) {
            updateBloodPressureSensor(context, healthConnectClient)
        }
        if (isEnabled(context, basalBodyTemperature)) {
            updateBasalBodyTemperatureSensor(context, healthConnectClient)
        }
        if (isEnabled(context, bodyTemperature)) {
            updateBodyTemperatureSensor(context, healthConnectClient)
        }
        if (isEnabled(context, skinTemperature)) {
            updateSkinTemperatureSensor(context, healthConnectClient)
        }
        if (isEnabled(context, bodyFat)) {
            updateBodyFatSensor(context, healthConnectClient)
        }
        if (isEnabled(context, leanBodyMass)) {
            updateLeanBodyMassSensor(context, healthConnectClient)
        }
        if (isEnabled(context, boneMass)) {
            updateBoneMassSensor(context, healthConnectClient)
        }
        if (isEnabled(context, bodyWaterMass)) {
            updateBodyWaterMass(context, healthConnectClient)
        }
        if (isEnabled(context, steps)) {
            updateStepsSensor(context, healthConnectClient)
        }
        if (isEnabled(context, distance)) {
            updateDistanceSensor(context, healthConnectClient)
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
                BigDecimal(it.inKilocalories).setScale(2, roundingMode),
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
            BigDecimal(lastRecord.weight.inKilograms).setScale(3, roundingMode),
            weight.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
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
            BigDecimal(lastRecord.energy.inKilocalories).setScale(0, roundingMode),
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
            BigDecimal(lastRecord.samples.last().beatsPerMinute).setScale(0, roundingMode),
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
            BigDecimal(lastRecord.heartRateVariabilityMillis).setScale(0, roundingMode),
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
            BigDecimal(lastRecord.beatsPerMinute).setScale(0, roundingMode),
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
            BigDecimal(lastRecord.percentage.value).setScale(0, roundingMode),
            oxygenSaturation.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateBloodGlucoseSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BloodGlucoseRecord::class))
        }.records as List<BloodGlucoseRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            bloodGlucose,
            BigDecimal(lastRecord.level.inMilligramsPerDeciliter).setScale(0, roundingMode),
            bloodGlucose.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateBloodPressureSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BloodPressureRecord::class))
        }.records as List<BloodPressureRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        val systolic = lastRecord.systolic.inMillimetersOfMercury
        onSensorUpdated(
            context,
            bloodPressure,
            BigDecimal(systolic).setScale(0, roundingMode),
            bloodPressure.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
                "systolic" to BigDecimal(systolic).setScale(0),
                "diastolic" to BigDecimal(lastRecord.diastolic.inMillimetersOfMercury).setScale(0)
            )
        )
    }

    private fun updateBasalBodyTemperatureSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BasalBodyTemperatureRecord::class))
        }.records as List<BasalBodyTemperatureRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            basalBodyTemperature,
            BigDecimal(lastRecord.temperature.inCelsius).setScale(1, roundingMode),
            basalBodyTemperature.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
                "measurementLocation" to lastRecord.measurementLocation.toString()
            )
        )
    }

    private fun updateBodyTemperatureSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BodyTemperatureRecord::class))
        }.records as List<BodyTemperatureRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            bodyTemperature,
            BigDecimal(lastRecord.temperature.inCelsius).setScale(1, roundingMode),
            bodyTemperature.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
                "measurementLocation" to lastRecord.measurementLocation.toString()
            )
        )
    }

    private fun updateSkinTemperatureSensor(context: Context, healthConnectClient: HealthConnectClient) {
// baseline & deltas are (currently?) only accessible inside the library
//        val records = runBlocking {
//            healthConnectClient.readRecords(buildReadRecordsRequest(SkinTemperatureRecord::class))
//        }.records as List<SkinTemperatureRecord>
//        if (records.isEmpty()) {
//            return
//        }
//        val lastRecord = records.last()
//        onSensorUpdated(
//            context,
//            skinTemperature,
//            BigDecimal(lastRecord.baseline.inCelsius).setScale(1, roundingMode),
//            skinTemperature.statelessIcon,
//            attributes = mapOf(
//                "startTime" to lastRecord.startTime,
//                "startZoneOffset" to lastRecord.startZoneOffset,
//                "endTime" to lastRecord.endTime,
//                "endZoneOffset" to lastRecord.endZoneOffset
//                "measurementLocation" to lastRecord.measurementLocation.toString()
//            )
//        )
    }

    private fun updateBodyFatSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BodyFatRecord::class))
        }.records as List<BodyFatRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            bodyFat,
            BigDecimal(lastRecord.percentage.value).setScale(1, roundingMode),
            bodyFat.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateLeanBodyMassSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(LeanBodyMassRecord::class))
        }.records as List<LeanBodyMassRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            leanBodyMass,
            BigDecimal(lastRecord.mass.inKilograms).setScale(3, roundingMode),
            leanBodyMass.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateBoneMassSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BoneMassRecord::class))
        }.records as List<BoneMassRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            boneMass,
            BigDecimal(lastRecord.mass.inKilograms).setScale(3, roundingMode),
            boneMass.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateBodyWaterMass(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(BodyWaterMassRecord::class))
        }.records as List<BodyWaterMassRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            bodyWaterMass,
            BigDecimal(lastRecord.mass.inKilograms).setScale(3, roundingMode),
            bodyWaterMass.statelessIcon,
            attributes = mapOf(
                "time" to lastRecord.time,
                "zoneOffset" to lastRecord.zoneOffset,
            )
        )
    }

    private fun updateStepsSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(StepsRecord::class))
        }.records as List<StepsRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            steps,
            BigDecimal(lastRecord.count).setScale(0, roundingMode),
            steps.statelessIcon,
            attributes = mapOf(
                "startTime" to lastRecord.startTime,
                "startZoneOffset" to lastRecord.startZoneOffset,
                "endTime" to lastRecord.endTime,
                "endZoneOffset" to lastRecord.endZoneOffset
            )
        )
    }

    private fun updateDistanceSensor(context: Context, healthConnectClient: HealthConnectClient) {
        val records = runBlocking {
            healthConnectClient.readRecords(buildReadRecordsRequest(DistanceRecord::class))
        }.records as List<DistanceRecord>
        if (records.isEmpty()) {
            return
        }
        val lastRecord = records.last()
        onSensorUpdated(
            context,
            distance,
            BigDecimal(lastRecord.distance.inMeters).setScale(0, roundingMode),
            distance.statelessIcon,
            attributes = mapOf(
                "startTime" to lastRecord.startTime,
                "startZoneOffset" to lastRecord.startZoneOffset,
                "endTime" to lastRecord.endTime,
                "endZoneOffset" to lastRecord.endZoneOffset
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
                oxygenSaturation,
                bloodGlucose,
                bloodPressure,
                basalBodyTemperature,
                bodyTemperature,
                bodyFat,
                leanBodyMass,
                boneMass,
                bodyWaterMass,
                steps,
                distance
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
