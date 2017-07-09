package com.github.j5ik2o.scala.ddd.functional.slick

import cats.free.Free
import cats.~>
import com.github.j5ik2o.scala.ddd.functional.cats.{ FreeIODeleteFeature, FreeIORepositoryFeature }
import slick.jdbc.JdbcProfile

import scala.concurrent.{ ExecutionContext, Future }

class UserRepository(val driver: UserSlick3Driver)
    extends FreeIORepositoryFeature
    with FreeIODeleteFeature
    with CatsDBIOMonadInstance {

  import driver.profile.api._

  override type IdValueType     = Long
  override type AggregateIdType = UserId
  override type AggregateType   = User
  override type EvalType[A]     = DBIO[A]
  override type RealizeType[A]  = Future[A]
  override type IOContext       = ExecutionContext

  override lazy val interpreter: AggregateRepositoryDSL ~> EvalType =
    new (AggregateRepositoryDSL ~> EvalType) {
      override def apply[A](fa: AggregateRepositoryDSL[A]): EvalType[A] = fa match {
        case s @ Store(aggregate) =>
          driver.store(aggregate)(s.ctx).asInstanceOf[EvalType[A]]
        case r @ ResolveById(id) =>
          driver.resolveBy(id)(r.ctx).asInstanceOf[EvalType[A]]
        case d @ Delete(id) =>
          driver.deleteById(id)(d.ctx).asInstanceOf[EvalType[A]]
      }
    }

  override val profile: JdbcProfile = driver.profile

  override def realize[A](program: Free[AggregateRepositoryDSL, A])(implicit ctx: IOContext): RealizeType[A] =
    driver.db.run(eval(program))

  override def eval[A](program: Free[AggregateRepositoryDSL, A])(implicit ctx: IOContext): EvalType[A] = {
    program.foldMap(interpreter)
  }

}