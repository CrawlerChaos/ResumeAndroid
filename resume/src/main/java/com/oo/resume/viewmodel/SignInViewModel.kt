package com.oo.resume.viewmodel

import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.oo.platform.repo.AbsentLiveData
import com.oo.platform.repo.RepositoryFactory
import com.oo.platform.viewmodel.BaseViewModel
import com.oo.resume.data.request.LoginRequest
import com.oo.resume.data.request.RegistRequest
import com.oo.resume.data.response.AccountDTO
import com.oo.resume.data.response.ErrorBody
import com.oo.resume.net.ResposeResultWithErrorType
import com.oo.resume.net.ResposeResult
import com.oo.resume.repository.AccountRepo

/**
 *  $author yangchao
 *  $email  cd.uestc.superyoung@gmail.com
 *  $date   2019-06-12 13:23
 *  $describe
 */
class SignInViewModel : BaseViewModel() {

    private val accountRepo = RepositoryFactory.getRepository(AccountRepo::class.java)

    private val loginRequest = MutableLiveData<LoginRequest>()
    private val registRequest = MutableLiveData<RegistRequest>()

    private val viewType = MutableLiveData<SignType>()
    private val registResult: LiveData<ResposeResult<AccountDTO>>
    private val loginResult: LiveData<ResposeResult<AccountDTO>>


    init {
        viewType.value = SignType.Login

        loginResult = Transformations.switchMap(
            loginRequest,
            Function<LoginRequest, LiveData<ResposeResult<AccountDTO>>> { request ->
                if (request == null) return@Function AbsentLiveData.create()
                accountRepo.login(request)
            })

        registResult = Transformations.switchMap(
            registRequest,
            Function<RegistRequest, LiveData<ResposeResult<AccountDTO>>> { request ->
                if (request == null) return@Function AbsentLiveData.create()
                accountRepo.regist(request)
            })

    }

    enum class SignType {
        Regist,
        Login
    }

    fun getViewType(): LiveData<SignType> {
        return viewType
    }

    fun getLoginResult(): LiveData<ResposeResult<AccountDTO>> {
        return loginResult
    }

    fun getRegistResult(): LiveData<ResposeResult<AccountDTO>> {
        return registResult
    }

    fun changeViewType() {
        when (viewType.value) {
            SignType.Regist -> viewType.value = SignType.Login
            else -> viewType.value = SignType.Regist
        }
    }

    fun login(request: LoginRequest?) {
        if (request == null) return
        loginRequest.value = request
    }

    fun regist(request: RegistRequest?) {
        if (request == null) return
        registRequest.value = request
    }
}