package com.womenwhocode.web

import org.http4s._
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import io.circe.generic.auto._
import org.http4s.client.blaze.PooledHttp1Client
import org.http4s.dsl._

object Webapp {

  val httpClient = PooledHttp1Client()

  case class PostcodeResult(postcode: String, country: String, region: String, codes: CodeDetail)
  case class PostcodeResponse(status: Int, result: PostcodeResult)
  case class CodeDetail(admin_district: String, admin_county: String)
  case class MultiPostcodeResponse(status: Int, result: List[PostcodeResult])

  implicit val formats = org.json4s.DefaultFormats

  implicit val decoder = jsonOf[PostcodeResponse]
  implicit val encoder = jsonEncoderOf[PostcodeResponse]
  implicit val multiDecoder = jsonOf[MultiPostcodeResponse]
  implicit val multiEncoder = jsonEncoderOf[MultiPostcodeResponse]
  //implicit val postcodeMapEncoder = jsonEncoderOf[Map[String, List[String]]] //Solution: to using an encoder instead of Serialization directly
  implicit val postcodeMapEncoder = jsonEncoderOf[Map[String, List[(String, String)]]]
  implicit val multiPostcodeReqDecoder = jsonOf[Map[String, List[String]]]
  implicit val multiPostcodeReqEncoder = jsonEncoderOf[Map[String, List[String]]]

  val service = HttpService {
    case GET -> Root =>
      Ok(s"Hello")

    case GET -> Root / "locations" =>
      Ok(s"locations endpoint")

    case GET -> Root / "locations" / postcode =>
      val getRequestTask = httpClient.expect[PostcodeResponse](s"http://api.postcodes.io/postcodes/${postcode}")
      getRequestTask.flatMap(json => Ok(json))

    case GET -> Root / "locations" / postcode / "nearest" =>
      def responseJson(response: MultiPostcodeResponse) = {
        Map("postcodes" -> response.result.map(postcodeRes => (postcodeRes.postcode, postcodeRes.region)))
      }

      val getRequestTask = httpClient.expect[MultiPostcodeResponse](s"http://api.postcodes.io/postcodes/${postcode}/nearest")
      getRequestTask.flatMap(response => Ok(responseJson(response)))

    case req @ POST -> Root / "locations" / "nearest" =>
      req.as[Map[String, List[String]]] flatMap ( json => Ok(json))
  }
}