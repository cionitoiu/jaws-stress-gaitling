
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.scenario.Simulation

/**
  * Jaws stress testing using Gatling and TCPDS/TCPH data.
  */
class JawsSimulationNew extends Simulation {

  val httpProtocol = http
    .baseURL("http://localhost:9080")
    .inferHtmlResources()

  val headers_0 = Map(
    "Accept" -> "application/json",
    "Content-Type" -> "application/json")

  val headers_1 = Map(
    "Accept" -> "text/plain",
    "Content-Type" -> "text/plain")

  val uri1 = "http://localhost:9080/jaws"

  //add here the rest of TCPDS/TCPH queries each in a separate file
  val queryFiles = Array("JawsSimulation_0001_request.txt", "JawsSimulation_0002_request.txt")

  object JawsQueries {
    def runJawsQuery(queryFile: String, count: String) = {
      //println("queryFile: " + queryFile)
      exec(http("request_run_" + count)
        .post("/jaws/run?limited=true&numberOfResults=99")
        .headers(headers_1)
        .body(RawFileBody(queryFile)).check(status.is(200)).check(bodyString.transform(string => string).saveAs("queryId"))).pause(5)
    }
    def peekJawsQuery(queryId: String, count: String)= exec(http("request_log_" + count)
      .get("/jaws/logs?queryID=" + queryId + "&offset=0&limit=10")
      .headers(headers_0)
      .check(status.is(200)).check(jsonPath("$..status").saveAs("status")))

    def resultJawsQuery(queryId: String, count: String)= exec(http("request_res_"+count)
      .get("/jaws/results?queryID=" + queryId + "&offset=0&limit=10").headers(headers_0))

  }

  val fileName = "filename"
  val index = "index"

  //Following scenario is tested for each query listed in array:
  // execute a query, then loop over logs until query is finished and then fetch the results.
  //File name and intermediary counters are kept in HTTP session
  val scn = scenario("JawsSimulation").foreach(queryFiles.toSeq, "filename", "fileindex") {
    JawsQueries.runJawsQuery("${filename}", "${fileindex}")
      .exec(http("request_log_${fileindex}")
        .get("/jaws/logs?queryID=${queryId}&offset=0&limit=10").headers(headers_0)
        .check(status.is(200)).check(jsonPath("$..status").saveAs("status"))).pause(10)
      .asLongAs(condition = session => session("status").as[String].equals("IN_PROGRESS"), "counter", exitASAP = true) {
        pause(5).exec(http("request_log_${fileindex}${counter}")
          .get("/jaws/logs?queryID=${queryId}&offset=0&limit=10").headers(headers_0)
          .check(status.is(200)).check(jsonPath("$..status").saveAs("status")))
      }
      .exec(http("request_res_${fileindex}")
        .get("/jaws/results?queryID=${queryId}&offset=0&limit=10").headers(headers_0)).pause(5)
  }

  setUp(scn.inject(atOnceUsers(2), rampUsers(2) over (5 seconds))).protocols(httpProtocol)
}