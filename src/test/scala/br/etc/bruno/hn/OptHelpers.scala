package br.etc.bruno.hn

// on the API level, most of the fields are OPTIONAL
trait OptHelpers {

  implicit def longToOptionalLong(l: Long): Option[Long] = Some(l)

  implicit def longsToOptionalLongs(l: Seq[Long]): Option[Seq[Long]] = Some(l)

  implicit def stringToOptionalString(s: String): Option[String] = Some(s)

  implicit def booleanToOptionalBoolean(b: Boolean): Option[Boolean] = if (!b) None else Some(b)

}
