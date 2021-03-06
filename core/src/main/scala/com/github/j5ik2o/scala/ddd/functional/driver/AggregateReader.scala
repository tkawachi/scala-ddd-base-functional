package com.github.j5ik2o.scala.ddd.functional.driver

trait AggregateReader extends AggregateIO {

  type SingleResultType[_]

  def resolveBy(id: AggregateIdType)(implicit ctx: IOContextType): DSL[SingleResultType[AggregateType]]

}
