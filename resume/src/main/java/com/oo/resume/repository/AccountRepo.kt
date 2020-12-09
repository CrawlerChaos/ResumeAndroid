package com.oo.resume.repository

import android.content.Context
import androidx.arch.core.util.Function
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Transformations
import com.google.gson.Gson
import com.oo.platform.repo.IRepository
import com.oo.platform.repo.RepositoryFactory
import com.oo.resume.data.request.LoginRequest
import com.oo.resume.data.request.RegistRequest
import com.oo.resume.data.request.ResetPasswordRequest
import com.oo.resume.data.response.AccountDTO
import com.oo.resume.data.response.ErrorBody
import com.oo.resume.net.ResposeResultWithErrorType
import com.oo.resume.net.ResposeResult
import com.oo.resume.net.RetrofitClient
import com.oo.resume.service.AccountService
import com.oo.resume.util.SpUtil

/**
 *  $author yangchao
 *  $email  cd.uestc.superyoung@gmail.com
 *  $date   2019-06-12 11:29
 *  $describe
 */
class AccountRepo : IRepository {

    private var account: MediatorLiveData<AccountDTO> = MediatorLiveData()
    private var sessionRepo = RepositoryFactory.getRepository(SessionRepo::class.java)

    init {
        restoreAccount()
    }

    fun getAccountInfo(): LiveData<AccountDTO> {
        return account
    }

    private fun storeAccount(account: AccountDTO?) {
        val sp = SpUtil.get(ACCOUNT_SP_NAME, Context.MODE_PRIVATE)
        if (sp == null || account == null) return
        val editor = sp.edit()
        editor.putString(KEY_ACCOUNT_INFO, Gson().toJson(account))
        if(editor.commit()) this.account.postValue(account)
    }

    private fun restoreAccount() {
        val sp = SpUtil.get(ACCOUNT_SP_NAME, Context.MODE_PRIVATE) ?: return
        val accountJson = sp.getString(KEY_ACCOUNT_INFO, "")
        if (accountJson.isNullOrEmpty()) return
        this.account.postValue(Gson().fromJson(accountJson, AccountDTO::class.java))
    }

    fun login(request: LoginRequest): LiveData<ResposeResult<AccountDTO>> {
        return Transformations.map(RetrofitClient
                .getService(AccountService::class.java)
                .login(request), Function { result ->
            if (result.isSuccess) storeAccount(result.data)
            setSession(result)
            result
        })
    }

    private fun setSession(result: ResposeResult<AccountDTO>?) {
        if (result == null || !result.isSuccess || result.data == null
                || result.data.session_key.isNullOrEmpty()
                || result.data.session_user.isNullOrEmpty()
        ) return

        sessionRepo.setSession(result.data.session_user, result.data.session_key)
    }


    fun resetPassword(request: ResetPasswordRequest): LiveData<ResposeResult<Boolean>> {
        return RetrofitClient
                .getService(AccountService::class.java)
                .resetPassword(request)
    }

    fun update(request: AccountDTO): LiveData<ResposeResult<AccountDTO>> {
        return Transformations.map(RetrofitClient
                .getService(AccountService::class.java)
                .update(request)) { result ->
            if (result.isSuccess) storeAccount(result.data)
            result
        }
    }

    fun regist(request: RegistRequest): LiveData<ResposeResult<AccountDTO>> {
        return Transformations.map(RetrofitClient
                .getService(AccountService::class.java)
                .regist(request), Function { result ->
            if (result.isSuccess) storeAccount(result.data)
            setSession(result)
            result
        })
    }

    companion object {
        const val ACCOUNT_SP_NAME = "account"
        const val KEY_ACCOUNT_INFO = "account_info"
    }
}