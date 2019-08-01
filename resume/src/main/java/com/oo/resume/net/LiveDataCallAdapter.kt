package com.oo.resume.net

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.oo.resume.data.const.ApiErrorCode
import com.oo.resume.data.response.ErrorBody
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.*
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.io.IOException
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.ConnectException
import java.net.SocketTimeoutException


class LiveDataCallAdapter private constructor() : CallAdapter.Factory() {

    private var mFactory: RxJava2CallAdapterFactory = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io())

    companion object {
        fun create(): LiveDataCallAdapter {
            return LiveDataCallAdapter()
        }
    }

    public override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {
        if (CallAdapter.Factory.getRawType(returnType) != LiveData::class.java) {
            return null
        }
        if (returnType !is ParameterizedType) {
            throw IllegalStateException("Response must be parametrized as " + "LiveData<ResposeResult> or LiveData<? extends ResposeResult>")
        }

        val responseType = CallAdapter.Factory.getParameterUpperBound(0, returnType)
        when (CallAdapter.Factory.getRawType(responseType)) {
            ResposeResult::class.java -> {
                if (responseType !is ParameterizedType) {
                    throw IllegalStateException("Response must be parametrized as " + "LiveData<Response<ResposeResult>> or LiveData<Response<? extends ResposeResult>>")
                }

                return LiveDataResponseCallAdapter<Any>(responseType)
            }
            else -> return null
        }
    }

    inner class LiveDataResponseCallAdapter<R> internal constructor(private val responseType: Type) :
        CallAdapter<R, LiveData<ResposeResult<R?>>> {

        override fun responseType(): Type {
            return responseType
        }

        override fun adapt(call: Call<R>): LiveData<ResposeResult<R?>> {
            val liveDataResponse = MutableLiveData<ResposeResult<R?>>()
            liveDataResponse.postValue(ResposeResult.loading())
            Schedulers.io().scheduleDirect { call.enqueue(LiveDataResponseCallCallback(liveDataResponse)) }
            return liveDataResponse
        }

        private inner class LiveDataResponseCallCallback<T> internal constructor(private val liveData: MutableLiveData<ResposeResult<T?>>) :
            Callback<T> {

            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (call.isCanceled) return
                AndroidSchedulers.mainThread().scheduleDirect {
                    if (response.isSuccessful) {
                        liveData.postValue(ResposeResult.success(response.body()))
                    } else {
                        onFailure(call, HttpException(response))
                    }
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                if (call.isCanceled) return
                AndroidSchedulers.mainThread().scheduleDirect {
                    liveData.postValue(ResposeResult.failure(asApiErrors(t)))
                }
            }
        }

        private fun asApiErrors(throwable: Throwable): ErrorBody? {
            // We had non-200 http error
            when (throwable) {
                is HttpException -> return handApiError(throwable)
                is SocketTimeoutException -> return ErrorBody(ApiErrorCode.SOCKET_TIMEOUT, throwable.message)
                is ConnectException -> return ErrorBody(ApiErrorCode.NETWORK_UNREACHABLE, throwable.message)
                else -> return ErrorBody(ApiErrorCode.UNKNOW, throwable.message)
            }
        }

        private fun handApiError(throwable: HttpException): ErrorBody? {
            val response = throwable.response()
            val body = response?.errorBody()
            if (body == null) return ErrorBody(ApiErrorCode.UNKNOW, throwable.message)
            try {
                return RetrofitClient.get().responseBodyConverter<ErrorBody>(
                    ErrorBody::class.java, arrayOfNulls(0)
                ).convert(body)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            return ErrorBody(ApiErrorCode.UNKNOW, throwable.message)
        }
    }


}