package com.oo.resume.service

import androidx.lifecycle.LiveData
import com.oo.resume.data.path.AccountUrl
import com.oo.resume.data.request.LoginRequest
import com.oo.resume.data.request.RegistRequest
import com.oo.resume.data.request.ResetPasswordRequest
import com.oo.resume.data.response.AccountDTO
import com.oo.resume.data.response.ErrorBody
import com.oo.resume.net.ResposeResultWithErrorType
import com.oo.resume.net.ResposeResult
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface AccountService {

    @POST(AccountUrl.PREFIX + AccountUrl.PATH_REGIST)
    fun regist(@Body params: RegistRequest): LiveData<ResposeResult<AccountDTO>>

    @POST(AccountUrl.PREFIX + AccountUrl.PATH_LOGIN)
    fun login(@Body params: LoginRequest): LiveData<ResposeResult<AccountDTO>>

    @PUT(AccountUrl.PREFIX + AccountUrl.PATH_UPDATE)
    fun update(@Body params: AccountDTO): LiveData<ResposeResult<AccountDTO>>

    @PUT(AccountUrl.PREFIX + AccountUrl.PATH_RESET_PASSWORD)
    fun resetPassword(@Body params: ResetPasswordRequest): LiveData<ResposeResult<Boolean>>

}
