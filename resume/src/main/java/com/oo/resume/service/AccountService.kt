package com.oo.resume.service

import com.oo.resume.data.path.UrlConst
import com.oo.resume.data.request.LoginRequest
import com.oo.resume.data.request.RegistRequest
import com.oo.resume.data.response.AccountDTO
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface AccountService {

    @POST(UrlConst.ACCOUNT_PREFIX + UrlConst.ACCOUNT_REGIST)
    fun regist(@Body params: RegistRequest): Observable<AccountDTO>

    @POST(UrlConst.ACCOUNT_PREFIX + UrlConst.ACCOUNT_LOGIN)
    fun login(@Body params: LoginRequest): Observable<AccountDTO>

    @PUT(UrlConst.ACCOUNT_PREFIX + UrlConst.ACCOUNT_UPDATE)
    fun update(@Body params: AccountDTO): Observable<AccountDTO>

}