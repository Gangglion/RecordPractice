package com.glion.recordpractice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.glion.recordpractice.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException


/*
MEMO : 녹음 중 exoPlayer 의 onProgress 방식으로, 매번 인식해서 무언가 작업을 할 수 있어야함.
재생 중 다른거 재생하면 기존에 재생하던건 정지해야함.
녹음같은 경우 시간제한을 둬서(타이머 실행?) 특정 시간 지나면 자동으로 종료되게 해야함(지속적으로 녹음되는 것을 방지)
캐시파일에 저장 후 MultiPart로 mp3 파일 서버 전송, API response 받아서 활용이 가능해야함.
url 로 MediaPlayer 가능한지 확인해야함.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var mContext: Context
    private lateinit var mBinding: ActivityMainBinding

    // 오디오 녹음을 위해 MediaRecorder Class 사용
    private var mediaRecorder: MediaRecorder? = null
    // 녹음한 파일을 저장할 경로 - 캐시파일에 저장 -> multiPart를 통해 전송 -> 저장한 캐시파일 삭제의 순서로 진행 예정
    private var outputFilePath: String? = null

    companion object{
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        const val CACHE_PATH = "data/data/com.glion.recordpractice/custom"
    }
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    // MediaPlayer - 재생을 위함
    private var mPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mContext = this
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        getMp3File()


        mBinding.btnStartRecord.setOnClickListener{
            if(permissionToRecordAccepted) {
                startRecording()
            } else{
                Toast.makeText(mContext, "권한 미허용 상태", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.btnStopRecord.setOnClickListener{
            stopRecording()
        }

        mBinding.btnPlayRecorded.setOnClickListener{
            playMusic()
        }
    }

    // 녹음 시작
    private fun startRecording() {
        val directory = File(CACHE_PATH)
        directory.mkdir()
        outputFilePath = "$CACHE_PATH/recordTest.mp3"
        mediaRecorder =  if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            MediaRecorder(mContext)
        } else{
            MediaRecorder()
        }
        mediaRecorder?.apply{
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFilePath)
        }

        try{
            mediaRecorder?.prepare()
            mediaRecorder?.start()
            mBinding.btnStartRecord.isEnabled = false
            mBinding.btnStopRecord.isEnabled = true
            Toast.makeText(mContext, "녹음을 시작합니다", Toast.LENGTH_SHORT).show()
        } catch(e: IOException){
            Log.e("shhan", "Error : ${e.printStackTrace()}")
        }
    }

    // 녹음 중지
    private fun stopRecording(){
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        mBinding.btnStartRecord.isEnabled = true
        mBinding.btnStopRecord.isEnabled = false
        Toast.makeText(mContext, "녹음을 중지합니다", Toast.LENGTH_SHORT).show()
    }

    // 권한 관련
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    // 특정 경로의 파일 중에서 원하는 파일을 찾아서 리턴
    private fun getMp3File(): String{
        val fileName = "recordTest.mp3"
        val file2 = "record.mp3"
        val listFile = File(CACHE_PATH).listFiles()
        for(file in listFile!!){
            Log.d("shhan", "File Name : ${file.name}")
            if(file2 == file.name)
                return file.name
            break
        }
        return ""
    }

    /**
     * 특정 경로/파일명으로 MediaPlayer 에 세팅하여 재생
     */
    private fun playMusic(){
        mPlayer = MediaPlayer()
        mPlayer?.setDataSource("$CACHE_PATH/${getMp3File()}")
        mPlayer?.prepare()
        mPlayer?.start()
        Log.d("shhan", "${mPlayer?.duration}")
    }
}