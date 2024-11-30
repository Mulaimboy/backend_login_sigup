package com.application

import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import io.ktor.server.plugins.cors.routing.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; isLenient = true })
    }
    install(CORS) {
        anyHost()
        allowNonSimpleContentTypes = true
    }
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
                call.respondText("Login successful!", status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
            } else {
                call.respondText("Invalid username or password", status = HttpStatusCode.Unauthorized, contentType = ContentType.Application.Json)
            }
        }
        post("/customer/sign-up") {
            val signUpRequest = call.receive<SignUpRequest>()
            val isSuccessful = signUp(signUpRequest.username, signUpRequest.password, signUpRequest.email, signUpRequest.role);
            if (isSuccessful) {
                call.respondText("Sign Up successful!", status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
            } else {
                call.respondText("User Already Exists", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
            }
        }
        post("/admin/login")
        {
            val loginRequest = call.receive<AdminLogin>()
            val isValid = verifyAdminLogin(loginRequest.username, loginRequest.password)
            if (isValid) {
                call.respondText("Sign Up successful!", status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
            } else {
                call.respondText("Wrong Email or Password", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
            }

        }

    }
}

fun getConnection(): Connection? {
    val dotenv = dotenv()  // Load the .env file

    try {
        // Retrieve the database connection parameters from the .env file
        val url = dotenv["DB_URL"] ?: "jdbc:mysql://localhost:3306/hms"  // Default if not set
        val user = dotenv["DB_USER"] ?: "root"  // Default if not set
        val password = dotenv["DB_PASSWORD"] ?: "password"  // Default if not set

        // Create and return the connection to the database
        return DriverManager.getConnection(url, user, password)
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
@Serializable
data class SignUpRequest(val email: String, val username: String, val password: String, val role: String)
@Serializable
data class CountryName(val countryName: String)
@Serializable
data class AdminLogin (val username : String,val password: String)

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
            "SELECT review.comment FROM review " +
                 "JOIN customer ON " +
                 "review.customer_id = customer.customer_id " +
                 "WHERE customer.customer_id = ?"
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
            "SELECT * FROM customer_login WHERE username = ? AND password = ?"
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

fun verifyAdminLogin(username: String, password: String): Boolean {
    val connection = getConnection()
    if (connection != null) {
        val statement = connection.prepareStatement(
            "SELECT * FROM admin WHERE username = ? AND password = ?"
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


fun signUp(username: String, password: String, email: String, role: String) : Boolean {
    val connection = getConnection();
    if (connection != null) {
        if(role == "guest") {
            val statement = connection.prepareStatement(
                "Select * from customer_login where username =?"
            )
            statement.setString(1, username)
            val resultSet = statement.executeQuery()
            if(resultSet.fetchSize != 0) {
                connection.close()
                return false
            }
            else {
                val insertStatement = connection.prepareStatement(
                    "INSERT INTO customer_login (username, password) VALUES (?, ?)"
                )
                insertStatement.setString(1, username)
                insertStatement.setString(2, password)
                insertStatement.executeUpdate()
                insertStatement.close()
                connection.close()
                return true
            }
        }
        else {
            val statement = connection.prepareStatement(
                "Select * from hotel_manager_login where username =?"
            )
            statement.setString(1, username)
            val resultSet = statement.executeQuery()
            if(resultSet.fetchSize != 0) {
                connection.close()
                return false
            }
            else {
                val insertStatement = connection.prepareStatement(
                    "INSERT INTO hotel_manager_login (username, password) VALUES (?, ?)"
                )
                insertStatement.setString(1, username)
                insertStatement.setString(2, password)
                insertStatement.executeUpdate()
                insertStatement.close()
                connection.close()
                return true
            }
        }
    }
    return false;
}