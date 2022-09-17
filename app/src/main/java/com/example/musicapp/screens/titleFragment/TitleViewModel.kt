package com.example.musicapp.screens.titleFragment

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.example.musicapp.foldersPaths
import com.example.musicapp.network.MusicApi
import com.example.musicapp.network.musicProfile.MusicProfile
import com.example.musicapp.network.musicProfile.Playlist
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.*
import java.lang.Exception
import java.net.URL
import java.security.MessageDigest


class TitleViewModel(private val applicationMy: Application) : AndroidViewModel(applicationMy) {


    private var _profile = MutableLiveData<MusicProfile>()
    val profile: LiveData<MusicProfile>
        get() = _profile


    private var _playlists = MutableLiveData<List<Playlist>>()
    val playlists: LiveData<List<Playlist>>
        get() = _playlists

    private val _showError = MutableLiveData<String>()
    val showError: LiveData<String>
        get() = _showError

    private val _renderUI = MutableLiveData<Boolean>(false)
    val renderUI: LiveData<Boolean>
        get() = _renderUI


    init {
        getResponseFromServer()
    }

    private fun getResponseFromServer() {
        Timber.i("my log we in the getResponseServer")
        viewModelScope.launch {
            try {
                _profile.value = MusicApi.retrofitService.getJSON()

                _playlists.value = _profile.value!!.schedule.playlists

                downloadMusicFiles()

                _renderUI.value = true
                Timber.i("my log folders : ${foldersPaths.toString()}")

            } catch (e: Exception) {
                _showError.value = e.message
            }
        }
    }


    private suspend fun downloadMusicFiles() {
        _playlists.value!!.forEach { playlist ->
            playlist.songs.forEach { song ->
                downloadMusicFileFromUrl(
                    song.url,
                    song.name,
                    getApplication<Application>().applicationContext,
                    playlist.name
                ).join()

            }
        }

    }

    private fun downloadMusicFileFromUrl(
        urlString: String, fileName: String, context: Context, playlist: String
    ): Job {
        Timber.i("my log in download $fileName")
        return viewModelScope.launch(Dispatchers.IO) {
            Timber.i("my log in coroutine")
            val url = URL(urlString)
            url.openConnection().connect()

            val inputStream = BufferedInputStream(url.openStream())

            val directoryString = context.filesDir.absolutePath + "/$playlist/"
            val directory = File(directoryString)

            if (!foldersPaths.containsKey(playlist)) {
                foldersPaths[playlist] = directoryString
            }

            if (!directory.exists()) {
                Timber.i("my log : make dir ${directory.toString()}")
                directory.mkdir();
            }

            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)

            val data = ByteArray(1024)
            var count = inputStream.read(data)

            try {
                while (count != -1) {
                    outputStream.write(data, 0, count)
                    count = inputStream.read(data)
                }
            } catch (e: Exception) {
                Timber.i("my log again error, lovely emulator >")
            }

            Timber.i("my log save complete $fileName")

            outputStream.flush()
            outputStream.close()
            inputStream.close()
        }
    }


}