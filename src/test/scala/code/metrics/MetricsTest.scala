package code.metrics

import java.text.SimpleDateFormat
import java.util.Date

import code.api.ServerSetup
import code.bankconnectors.OBPLimit


/*
TODO. We need to test concurrent / multiple inserts.
 */


class MetricsTest extends ServerSetup with WipeMetrics {

  val testUrl1 = "http://example.com/foo"
  val testUrl2 = "http://example.com/bar"

  val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
  val day1 = dateFormatter.parse("2015-01-12T01:00:00")
  val day2 = dateFormatter.parse("2015-01-13T14:00:00")

  val startOfDayFormat = new SimpleDateFormat("yyyy-MM-dd")
  val startOfDay1 = startOfDayFormat.parse("2015-01-12")
  val startOfDay2 = startOfDayFormat.parse("2015-01-13")

  val metrics = APIMetrics.apiMetrics.vend

  val limit = 100

  override def beforeEach(): Unit = {
    super.beforeEach()
    wipeAllExistingMetrics()
  }

  //java dates are weird doing Date == subclassOfDate can evaluate to false even if they
  //represent the same point in time
  def dateEqual(date1 : Date, date2 : Date) : Boolean = {
    date1.compareTo(date2) == 0
  }

  def shouldBeEqual(date1 : Date, date2 : Date): Unit = {
    date1.compareTo(date2) must equal(0)
  }

  feature("API Metrics") {

    scenario("We save a new API metric") {
      metrics.saveMetric(testUrl1, day1, -1L)

      val byUrl = metrics.getAllMetrics(List(OBPLimit(limit))).groupBy(_.getUrl())

      byUrl.keys.size must equal(1)
      val metricsForUrl = byUrl(testUrl1)

      metricsForUrl.size must equal(1)

      val metric = metricsForUrl(0)
      shouldBeEqual(metric.getDate, day1)
      metric.getUrl() must equal(testUrl1)
    }

    scenario("Group all metrics by url") {
      metrics.saveMetric(testUrl1, day1, -1L)
      metrics.saveMetric(testUrl1, day1, -1L)
      metrics.saveMetric(testUrl1, day2, -1L)
      metrics.saveMetric(testUrl2, day2, -1L)

      val byUrl = metrics.getAllMetrics(List(OBPLimit(limit))).groupBy(_.getUrl())
      byUrl.keySet must equal(Set(testUrl1, testUrl2))

      val url1Metrics = byUrl(testUrl1)
      url1Metrics.size must equal(3)
      url1Metrics.count(_.getUrl() == testUrl1) must equal(3)
      url1Metrics.count(m => dateEqual(m.getDate(), day1)) must equal(2)
      url1Metrics.count(m => dateEqual(m.getDate(), day2)) must equal(1)

      val url2Metrics = byUrl(testUrl2)
      url2Metrics.size must equal(1)
      url2Metrics.count(_.getUrl() == testUrl2) must equal(1)
      url2Metrics.count(m => dateEqual(m.getDate(), day2)) must equal(1)
    }

    scenario("Group all metrics by day") {
      metrics.saveMetric(testUrl1, day1, -1L)
      metrics.saveMetric(testUrl1, day1, -1L)
      metrics.saveMetric(testUrl1, day2, -1L)
      metrics.saveMetric(testUrl2, day2, -1L)

      val byDay = metrics.getAllMetrics(List(OBPLimit(limit))).groupBy(APIMetrics.getMetricDay)
      byDay.keySet must equal(Set(startOfDay1, startOfDay2))

      val day1Metrics = byDay(startOfDay1)
      day1Metrics.size must equal(2)
      day1Metrics.count(m => dateEqual(m.getDate(), day1)) must equal(2)
      day1Metrics.count(_.getUrl() == testUrl1) must equal(2)

      val day2Metrics = byDay(startOfDay2)
      day2Metrics.size must equal(2)
      day2Metrics.count(m => dateEqual(m.getDate(), day2)) must equal(2)
      day2Metrics.count(_.getUrl() == testUrl1) must equal(1)
      day2Metrics.count(_.getUrl() == testUrl2) must equal(1)
    }

  }

}

/**
 * This trait provides a method to wipe all existing metrics
 *
 * If the APIMetrics implementation changes, this trait will need to change as well
 */
trait WipeMetrics {
  def wipeAllExistingMetrics() = {
    APIMetrics.apiMetrics.vend.bulkDeleteMetrics()
  }
}
