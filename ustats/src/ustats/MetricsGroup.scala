package ustats

import types.DoubleAdder
import types.ConcurrentHashMap

class MetricsGroup[A](
  labels: Seq[String],
  mkNew: Seq[Any] => A
) extends Selectable:
  private val all = new ConcurrentHashMap[Seq[Any], A]

  def applyDynamic(f: String)(labelValues: Any*): A =
    all.computeIfAbsent(labelValues, values => mkNew(values))

  def labelled(labelValues: Any*) =
    require(labelValues.size == labels.size, "label size mismatch")
    applyDynamic("")(labelValues*)

object MetricsGroup:
  import scala.quoted.Expr
  import scala.quoted.Quotes
  import scala.quoted.Type
  import scala.quoted.Varargs

  transparent inline final def refine[A](inline labels: Seq[String], mkNew: Seq[Any] => A): MetricsGroup[A] =
    ${refineImpl[A]('labels, 'mkNew)}

  private def refineImpl[A](using qctx: Quotes, tpe: Type[A])(
    labels: Expr[Seq[String]],
    mkNew: Expr[Seq[Any] => A]
  ): Expr[MetricsGroup[A]] =
    import qctx.reflect.*

    val strings: List[String] = labels.value match
      case None =>
        report.error("labels must be statically known at compile time", labels)
        Nil
      case Some(xs) => xs.toList

    val mt = MethodType(strings)(
      _ => strings.map(_ => TypeRepr.of[Any]),
      _ => TypeRepr.of[A]
    )

    val tpe = Refinement(
      TypeRepr.of[MetricsGroup[A]],
      "apply",
      mt
    ).asType

    tpe match
      case '[tpe] =>
        '{MetricsGroup[A](${Expr(strings)}, $mkNew).asInstanceOf[MetricsGroup[A] & tpe]}

