package com.example.batterymonitor

import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.content.Context
import android.graphics.Color
import android.view.WindowManager
import android.widget.ImageView
import kotlinx.android.synthetic.main.activity_main.*
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import java.io.*
import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.os.Environment
import android.widget.Button
import android.widget.ProgressBar
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.opencsv.CSVWriter
import com.opencsv.bean.ColumnPositionMappingStrategy
import com.opencsv.bean.StatefulBeanToCsv
import com.opencsv.bean.StatefulBeanToCsvBuilder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.jvm.internal.impl.types.TypeCapabilities


class powerMeasurement {
    var timestamp:Long = 0
    var battery_status:Int = 0
    var battery_charging:Int = 0
    var battery_capacity:Int = 0
    var battery_cc:Int = 0
    var battery_currentAvg:Int = 0
    var battery_currentNow:Int = 0
    var battery_energyCount:Long = 0
    var battery_technology:String? = null
    var battery_temp:Int = 0
    var battery_voltage:Int = 0
    var battery_health:Int = 0
    var displayState: String? = null

    constructor() {}
    constructor(
                battery_status:Int,
                battery_charging:Int,
                battery_capacity:Int,
                battery_cc:Int,
                battery_currentAvg:Int,
                battery_currentNow:Int,
                battery_energyCount:Long,
                battery_technology:String?,
                battery_temp:Int,
                battery_voltage:Int,
                battery_health:Int,
                displayState:String?) {

        this.timestamp = (System.currentTimeMillis()/1000).toLong()
        this.battery_status = battery_status
        this.battery_charging = battery_charging
        this.battery_capacity = battery_capacity
        this.battery_cc = battery_cc
        this.battery_currentAvg = battery_currentAvg
        this.battery_currentNow = battery_currentNow
        this.battery_energyCount = battery_energyCount
        this.battery_technology = battery_technology
        this.battery_temp = battery_temp
        this.battery_voltage = battery_voltage
        this.battery_health = battery_health
        this.displayState = displayState
    }
}


class experimentConfig {
    var startTime_timestamp:Long = 0
    var endTime_timestamp:Long = 0

    var startTime_time:String? = null
    var endTime_time:String? = null

    var startDate:String? = null
    var endDate:String? = null

    var deviceName:String? = null

    var log_rate:Int = 0
    var experiemnt_rate:Int = 0
    var experiemnt_log:String? = null
    var experiemnt_list:String? = null
    var experiemnt_name:String? = null

}

val nullMeasurement = powerMeasurement(
                                    battery_status=0,
                                    battery_charging=0,
                                    battery_capacity=0,
                                    battery_cc=0,
                                    battery_currentAvg=0,
                                    battery_currentNow=0,
                                    battery_energyCount=0,
                                    battery_technology=null,
                                    battery_temp=0,
                                    battery_voltage=0,
                                    battery_health=0,
                                    displayState=null)

//
//fun loadConfigFile(path: Path): experimentConfig {
//    val mapper = ObjectMapper(YAMLFactory()) // Enable YAML parsing
//    mapper.registerModule(KotlinModule()) // Enable Kotlin support
////    fileReader = BufferedReader(testConfigReader)
//
//    return Files.newBufferedReader(path).use {
//        mapper.readValue(it, experimentConfig::class.java)
//    }
//}

class MainActivity : AppCompatActivity() {
    private var expListText:String = ""
    private val CSV_HEADER = arrayOf<String>("timestamp","status","charging","capacity","cc","currentAvg","currentNow",
                                        "energyCount","technology","temp","voltage","health","displayState")

    var measurementList = mutableListOf(nullMeasurement)
    var experimentID = 0
    var experimentStartTime: Long = 0
    //    var experimentsToRun : MutableList<Array<String>>? = null
    var experimentsToRun = listOf(arrayOf("sp","-"))
//    var configData = loadConfigFile(getExternalFilesDir("measurementLogs") + "config")
    val tempStabiliseTime:Long = 1_000

    val logCounter = Thread{
        Thread.sleep(tempStabiliseTime) //wait for 30 seconds for temperature to stablise
        val batchSize = 10
        val counterPeriod_ms:Long = 200
        var measurementCount = 0
        var totalBatches = 0
        while (true) {
            //Make measurement.
            if (measurementCount == 0 && totalBatches > 0){
                measurementList = mutableListOf(getPowerMeasurement(displayState = "e_" + experimentID.toString()))
            } else {
                measurementList.add(getPowerMeasurement(displayState = "e_" + experimentID.toString()))
            }
            measurementCount++

            if (measurementCount == batchSize || experimentID == -1) {
                if (totalBatches == 0) {
                    //Create new file and write data
                    wtiteLogEntriesToFile(false)
                } else {
                    //open file and write data
                    wtiteLogEntriesToFile(true)
                }
                measurementCount = 0
                totalBatches++
            }

            try {
                Thread.sleep(counterPeriod_ms)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    val experimentCounter = Thread{
        Thread.sleep(tempStabiliseTime) //wait for 30 seconds for temperature to stablise
        val counterPeriod_ms:Long = 60_000
        val baseLinePeriod_ms:Long = 60_000
//        val counterPeriod_ms:Long = 100

        var experimentCount = 0
        var showTest = true
        val display = findViewById<ImageView>(R.id.displayContainer)
        val expButton = findViewById<Button>(R.id.mainButton)
//        var rgbValues = arrayListOf<Int>((0,0,0))
        // val assetManager = assets
        // val files = assetManager.list("inputConfig")
        // println(files[0])

        while (true) {
            //get experiment by ID and set display as required.
            println(experimentsToRun[experimentID][0].toString() + " | " + experimentsToRun[experimentID][1].toString())
            if (showTest) {
                if (experimentsToRun[experimentID][0] == "rgb") {
                    var rgbValues = experimentsToRun[experimentID][1].split("-")
                    display.post{ display.setBackgroundColor(Color.rgb(rgbValues[0].toInt(), rgbValues[1].toInt(), rgbValues[2].toInt()))}
                    if (display.getDrawable() != null) {
                        display.post{ display.setImageDrawable(null)}
                    }
                } else if (experimentsToRun[experimentID][0] == "image"){
//                    display.post{ display.setBackgroundColor(Color.rgb(0,0,0))}
                     println("experimentCounter hit!")
//                    var imgPath = getExternalFilesDir("inputImages").toString()
//                    var logFile:File = File(getExternalFilesDir("measurementLogs"),fileName)

                    var imageFile:File

                    if (experimentsToRun[experimentID][1].contains("."))
                    {
                        imageFile = File(getExternalFilesDir("inputImages"),experimentsToRun[experimentID][1])
//                        imgPath += "/"+ experimentsToRun[experimentID][1];
//                        display.post{ display.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("inputImages/" +experimentsToRun[experimentID][1])))}
                    }else {
                        imageFile = File(getExternalFilesDir("inputImages"),experimentsToRun[experimentID][1]+".png")
//                        imgPath += "/"+ experimentsToRun[experimentID][1] + ".png";

//                        display.post{ display.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("inputImages/" +experimentsToRun[experimentID][1] + ".png")))}
                    }

                    print(imageFile.toString())
                    display.post{ display.setImageBitmap(BitmapFactory.decodeStream(imageFile.inputStream()))}

//                    File file = new File(path);
//                    FileInputStream fileInputStream = new FileInputStream(file);


                } else {
                    if (display.getDrawable() != null) {
                        display.post{ display.setImageDrawable(null)}
                    }
                    display.post{ display.setBackgroundColor(Color.rgb(255,255,255))}
                }
            } else {
                if (experimentsToRun[0][0] == "rgb") {
                    var rgbValues = experimentsToRun[0][1].split("-")
                    display.post{ display.setBackgroundColor(Color.rgb(rgbValues[0].toInt(), rgbValues[1].toInt(), rgbValues[2].toInt()))}
                    if (display.getDrawable() != null) {
                        display.post{ display.setImageDrawable(null)}
                    }
                } else if (experimentsToRun[0][0] == "image"){
//                    display.post{ display.setBackgroundColor(Color.rgb(0,0,0))}
                    println("experimentCounter hit!")
//                    var imgPath = getExternalFilesDir("inputImages").toString()
//                    var logFile:File = File(getExternalFilesDir("measurementLogs"),fileName)

                    var imageFile:File

                    if (experimentsToRun[0][1].contains("."))
                    {
                        imageFile = File(getExternalFilesDir("inputImages"),experimentsToRun[0][1])
//                        imgPath += "/"+ experimentsToRun[0][1];
//                        display.post{ display.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("inputImages/" +experimentsToRun[0][1])))}
                    }else {
                        imageFile = File(getExternalFilesDir("inputImages"),experimentsToRun[0][1]+".png")
//                        imgPath += "/"+ experimentsToRun[0][1] + ".png";

//                        display.post{ display.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("inputImages/" +experimentsToRun[0][1] + ".png")))}
                    }

                    print(imageFile.toString())
                    display.post{ display.setImageBitmap(BitmapFactory.decodeStream(imageFile.inputStream()))}

//                    File file = new File(path);
//                    FileInputStream fileInputStream = new FileInputStream(file);


                } else {
                    if (display.getDrawable() != null) {
                        display.post{ display.setImageDrawable(null)}
                    }
                    display.post{ display.setBackgroundColor(Color.rgb(255,255,255))}
                }
//                display.post{ display.setBackgroundColor(Color.rgb(0,0,0))}
//                if (display.getDrawable() != null) {
//                    display.post{ display.setImageDrawable(null)}
//                }
            }


            try {
                if (showTest) {
                    Thread.sleep(counterPeriod_ms)
                } else {
                    Thread.sleep(baseLinePeriod_ms)
                }

                showTest = !showTest
                if (showTest){
                    experimentCount++
                    experimentID = experimentCount
                } else {
                    experimentID = 0
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }


            if (experimentCount == experimentsToRun.size){
                experimentID = -1
                showTest = true
                println("END of TESTs")
                display.post{ display.setBackgroundColor(Color.rgb(0,0,0))}
                if (display.getDrawable() != null) {
                    display.post{ display.setImageDrawable(null)}
                }
                Thread.sleep(1_000)

                display.post{ display.setBackgroundColor(Color.rgb(255,255,255))}
                if (display.getDrawable() != null) {
                    display.post{ display.setImageDrawable(null)}
                }
                Thread.sleep(1_000)


                display.post{ display.setBackgroundColor(Color.rgb(0,0,0))}
                if (display.getDrawable() != null) {
                    display.post{ display.setImageDrawable(null)}
                }
                Thread.sleep(1_000)


                display.post{ display.setBackgroundColor(Color.rgb(255,255,255))}
                if (display.getDrawable() != null) {
                    display.post{ display.setImageDrawable(null)}
                }
                Thread.sleep(1_000)

                experimentID = experimentCount++

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val expList = findViewById<TextView>(R.id.expList)
//        println(configData)

        experimentsToRun = loadExperiments()
        for (experiment in experimentsToRun) {
            println(experiment[0] + " - " + experiment[1])
        }
        expList.setText(expListText)
    }

    fun loadExperiments(): List<Array<String>>{
        var fileReader: BufferedReader? = null
        var csvReader: CSVReader? = null
        var incremental = false
        val records:List<Array<String>>

        try {

//            val testConfigReader = InputStreamReader(getAssets().open("inputConfig/testConfig.csv"), StandardCharsets.UTF_8)
            val testConfigReader = InputStreamReader(File(getExternalFilesDir("inputConfig"),"testConfig.csv").inputStream(), StandardCharsets.UTF_8)

            fileReader = BufferedReader(testConfigReader)

            if (incremental) {
                csvReader = CSVReader(fileReader)
                var record: Array<String>?
                csvReader.readNext() // skip Header

                record = csvReader.readNext()
                while (record != null) {
                    println(record[0] + " | " + record[1])
                    record = csvReader.readNext()
                }
                csvReader.close()
                return listOf(arrayOf("error","error"))

            } else {
                csvReader = CSVReaderBuilder(fileReader).withSkipLines(1).build()

                val records = csvReader.readAll()
                for (_record in records) {
//                    println(_record[0] + " | " + _record[1])
                    this.expListText += _record[0] + " \t | \t " + _record[1] + "\n"
                }
                return records
            }
        } catch (e: Exception) {
            println("Reading CSV Error!")
            e.printStackTrace()
        } finally {
            try {
                fileReader!!.close()
                csvReader!!.close()
            } catch (e: IOException) {
                println("Closing fileReader/csvParser Error!")
                e.printStackTrace()
            }
//            return records
//            return listOf(arrayOf("error","error"))
        }
        return listOf(arrayOf("error","error"))
    }

    fun experimentRoot(view: View) {
        println("Button Pressed!")

        experimentStartTime = (System.currentTimeMillis()/1000).toLong()

        val expButton = findViewById<Button>(R.id.mainButton)
        val pclLogo = findViewById<ImageView>(R.id.pclLogo)

        expList.setVisibility(View.GONE)
        expButton.setVisibility(View.INVISIBLE)
        pclLogo.setVisibility(View.INVISIBLE)

        this.logCounter.start()
        this.experimentCounter.start()
    }


    fun wtiteLogEntriesToFile(append:Boolean) {
        var csvWriter: CSVWriter? = null
        val fileName = "logFile_" + experimentStartTime.toString() + ".csv"

        var logFile:File = File(getExternalFilesDir("measurementLogs"),fileName)
        println(logFile.toString())
        try {

            val logFileOutPutStream = FileOutputStream(logFile, append)
            val logFileWriter = OutputStreamWriter(logFileOutPutStream, StandardCharsets.UTF_8)
            csvWriter = CSVWriter(logFileWriter,
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)

            if (append == false) {
                csvWriter.writeNext(CSV_HEADER)
                }

            for (measurement in measurementList) {
                val data = arrayOf<String>(
                        measurement.timestamp.toString(),
                        measurement.battery_status.toString(),
                        measurement.battery_charging.toString(),
                        measurement.battery_capacity.toString(),
                        measurement.battery_cc.toString(),
                        measurement.battery_currentAvg.toString(),
                        measurement.battery_currentNow.toString(),
                        measurement.battery_energyCount.toString(),
                        measurement.battery_technology.toString(),
                        measurement.battery_temp.toString(),
                        measurement.battery_voltage.toString(),
                        measurement.battery_health.toString(),
                        measurement.displayState.toString()
                    )

                csvWriter.writeNext(data)
            }

            println("Write CSV using CSVWriter successfully!")

        } catch (e: Exception) {
            println("Writing CSV error!")
            e.printStackTrace()
        } finally {
            try {
                csvWriter!!.close()
            } catch (e: IOException) {
            println("Flushing/closing error!")
            e.printStackTrace()
            }
        }
    }

    fun getPowerMeasurement(displayState:String?): powerMeasurement{

        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }

        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        val bm_status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val bm_health: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        // val bm_level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        // val bm_scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val bm_technology: String = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Null"
        val bm_temp: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val bm_voltage: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1) ?: -1
        val bm_capacity: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val bm_cc: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        val bm_currentAvg: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)
        val bm_currentNow: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        val bm_energyCount: Long = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        val bm_charging: Int = batteryManager.getIntProperty(BatteryManager.BATTERY_STATUS_CHARGING)
        // val bm_fullStatus: Int = BatteryManager.BATTERY_PROPERTY_STATUS

        return powerMeasurement(
            battery_status=bm_status,
            battery_charging = bm_charging,
            battery_capacity = bm_capacity,
            battery_cc = bm_cc,
            battery_currentAvg = bm_currentAvg,
            battery_currentNow = bm_currentNow,
            battery_energyCount = bm_energyCount,
            battery_technology = bm_technology,
            battery_temp = bm_temp,
            battery_voltage = bm_voltage,
            battery_health = bm_health,
            displayState = displayState
        )
    }
}