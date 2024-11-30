package com.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    routing {
        get("/test-db-connection") {
            val connection = getConnection()
            if (connection != null) {
                call.respondText("Database connection successful!")
                connection.close()
            } else {
                call.respondText("Database connection failed!")
            }
        }

        get("/fetch-country-name") {
            val data = fetchCountryName(3)
            call.respondText(data, ContentType.Application.Json)
        }

        get("/fetch-review-comment") {
            val data = fetchReviewComment(5)
            call.respondText(data, ContentType.Application.Json)
        }
        post("/customer/login") {
            val loginRequest = call.receive<LoginRequest>()
            val isValid = verifyLogin(loginRequest.username, loginRequest.password)
            if (isValid) {
                call.respondText("Login successful!", status = HttpStatusCode.OK)
            } else {
                call.respondText("Invalid username or password", status = HttpStatusCode.Unauthorized)
            }
        }
    }
}

fun getConnection(): Connection? {
    return try {
        val url = "jdbc:mysql://localhost:3306/hms"
        val user = "umrn"
        val password = "0505"
        DriverManager.getConnection(url, user, password)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Serializable
data class CountryName(val countryName: String)

fun fetchCountryName(customerId: Int): String {
    val connection = getConnection()
    if (connection != null) {
        val statement = connection.prepareStatement(
            "SELECT country.country_name FROM country " +
                    "JOIN city ON country.country_id = city.country_id " +
                    "JOIN address ON city.city_id = address.city_id " +
                    "JOIN customer ON customer.customer_id = address.customer_id " +
                    "WHERE customer.customer_id = ?"
        )
        statement.setInt(1, customerId)
        val resultSet = statement.executeQuery()
        val result = mutableListOf<CountryName>()
        while (resultSet.next()) {
            result.add(CountryName(resultSet.getString("country_name")))
        }
        connection.close()
        return Json.encodeToString(result)
    } else {
        return Json.encodeToString(mapOf("error" to "Failed to connect to the database"))
    }
}

@Serializable
data class ReviewComment(val comment: String)

fun fetchReviewComment(customerId: Int): String {
    val connection = getConnection()
    if (connection != null) {
        val statement = connection.prepareStatement(
            "select review.comment from review join customer on review.customer_id = customer.customer_id where customer.customer_id = ?"
        )
        statement.setInt(1, customerId)
        val resultSet = statement.executeQuery()
        val result = mutableListOf<ReviewComment>()
        while (resultSet.next()) {
            result.add(ReviewComment(resultSet.getString("comment")))
        }
        connection.close()
        return Json.encodeToString(result)
    } else {
        return Json.encodeToString(mapOf("error" to "Failed to connect to the database"))
    }
}

@Serializable
data class LoginRequest(val username: String, val password: String)

fun verifyLogin(username: String, password: String): Boolean {
    val connection = getConnection()
    if (connection != null) {
        val statement = connection.prepareStatement(
            "SELECT * FROM customer WHERE email = ? AND password = ?"
        )
        statement.setString(1, username)
        statement.setString(2, password)
        val resultSet = statement.executeQuery()
        resultSet.next()
        val count = resultSet.getInt(1)
        connection.close()
        return count > 0
    } else {
        return false
    }
}