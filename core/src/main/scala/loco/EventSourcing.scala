package loco

import java.time.Instant
import java.util.concurrent.TimeUnit
import cats.{Monad, Order}
import cats.data.{Chain, NonEmptyList}
import cats.effect.{Clock, Sync}
import loco.ErrorReporter._
import loco.command.{Command, FailedCommand, SuccessCommand}
import loco.domain._
import loco.repository.EventsRepository
import loco.view._

import scala.language.higherKinds

/**
 * Manages saving events and fetching aggregates, handling commands, etc.
 */
trait EventSourcing[F[_], E <: Event, A <: Aggregate[E]] {

  /**
   * Saves the given `events` for an aggregate with given `id`. `lastKnownVersion` acts as an optimistic lock guard.
   * Next versions for the given `events` are generated by incrementing given `lastKnownVersion`. Should fail in case an
   * event with some id and version is already exist.
   */
  def saveEvents(
      events: NonEmptyList[E],
      id: AggregateId[E] = AggregateId.random,
      lastKnownVersion: AggregateVersion[E] = AggregateVersion.none): F[AggregateId[E]]

  /**
   * Saves the given `meta events`. Report back all errors
   */
  def saveMetaEvents(events: NonEmptyList[MetaEvent[E]]): F[NonEmptyList[(AggregateId[E], Option[Throwable])]]

  /**
   * Executes the given command and returns it result.
   */
  def executeCommand[R](id: AggregateId[E], command: Command[F, E, A, R]): F[R]

  /**
   * Builds a meta aggregate(aggregate and version) for given `id`. In case there are no events associated with the
   * given `id` - None is returned.
   */
  def fetchMetaAggregate(id: AggregateId[E]): F[Option[MetaAggregate[E, A]]]
}

class DefaultEventSourcing[F[_], E <: Event, A <: Aggregate[E]](
    builder: MetaAggregateBuilder[E, A],
    repository: EventsRepository[F, E],
    view: View[F, E])(implicit C: Clock[F], S: Sync[F], ER: ErrorReporter[F])
    extends EventSourcing[F, E, A] {

  import cats.implicits._

  override def saveEvents(
      events: NonEmptyList[E],
      id: AggregateId[E] = AggregateId.random,
      lastKnownVersion: AggregateVersion[E] = AggregateVersion.none): F[AggregateId[E]] = {
    for {
      instant <- C.realTime.map(d => Instant.ofEpochMilli(d.toMillis))
      metaEvents = MetaEvent.fromRawEvents(id, instant, lastKnownVersion, events)
      res <- saveMetaEvents(metaEvents).map(_.find(_._1 == id).head)
      _ <- res._2.fold(S.unit)(S.raiseError)
    } yield res._1
  }

  override def fetchMetaAggregate(id: AggregateId[E]): F[Option[MetaAggregate[E, A]]] = {
    repository.fetchEvents(id).compile.fold(builder.empty(id))((agr, event) => builder(agr, event)).map { aggregate =>
      if (aggregate.version.version == 0) {
        None
      } else {
        Some(aggregate)
      }
    }
  }

  override def executeCommand[R](id: AggregateId[E], command: Command[F, E, A, R]): F[R] = {
    def save(version: AggregateVersion[E], events: Chain[E]): F[AggregateId[E]] = {
      NonEmptyList.fromList(events.toList).map(saveEvents(_, id, version)).getOrElse(id.pure[F])
    }

    for {
      metaAggregate <- fetchMetaAggregate(id).map(_.getOrElse(builder.empty(id)))
      commandResult <- command.events(metaAggregate.aggregate)
      result <- commandResult match {
        case SuccessCommand(result, events) =>
          save(metaAggregate.version, events) *> Monad[F].pure(result)
        case FailedCommand(exception, events) =>
          save(metaAggregate.version, events) *> Sync[F].raiseError[R](exception)
      }
    } yield result
  }

  override def saveMetaEvents(
      metaEvents: NonEmptyList[MetaEvent[E]]): F[NonEmptyList[(AggregateId[E], Option[Throwable])]] = {
    val saves = metaEvents
      .toList
      .groupBy(_.aggregateId)
      .map {
        case (id, events) =>
          (for {
            _ <- repository.saveEvents(NonEmptyList.fromListUnsafe(events))
            _ <- view.handle(NonEmptyList.fromListUnsafe(events)).reportError
          } yield id -> Option.empty[Throwable]).recover {
            case ex =>
              id -> Option(ex)
          }
      }
      .toList

    saves.sequence.map(NonEmptyList.fromListUnsafe)
  }

}

object DefaultEventSourcing {
  def apply[F[_], E <: Event, A <: Aggregate[E]](
      aggregateBuilder: AggregateBuilder[A, E],
      repository: EventsRepository[F, E],
      view: View[F, E],
      ER: ErrorReporter[F])(implicit C: Clock[F], S: Sync[F]): DefaultEventSourcing[F, E, A] = {
    val metaAggregateBuilder = new MetaAggregateBuilder[E, A](aggregateBuilder)
    new DefaultEventSourcing(metaAggregateBuilder, repository, view)(C, S, ER)
  }
}
