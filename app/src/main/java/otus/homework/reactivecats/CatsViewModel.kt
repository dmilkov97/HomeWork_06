package otus.homework.reactivecats

import android.content.Context
import android.net.http.HttpException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import okio.IOException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CatsViewModel(
    catsService: CatsService,
    localCatFactsGenerator: LocalCatFactsGenerator,
    context: Context
) : ViewModel() {

    private val _catsLiveData = MutableLiveData<Result>()
    val catsLiveData: LiveData<Result> = _catsLiveData
    private val сompositeDisposable = CompositeDisposable()
    private var _catsService: CatsService = catsService
    private val _localCatFactsGenerator = localCatFactsGenerator

    init {
        getFacts()

    }

    fun getFacts() {
        val disposable = _catsService.getCatFact()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .onErrorResumeNext { _localCatFactsGenerator.generateCatFact() }
            .subscribe({
                _catsLiveData.value = Success(it)
            }, {
                when (it) {
                    is IOException -> {
                        _catsLiveData.value = ServerError
                    }
                    else -> {
                        _catsLiveData.value = Error(it.message.toString())
                    }
                }
            })

        сompositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        сompositeDisposable.dispose()
    }
}

class CatsViewModelFactory(
    private val catsRepository: CatsService,
    private val localCatFactsGenerator: LocalCatFactsGenerator,
    private val context: Context
) :
    ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CatsViewModel(catsRepository, localCatFactsGenerator, context) as T
}

sealed class Result
data class Success(val fact: Fact) : Result()
data class Error(val message: String) : Result()
object ServerError : Result()