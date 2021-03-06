package io.prediction.output.itemsim

import io.prediction.commons.Config
import io.prediction.commons.modeldata.ItemSimScore
import io.prediction.commons.settings.{ Algo, App, Engine, OfflineEval }

import scala.util.Random

import com.github.nscala_time.time.Imports._

trait ItemSimAlgoOutput {
  def output(iid: String, n: Int, itypes: Option[Seq[String]])(implicit app: App, algo: Algo, offlineEval: Option[OfflineEval]): Iterator[String]
}

object ItemSimAlgoOutput {
  val config = new Config

  def output(iid: String, n: Int, itypes: Option[Seq[String]], latlng: Option[Tuple2[Double, Double]], within: Option[Double], unit: Option[String])(implicit app: App, engine: Engine, algo: Algo, offlineEval: Option[OfflineEval] = None): Seq[String] = {
    val items = offlineEval map { _ => config.getAppdataTrainingItems } getOrElse { config.getAppdataItems }

    /** Serendipity settings. */
    val serendipity = engine.params.get("serendipity").map { _.asInstanceOf[Int] }

    /**
     * Serendipity value (s) from 0-10 in engine settings.
     * Implemented as randomly picking items from top n*(s+1) results.
     */
    val finalN = serendipity.map { s => n * (s + 1) }.getOrElse(n)

    /**
     * At the moment, PredictionIO depends only on MongoDB for its model data storage.
     * Since we are still using the legacy longitude-latitude format, the maximum number
     * of documents that can be returned from a query with geospatial constraint is 100.
     * A "manual join" is still feasible with this size.
     */
    val (iids, iidsCopy): (Iterator[String], Iterator[String]) = latlng.map { ll =>
      val geoItems = items.getByAppidAndLatlng(app.id, ll, within, unit).map(_.id).toSet
      // use n = 0 to return all available iids for now
      ItemSimCFAlgoOutput.output(iid, 0, itypes).filter { geoItems(_) }
    }.getOrElse {
      // use n = 0 to return all available iids for now
      ItemSimCFAlgoOutput.output(iid, 0, itypes)
    }.duplicate

    /** Start and end time filtering. */
    val itemsForTimeCheck = items.getByIds(app.id, iidsCopy.toSeq)
    val iidsWithValidTimeSet = (itemsForTimeCheck filter { item =>
      (item.starttime, item.endtime) match {
        case (Some(st), None) => DateTime.now >= st
        case (None, Some(et)) => DateTime.now <= et
        case (Some(st), Some(et)) => st <= DateTime.now && DateTime.now <= et
        case _ => true
      }
    } map { _.id }).toSet
    val iidsWithValidTime: Iterator[String] = iids.filter { iidsWithValidTimeSet(_) }

    /** At this point "output" is guaranteed to have n*(s+1) items (seen or unseen) unless model data is exhausted. */
    val output = iidsWithValidTime.take(finalN).toList

    /** Serendipity output. */
    val serendipityOutput = serendipity.map { s =>
      if (s > 0)
        Random.shuffle(output).take(n)
      else
        output
    } getOrElse output

    /**
     * Freshness (0 <= f <= 10) is implemented as the ratio of final results being top N results re-sorted by start time.
     * E.g. For f = 4, 40% of the final output will consist of top N results re-sorted by start time.
     */
    val freshness = engine.params.get("freshness") map { _.asInstanceOf[Int] }

    /** Freshness output. */
    val finalOutput = freshness map { f =>
      if (f > 0) {
        val freshnessN = scala.math.round(n * f / 10)
        val otherN = n - freshnessN
        val freshnessOutput = items.getRecentByIds(app.id, output).map(_.id)
        val finalFreshnessOutput = freshnessOutput.take(freshnessN)
        val finalFreshnessOutputSet = finalFreshnessOutput.toSet
        finalFreshnessOutput ++ (serendipityOutput filterNot { finalFreshnessOutputSet(_) }).take(otherN)
      } else
        serendipityOutput
    } getOrElse serendipityOutput

    finalOutput
  }

}
