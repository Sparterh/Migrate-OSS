package balti.migrate.utilities

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.content.FileProvider
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import balti.migrate.R
import kotlinx.android.synthetic.main.error_report_layout.view.*
import java.io.*

class CommonToolKotlin(val context: Context) {

    companion object {
        val DEBUG_TAG = "migrate_tag"
        val MAIN_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/6749792408"
        val BACKUP_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/5791933954"
        val EXTRA_BACKUPS_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/5217218882"
        val BACKUP_PROGRESS_ACTIVITY_AD_ID = "ca-app-pub-6582325651261661/2755664936"
        val UNIVERSAL_TEST_ID = "ca-app-pub-3940256099942544/6300978111"

        val MAX_TWRP_ZIP_SIZE = 4194300L

        val DEFAULT_INTERNAL_STORAGE_DIR = "/sdcard/Migrate"
        val TEMP_DIR_NAME = "/data/local/tmp/migrate_cache"

        val ACTION_BACKUP_PROGRESS = "Migrate progress broadcast"
        val ACTION_REQUEST_BACKUP_DATA = "get data"
        val ACTION_EXTRA_BACKUP_ACTIVITY_STARTED = "extraBackupsStarted"

        val PACKAGE_NAME_PLAY_STORE = "com.android.vending"
        val PACKAGE_NAME_FDROID = "org.fdroid.fdroid.privileged"
        val PACKAGE_NAMES_PACKAGE_INSTALLER = arrayOf("com.google.android.packageinstaller")

        val PREF_FILE_APPS = "apps"
        val PREF_FILE_MAIN = "main"
        val PREF_FIRST_RUN = "firstRun"
        val PREF_VERSION_CURRENT = "version"
        val PREF_ANDROID_VERSION_WARNING = "android_version_warning"
        val PREF_DEFAULT_BACKUP_PATH = "defaultBackupPath"
        val PREF_ASK_FOR_RATING = "askForRating"
        val PREF_SYSTEM_APPS_WARNING = "system_apps_warning"
        val PREF_ALTERNATE_ACCESS_ASKED = "alternate_access_asked"
        val PREF_CALCULATING_SIZE_METHOD = "calculating_size_method"
        val PREF_TERMINAL_METHOD = 1
        val PREF_ALTERNATE_METHOD = 2
        val PREF_MAX_BACKUP_SIZE = "max_backup_size"
        val PREF_AUTOSELECT_EXTRAS = "autoSelectExtraBackups"
        val PREF_SHOW_STOCK_WARNING = "showStockWarning"


        val PROPERTY_APP_SELECTION = "app"        // used to set property in AppListAdapter
        val PROPERTY_DATA_SELECTION = "data"        // used to set property in AppListAdapter
        val PROPERTY_PERMISSION_SELECTION = "permission"        // used to set property in AppListAdapter

        //extra backups

        val JOBCODE_READ_CONTACTS = 3443
        val JOBCODE_READ_SMS = 2398
        val JOBCODE_READ_SMS_THEN_CALLS = 2399
        val JOBCODE_READ_CALLS = 1109
        val JOBCODE_READ_DPI = 3570
        val JOBCODE_READ_ADB = 2339
        val JOBCODE_READ_WIFI = 1264

        val JOBCODE_LOAD_CONTACTS = 7570
        val JOBCODE_LOAD_SMS = 1944
        val JOBCODE_LOAD_CALLS = 2242
        val JOBCODE_LOAD_KEYBOARDS = 6765
        val JOBCODE_LOAD_INSTALLERS = 8709

        val JOBCODE_MAKE_APP_PACKETS = 65364

        val CONTACT_PERMISSION = 933
        val SMS_PERMISSION = 944
        val CALLS_PERMISSION = 676
        val SMS_AND_CALLS_PERMISSION = 567

        val WIFI_FILE_NAME = "WifiConfigStore.xml"
        val WIFI_FILE_PATH = "/data/misc/wifi/$WIFI_FILE_NAME"
        val WIFI_FILE_NOT_FOUND = "***not_found***"

        // installer list adapter

        val NOT_SET_POSITION = 0
        val PLAY_STORE_POSITION = 1
        val FDROID_POSITION = 2
    }

    fun unpackAssetToInternal(assetFileName: String, targetFileName: String, toInternal: Boolean): String {

        val assetManager = context.assets
        val unpackFile = if (toInternal) File(context.filesDir, targetFileName)
        else File(context.externalCacheDir, targetFileName)

        var read: Int
        val buffer = ByteArray(4096)

        return try {
            val inputStream = assetManager.open(assetFileName)
            val writer = FileOutputStream(unpackFile)
            while (true) {
                read = inputStream.read(buffer)
                if (read > 0) writer.write(buffer, 0, read)
                else break
            }
            writer.close()
            unpackFile.setExecutable(true)
            return unpackFile.absolutePath
        } catch (e: IOException){
            e.printStackTrace()
            ""
        }
    }

    fun reportLogs(isErrorLogMandatory: Boolean) {

        val progressLog = File(context.externalCacheDir, "progressLog.txt")
        val errorLog = File(context.externalCacheDir, "errorLog.txt")

        val backupScripts = context.externalCacheDir.listFiles { f: File ->
            (f.name.startsWith("the_backup_script_") || f.name.startsWith("retry_script_"))
                    && f.name.endsWith(".sh")

        }

        if (isErrorLogMandatory && !errorLog.exists()) {
            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(context.getString(R.string.error_log_does_not_exist))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        else if (errorLog.exists() || progressLog.exists() || backupScripts.isNotEmpty()){

            val eView = View.inflate(context, R.layout.error_report_layout, null)

            eView.share_progress_checkbox.isChecked = progressLog.exists()
            eView.share_progress_checkbox.isEnabled = progressLog.exists()

            eView.share_script_checkbox.isChecked = backupScripts.isNotEmpty()
            eView.share_script_checkbox.isEnabled = backupScripts.isNotEmpty()

            eView.share_errors_checkbox.isChecked = errorLog.exists()
            eView.share_errors_checkbox.isEnabled = errorLog.exists() && !isErrorLogMandatory

            AlertDialog.Builder(context)
                    .setView(eView)
                    .setPositiveButton(R.string.agree_and_send) {_, _ ->

                        val body = deviceSpecifications

                        val emailIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            type = "text/plain"
                            putExtra(Intent.EXTRA_EMAIL, arrayOf("help.baltiapps@gmail.com"))
                            putExtra(Intent.EXTRA_SUBJECT, "Log report for Migrate")
                            putExtra(Intent.EXTRA_TEXT, body)
                        }

                        val uris = ArrayList<Uri>(0)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            if (eView.share_errors_checkbox.isChecked) uris.add(FileProvider.getUriForFile(context, "migrate.provider", errorLog))
                            if (eView.share_progress_checkbox.isChecked) uris.add(FileProvider.getUriForFile(context, "migrate.provider", progressLog))
                            if (eView.share_script_checkbox.isChecked)
                                for (f in backupScripts)
                                    uris.add(FileProvider.getUriForFile(context, "migrate.provider", f))
                        }
                        else {
                            if (eView.share_errors_checkbox.isChecked) uris.add(Uri.fromFile(errorLog))
                            if (eView.share_progress_checkbox.isChecked) uris.add(Uri.fromFile(progressLog))
                            if (eView.share_script_checkbox.isChecked) for (f in backupScripts) uris.add(Uri.fromFile(f))
                        }

                        emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)

                        try {
                            context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.select_mail)))
                            Toast.makeText(context, context.getString(R.string.select_mail), Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                        }

                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()

        }
        else {

            val msg = context.getString(R.string.progress_log_does_not_exist) + "\n" +
                    context.getString(R.string.error_log_does_not_exist) + "\n" +
                    context.getString(R.string.backup_script_does_not_exist) + "\n"

            AlertDialog.Builder(context)
                    .setTitle(R.string.log_files_do_not_exist)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }

    }

    var deviceSpecifications: String =
            "CPU_ABI: " + Build.SUPPORTED_ABIS[0] + "\n" +
                    "Brand: " + Build.BRAND + "\n" +
                    "Manufacturer: " + Build.MANUFACTURER + "\n" +
                    "Model: " + Build.MODEL + "\n" +
                    "Device: " + Build.DEVICE + "\n" +
                    "SDK: " + Build.VERSION.SDK_INT + "\n" +
                    "Board: " + Build.BOARD + "\n" +
                    "Hardware: " + Build.HARDWARE
        private set


    fun isServiceRunning(name: String): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (s in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (name == s.service.className)
                return true
        }
        return false
    }


    fun suEcho(): Array<Any> {
        val suRequest = Runtime.getRuntime().exec("su")

        val writer = BufferedWriter(OutputStreamWriter(suRequest.outputStream))

        //writer.write("pm grant " + context.getPackageName() + " android.permission.DUMP\n" );
        //writer.write("pm grant " + context.getPackageName() + " android.permission.PACKAGE_USAGE_STATS\n" );
        writer.write("exit\n")
        writer.flush()

        val errorReader = BufferedReader(InputStreamReader(suRequest.errorStream))
        val outputReader = BufferedReader(InputStreamReader(suRequest.inputStream))

        var line: String?
        var errorMessage = ""

        while (true) {
            line = outputReader.readLine()
            if (line != null) errorMessage = errorMessage + line + "\n"
            else break
        }
        errorMessage += "Error:\n\n"
        while (true) {
            line = errorReader.readLine()
            if (line != null) errorMessage = errorMessage + line + "\n"
            else break
        }

        suRequest.waitFor()
        return arrayOf(suRequest.exitValue() == 0, errorMessage)
    }

    fun getDirLength(directoryPath: String): Long {
        val file = File(directoryPath)
        return if (file.exists()) {
            if (!file.isDirectory) file.length()
            else {
                val files = file.listFiles()
                var sum = 0L
                for (f in files) sum += getDirLength(f.absolutePath)
                sum
            }
        } else 0
    }

    fun dirDelete(path: String) {
        val file = File(path)
        if (file.exists()) {
            if (!file.isDirectory)
                file.delete()
            else {
                val files = file.listFiles()
                for (f in files)
                    dirDelete(f.absolutePath)
                file.delete()
            }
        }
    }

    fun getHumanReadableStorageSpace(space: Long): String {
        var res = "KB"

        var s = space.toDouble()

        if (s > 1024) {
            s /= 1024.0
            res = "MB"
        }
        if (s > 1024) {
            s /= 1024.0
            res = "GB"
        }

        return String.format("%.2f", s) + " " + res
    }

    fun getSdCardPaths(): Array<String> {
        val possibleSDCards = arrayListOf<String>()
        val storage = File("/storage/")
        if (storage.exists() && storage.canRead()) {
            val files = storage.listFiles { pathname ->
                (pathname.isDirectory && pathname.canRead()
                        && pathname.absolutePath != Environment.getExternalStorageDirectory().absolutePath)
            }
            for (f in files) {
                val sdDir = File("/mnt/media_rw/" + f.name)
                if (sdDir.exists() && sdDir.isDirectory && sdDir.canWrite())
                    possibleSDCards.add(sdDir.absolutePath)
            }
        }
        return possibleSDCards.toTypedArray()
    }

    fun showSdCardSupportDialog(): AlertDialog =
        AlertDialog.Builder(context)
                .setView(View.inflate(context, R.layout.learn_about_sd_card, null))
                .setPositiveButton(android.R.string.ok, null)
                .show()

    fun openWebLink(url: String) {
        if (url != "") {
            context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            })
        }
    }

    fun applyNamingCorrectionForShell(name: String) =
            name.replace("(", "\\(").replace(")", "\\)").replace(" ", "\\ ")



    fun tryIt(f: () -> Unit, showError: Boolean = false, isCancelable: Boolean = true, title: String = ""){
        try {
            f()
        }
        catch (e: Exception){
            if (showError)
            {
                showErrorDialog(e.message.toString(), title, isCancelable)
                e.printStackTrace()
            }
            else e.printStackTrace()
        }
    }

    fun tryIt(f: () -> Unit, title: String = ""){
        tryIt(f, false, true, title)
    }

    fun tryIt(f: () -> Unit){
        tryIt(f, "")
    }

    fun showErrorDialog(message: String, title: String = "", isCancelable: Boolean = true){
        try {
            Log.d(DEBUG_TAG, "should display")
            AlertDialog.Builder(context)
                    .setIcon(R.drawable.ic_error)
                    .setMessage(message).apply {

                        if (isCancelable)
                            setNegativeButton(R.string.close, null)
                        else {
                            setCancelable(false)
                            setNegativeButton(R.string.close) { _, _ ->
                                if (context is AppCompatActivity) {
                                    (context as AppCompatActivity).finish()
                                }
                            }
                        }

                        if (title == "")
                            setTitle(R.string.error_occurred)
                        else setTitle(title)

                    }
                    .show()
        } catch (e: Exception){
            e.printStackTrace()
            try {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            } catch (_: Exception){}
        }
    }

}