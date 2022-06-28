package com.medgaus.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medgaus.network.YouTubeWebApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory


@OptIn(FlowPreview::class)
class SearchViewModel : ViewModel() {

    private companion object {
        const val SEARCH_DEBOUNCE = 1_000L
    }

    private val _uiState = MutableStateFlow(UiState.Init)
    val uiState = _uiState.asStateFlow()

    private val queryState = MutableStateFlow("")

    init {
        // TODO: move creation of services into DI
        val okHttpClient = OkHttpClient.Builder().build()

        val retrofit = Retrofit.Builder()
            .addConverterFactory(ScalarsConverterFactory.create())
            .baseUrl(YouTubeWebApi.SERVER_BASE_URL)
            .client(okHttpClient).build()

        val webApi = retrofit.create(YouTubeWebApi::class.java)

        viewModelScope.launch {
            queryState
                .debounce(SEARCH_DEBOUNCE)
                .collect { query ->
                    val searchResult = webApi.search(query)
                    searchResult.body()?.string()?.let { pageBody ->
                        VideoUrlParser().parseVideoUrl(pageBody)
                    }
                }
        }
    }

    fun onSearchFieldTextChanged(text: String) {
        queryState.tryEmit(text)
    }

    sealed class UiState {
        object Init : UiState()
        object Loading : UiState()
        data class Error(val message: String) : UiState()
        data class ScreenData(val data: List<String>) : UiState()
    }

}

// TODO: should be moved out of here 
class VideoUrlParser {

    // Example of video reference
    //    <a id="thumbnail" class="yt-simple-endpoint inline-block style-scope ytd-thumbnail" aria-hidden="true" tabindex="-1" rel="null" href="/watch?v=rsBNRl34UhQ">

    companion object {
        const val VIDEO_URL_REGEX =
            "<a.*class=\"yt-simple-endpoint inline-block style-scope ytd-thumbnail\".*>"
    }

    private val regex = Regex(VIDEO_URL_REGEX)

    fun parseVideoUrl(pageHtml: String): List<String> {
        return regex
            .findAll(pageHtml)
            .toList()
            .map {
                it.value.substringAfterLast("?v=").dropLast(2)
            }
    }

}