package com.application
import io.github.cdimascio.dotenv.dotenv
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*


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
        post("/fetch-hotel-rooms") {
            val managerId = call.receive<ManagerId>()
            val data = fetchHotelRooms(managerId.managerId)
            call.respond(data)// Respond with the list directly as JSON
        }
        get("/fetch-country-name") {
            val data = fetchCountryName(3)
            call.respondText(data, ContentType.Application.Json)
        }
        get("/fetch-review-comment") {
            val customerId = call.receive<CustomerId>();
            val data = fetchReviewComment(customerId.id);
            call.respondText(data, ContentType.Application.Json)
        }
        post("/fetch-hotels")
        {
            val data= fetchHotels();
            call.respond(data);
        }
        post("/customer/login") {
            val loginRequest = call.receive<LoginRequest>()
            val isValid = verifyLogin(loginRequest.username, loginRequest.password, loginRequest.role)
            if (isValid != null) {
                call.respondText(
                    """{
                        "message": "Login successful!",
                        "data": $isValid
                    }""",
                    status = HttpStatusCode.OK,
                    contentType = ContentType.Application.Json
                )
            } else {
                call.respondText(
                    "Invalid username or password",
                    status = HttpStatusCode.Unauthorized,
                    contentType = ContentType.Application.Json
                )
            }
        }
        post("/fetch-hotelroomdetails")
        {
            val hotelName=call.receive<HotelName>()
            val data = fetchHotelRoomsbyName(hotelName.Hotelname);
            call.respond(data);

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
        post("/hotel-manage/add-room")
        {
            val addRoom= call.receive<RoomDetails>()
            val RoomAdded = AddRoom(addRoom.Roomtype,addRoom.description,addRoom.price,addRoom.availability,addRoom.managerId,addRoom.amenityId)
            if(RoomAdded){
                call.respondText("Sign Up successful!", status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
        } else {
        call.respondText("Wrong Email or Password", status = HttpStatusCode.BadRequest, contentType = ContentType.Application.Json)
    }
        }
        post ("/customer/hotelroomdetails/booking"){
            val bookingDetails =call.receive<BookingDetails>()
            val isSuccess= setBookingDetail(bookingDetails.hotel_room_id,bookingDetails.customer_id,bookingDetails.checking_date,bookingDetails.checkout_date);
            if(isSuccess)
            {
                call.respondText("Succesfully Booked", status = HttpStatusCode.OK, contentType = ContentType.Application.Json)
            }
            else{
                call.respondText("Not Found", status = HttpStatusCode.NotFound, contentType = ContentType.Application.Json)
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
data class CustomerId(val id: Int)
@Serializable
data class ManagerId(val managerId: Int)
@Serializable
data class SignUpRequest(val email: String, val username: String, val password: String, val role: String)
@Serializable
data class CountryName(val countryName: String)
@Serializable
data class AdminLogin (val username : String,val password: String)
@Serializable
data class RoomDetails(val Roomtype :Int,val availability :String,val description : String , val price : Double ,val managerId: Int,val amenityId:Int)
@Serializable
data class HotelDetails(val Hotelname:String ,val Description :String);
@Serializable
data class HotelName(val Hotelname: String);
@Serializable
data class RoomDetailsmeow(val Roomtype :String,val availability :String,val description : String , val price : Double ,val hotelName: String,val amenityname:String,val Hotelroomid:Int)
@Serializable
data class BookingDetails(val hotel_room_id:Int, val customer_id:Int,val checking_date:String,val checkout_date:String);
fun setBookingDetail(hotel_room_id: Int,customerId: Int,checking_date: String,checkout_date: String):Boolean
{
    val connection= getConnection()
    if (connection != null) {
        val statement= connection.prepareStatement(
            "Insert Into booking(customer_id,hotel_room_id,booking_date,check_in_date,check_out_date,status_id) values (?,?,?,?,?,?)"
        )
        val currentTimestamp: String = Timestamp.valueOf(LocalDateTime.now()).toString()
        statement.setInt(1,customerId);
        statement.setInt(2,hotel_room_id)
        statement.setString(3, currentTimestamp);
        statement.setString(4, checking_date);
        statement.setString(5, checkout_date);
        statement.setInt(6,1);
        val result = statement.executeUpdate();
        statement.close()
        connection.close()
        return true;
    }
    return false;
}
fun fetchHotelRoomsbyName(hotelName: String): List<RoomDetailsmeow> {
    val hotelDetailsList = mutableListOf<RoomDetailsmeow>()
    val connection = getConnection()

    if (connection != null) {
        // Fetch the hotel ID based on the hotel name
        val statement = connection.prepareStatement(
            "SELECT hotel_id FROM hotels WHERE hotel_name = ?"
        )
        statement.setString(1, hotelName)
        val result = statement.executeQuery()

        if (result.next()) {
            val hotelId = result.getInt("hotel_id")

            // Prepare and execute the query to fetch room details
            val newstatement = connection.prepareStatement(
                """
                SELECT rt.description, 
                       hnr.price_per_night, 
                       a.amenity_name, 
                       hnr.availability_status, 
                       rt.type_name, 
                       h.hotel_name, 
                       hnr.hotel_room_id
                FROM hotel_newroom hnr
                JOIN room_type rt ON hnr.room_type_id = rt.room_type_id
                JOIN room_amenities ra ON hnr.hotel_room_id = ra.hotel_room_id
                JOIN amenities a ON a.amenity_id = ra.amenity_id
                JOIN hotels h ON hnr.hotel_id = h.hotel_id
                WHERE h.hotel_id = ?
                """
            )
            newstatement.setInt(1, hotelId)
            val newResult = newstatement.executeQuery()

            // Process the result set
            while (newResult.next()) {
                val roomDetails = RoomDetailsmeow(
                    description = newResult.getString("description"),
                    price = newResult.getDouble("price_per_night"),
                    amenityname = newResult.getString("amenity_name"),
                    availability = newResult.getString("availability_status"),
                    Roomtype = newResult.getString("type_name"),
                    hotelName = newResult.getString("hotel_name"),
                    Hotelroomid = newResult.getInt("hotel_room_id")
                )
                hotelDetailsList.add(roomDetails)
            }
            newResult.close()
            newstatement.close()
        }
        result.close()
        statement.close()
        connection.close()
    }
    return hotelDetailsList
}

fun fetchHotels():List<HotelDetails>
{
    val HotelDetailsList = mutableListOf<HotelDetails>()
    val connection = getConnection()
    if (connection != null) {
        val statement = connection.prepareStatement(
            "SELECT * FROM hotels"
        )
        val resultSet=statement.executeQuery();
        while (resultSet.next()) {
            val roomDetails = HotelDetails(
                Description = resultSet.getString("description"),
                Hotelname = resultSet.getString("hotel_name")
            )
            HotelDetailsList.add(roomDetails)
        }
        resultSet.close()
        statement.close()
        connection.close()

    }
    return HotelDetailsList;
}

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
data class HotelRoomDetails(
    val description: String,
    val pricePerNight: Double,
    val amenityName: String,
    val availabilityStatus: String,
    val typeName: String,
    val hotelName: String
)

fun fetchHotelRooms(managerId: Int): List<HotelRoomDetails> {
    val connection = getConnection()
    val roomDetailsList = mutableListOf<HotelRoomDetails>()

    if (connection != null) {
        val statement = connection.prepareStatement(
            "SELECT rt.description, hnr.price_per_night, a.amenity_name, hnr.availability_status, rt.type_name, h.hotel_name " +
                    "FROM hotel_newroom hnr " +
                    "JOIN room_type rt ON hnr.room_type_id = rt.room_type_id " +
                    "JOIN room_amenities ra ON hnr.hotel_room_id = ra.hotel_room_id " +
                    "JOIN amenities a ON a.amenity_id = ra.amenity_id " +
                    "JOIN hotels h ON hnr.hotel_id = h.hotel_id " +
                    "JOIN hotel_manager hm ON h.manager_id = hm.manager_id " +
                    "WHERE hm.manager_id = ?;"
        )
        statement.setInt(1, managerId)
        val resultSet = statement.executeQuery()

        while (resultSet.next()) {
            val roomDetails = HotelRoomDetails(
                description = resultSet.getString("description"),
                pricePerNight = resultSet.getDouble("price_per_night"),
                amenityName = resultSet.getString("amenity_name"),
                availabilityStatus = resultSet.getString("availability_status"),
                typeName = resultSet.getString("type_name"),
                hotelName = resultSet.getString("hotel_name")
            )
            roomDetailsList.add(roomDetails)
        }
        resultSet.close()
        statement.close()
        connection.close()
    }
    return roomDetailsList
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
data class LoginRequest(val username: String, val password: String,val role: String)

fun verifyLogin(username: String, password: String,role: String): String? {
    val connection = getConnection()
    if (connection != null) {
        try {
            // Check customer login
            if(role=="customer")
            {
                val customerStatement = connection.prepareStatement(
                    "SELECT customer_id FROM customer_login WHERE username = ? AND password = ?"
                )
                customerStatement.setString(1, username)
                customerStatement.setString(2, password)
                val customerResultSet = customerStatement.executeQuery()
                if(customerResultSet.next())
                {
                    val customerId = customerResultSet.getString("customer_id")
                    connection.close()
                    return customerId
                }


            }
            else {
                // Check manager login
                val managerStatement = connection.prepareStatement(
                    "SELECT manager_id FROM hotel_manager_login WHERE username = ? AND password = ?"
                )
                managerStatement.setString(1, username)
                managerStatement.setString(2, password)
                val managerResultSet = managerStatement.executeQuery()

                if (managerResultSet.next()) {
                    val managerId = managerResultSet.getString("manager_id")
                    connection.close()
                    return managerId
                }
            }
            connection.close()
            return null
        } catch (e: Exception) {
            connection.close()
            throw e
        }
    }
    return null
}
fun AddRoom(Roomtype: Int, description: String, price: Double, availability: String, managerId: Int, amenityId: Int): Boolean {
    val connection = getConnection()
    if (connection != null) {
        // Get hotel_id based on manager_id
        val statement = connection.prepareStatement(
            "SELECT hotel_id FROM hotels WHERE manager_id = ?"
        )
        statement.setInt(1, managerId)
        val resultSet = statement.executeQuery()

        // Check if the query returned any result
        if (resultSet.next()) {
            val hotelId = resultSet.getInt("hotel_id")
            println("Hotel ID for Manager $managerId: $hotelId")

            // Insert new room
            val insertQuery = connection.prepareStatement(
                "INSERT INTO hotel_newroom(hotel_id, room_type_id, price_per_night, availability_status) VALUES (?, ?, ?, ?)"
            )
            insertQuery.setInt(1, hotelId)
            insertQuery.setInt(2, Roomtype)
            insertQuery.setBigDecimal(3, BigDecimal(price))
            insertQuery.setString(4, availability)
            insertQuery.executeUpdate()
            insertQuery.close()

            // Get the most recent hotel_room_id for the given hotel_id
            val roomStatement = connection.prepareStatement(
                "SELECT hotel_room_id FROM hotel_newroom WHERE hotel_id = ? ORDER BY hotel_room_id DESC LIMIT 1"
            )
            roomStatement.setInt(1, hotelId)
            val roomResultSet = roomStatement.executeQuery()

            // Ensure that a room exists and get the hotel_room_id
            if (roomResultSet.next()) {
                val hotelRoomId = roomResultSet.getInt("hotel_room_id")

                // Insert the amenity for the newly added room
                val insertAmenityQuery = connection.prepareStatement(
                    "INSERT INTO room_amenities(hotel_room_id, amenity_id) VALUES (?, ?)"
                )
                insertAmenityQuery.setInt(1, hotelRoomId)
                insertAmenityQuery.setInt(2, amenityId)
                insertAmenityQuery.executeUpdate()

                // Close the amenity insert statement
                insertAmenityQuery.close()
                roomStatement.close()
                connection.close()

                return true
            } else {
                // No room found, something went wrong
                println("Failed to retrieve the hotel_room_id.")
                roomStatement.close()
                connection.close()
                return false
            }
        } else {
            // No hotel found for the manager
            println("No hotel found for Manager $managerId.")
            statement.close()
            connection.close()
            return false
        }
    } else {
        println("Database connection failed.")
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