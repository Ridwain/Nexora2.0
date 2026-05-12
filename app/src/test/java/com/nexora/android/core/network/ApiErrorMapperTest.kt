package com.nexora.android.core.network

import com.google.gson.Gson
import com.nexora.android.domain.session.NexoraError
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class ApiErrorMapperTest {
    private val mapper = ApiErrorMapper(Gson())

    @Test
    fun mapsUnauthorizedHttpErrors() {
        val errorBody = """{"message":"JWT expired","code":"PGRST301"}"""
            .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Unit>(401, errorBody))

        val error = mapper.map(exception)

        assertTrue(error is NexoraError.Unauthorized)
        assertEquals("JWT expired", error.message)
        assertEquals("PGRST301", error.code)
    }

    @Test
    fun mapsValidationHttpErrors() {
        val errorBody = """{"msg":"first_name_required"}"""
            .toResponseBody("application/json".toMediaType())
        val exception = HttpException(Response.error<Unit>(400, errorBody))

        val error = mapper.map(exception)

        assertTrue(error is NexoraError.Validation)
        assertEquals("first_name_required", error.message)
    }

    @Test
    fun mapsIoErrorsAsNetworkErrors() {
        val error = mapper.map(IOException("offline"))

        assertTrue(error is NexoraError.Network)
    }
}
