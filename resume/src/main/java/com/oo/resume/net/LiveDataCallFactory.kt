package com.oo.resume.net

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.JsonIOException
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import com.oo.resume.data.response.ErrorBody
import io.reactivex.Scheduler
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


class LiveDataCallFactory private constructor(
    val subscribWorker: Scheduler.Worker,
    val observeWorker: Scheduler.Worker
) : CallAdapter.Factory() {

    companion object {

        private val gson = Gson()

        fun create(subscribScheduler: Scheduler, observeScheduler: Scheduler): LiveDataCallFactory {
            return LiveDataCallFactory(
                subscribScheduler.createWorker(),
                observeScheduler.createWorker()
            )
        }

        @Suppress("UNCHECKED_CAST")
        private fun <T> convertError(throwable: Throwable, type: Type): T? {
            return when (throwable) {
                is HttpException -> {
                    val response = throwable.response()
                    val body = response?.errorBody()
                        ?: return null
                    val adapter = gson.getAdapter(TypeToken.get(type))
                    val jsonReader = gson.newJsonReader(body.charStream())
                    try {
                        val result = adapter.read(jsonReader)
                        if (jsonReader.peek() != JsonToken.END_DOCUMENT) {
                            throw JsonIOException("JSON document was not fully consumed.")
                        }
                        return result as T
                    } catch (e: Throwable) {
                        return null
                    } finally {
                        body.close()
                    }
                }
                else -> null
            }
        }
    }

    public override fun get(
        returnType: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != LiveData::class.java) {
            return null
        }

        isTypeIllige(returnType)
        returnType as ParameterizedType
        val responseResultType = getParameterUpperBound(0, returnType)

        isTypeIllige(responseResultType)
        responseResultType as ParameterizedType
        var responseType: Type = getParameterUpperBound(0, responseResultType)

        return when (getRawType(responseResultType)) {
            ResposeResult::class.java -> LiveDataCallAdapter<Nothing>(responseType)

            ResposeResultWithErrorType::class.java -> LiveDataWithErrorTypeCallAdapter<Nothing, Nothing>(
                responseType,
                getParameterUpperBound(1, responseResultType)
            )

            else -> LiveDataCallAdapter<Nothing>(responseType)
        }
    }

    private fun isTypeIllige(type: Type) {
        if (type !is ParameterizedType) {
            throwError()
        }
    }

    private fun throwError() {
        throw IllegalStateException("Response must be parametrized as " + "LiveData<ResposeResultWithErrorType<DataT,ErrorT>> or LiveData<ResposeResult<DataT>> or LiveData<DataT>")
    }

    inner class LiveDataCallAdapter<ResponseBodyT> internal constructor(private val responseType: Type) :
        BaseLiveDataResponseCallAdapter<ResposeResult<ResponseBodyT>, ResponseBodyT, ErrorBody>(
            responseType,
            ErrorBody::class.java
        ) {
        override fun loading(): ResposeResult<ResponseBodyT> {
            return ResposeResult.loading()
        }

        override fun success(response: ResponseBodyT?): ResposeResult<ResponseBodyT> {
            return ResposeResult.success(response)
        }

        override fun failure(error: ErrorBody?): ResposeResult<ResponseBodyT> {
            return ResposeResult.failure(error)
        }
    }

    inner class LiveDataWithErrorTypeCallAdapter<ResponseBodyT, ErrorBodyT> internal constructor(
        private val responseType: Type,
        private val errorType: Type
    ) :
        BaseLiveDataResponseCallAdapter<ResposeResultWithErrorType<ResponseBodyT, ErrorBodyT>, ResponseBodyT, ErrorBodyT>(
            responseType,
            errorType
        ) {
        override fun loading(): ResposeResultWithErrorType<ResponseBodyT, ErrorBodyT> {
            return ResposeResultWithErrorType.loading()
        }

        override fun success(response: ResponseBodyT?): ResposeResultWithErrorType<ResponseBodyT, ErrorBodyT> {
            return ResposeResultWithErrorType.success(response)
        }

        override fun failure(error: ErrorBodyT?): ResposeResultWithErrorType<ResponseBodyT, ErrorBodyT> {
            return ResposeResultWithErrorType.failure(error)
        }

    }

    abstract inner class BaseLiveDataResponseCallAdapter<ResponseResultT : ResposeResultWithErrorType<ResponseBodyT, ErrorBodyT>, ResponseBodyT, ErrorBodyT> internal constructor(
        private val responseType: Type,
        private val errorType: Type
    ) :
        CallAdapter<ResponseBodyT, LiveData<ResponseResultT>> {

        override fun responseType(): Type {
            return responseType
        }

        override fun adapt(call: Call<ResponseBodyT>): LiveData<ResponseResultT> {
            val liveDataResponse = MutableLiveData<ResponseResultT>()
            liveDataResponse.postValue(loading())
            subscribWorker.schedule { call.enqueue(LiveDataResponseCallCallback(liveDataResponse)) }
            return liveDataResponse
        }

        abstract fun loading(): ResponseResultT

        abstract fun success(response: ResponseBodyT?): ResponseResultT

        abstract fun failure(error: ErrorBodyT?): ResponseResultT

        private inner class LiveDataResponseCallCallback internal constructor(private val liveData: MutableLiveData<ResponseResultT>) :
            Callback<ResponseBodyT> {

            override fun onResponse(call: Call<ResponseBodyT>, response: Response<ResponseBodyT>) {
                if (call.isCanceled) return
                observeWorker.schedule {
                    if (response.isSuccessful) {
                        liveData.postValue(success(response.body()))
                    } else {
                        onFailure(call, HttpException(response))
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBodyT>, t: Throwable) {
                if (call.isCanceled) return
                observeWorker.schedule {
                    liveData.postValue(failure(convertError(t, errorType)))
                }
            }
        }

    }


}
