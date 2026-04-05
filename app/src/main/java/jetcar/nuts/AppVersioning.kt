package jetcar.nuts

object AppVersioning {
    fun compareVersions(left: String, right: String): Int {
        val leftParts = left.split('.').mapNotNull { it.toIntOrNull() }
        val rightParts = right.split('.').mapNotNull { it.toIntOrNull() }
        val maxSize = maxOf(leftParts.size, rightParts.size)

        for (index in 0 until maxSize) {
            val leftValue = leftParts.getOrElse(index) { 0 }
            val rightValue = rightParts.getOrElse(index) { 0 }
            if (leftValue != rightValue) {
                return leftValue.compareTo(rightValue)
            }
        }

        return 0
    }
}
