import kotlin.math.*

/**
 * 3x3の行列
 * @property cols   3つの列を表すベクトル
 * @property rows   3つの行を表すベクトル
 * @property det    行列式
 * @property T      転置行列
 * @property inv    逆行列
 * @property isOrthogonal   直交行列かどうかの判定
 */
data class Matrix3(
    val m11: Double, val m12: Double, val m13: Double,
    val m21: Double, val m22: Double, val m23: Double,
    val m31: Double, val m32: Double, val m33: Double
) {

    // プロパティ
    val cols: List<Vector3> by lazy { toCols() }
    val rows: List<Vector3> by lazy { toRows() }
    val det: Double by lazy { computeDeterminant() }
    val T: Matrix3 by lazy { transpose() }
    val inv: Matrix3? by lazy { if (isOrthogonal == true) T else computeInverse() }
    var isOrthogonal: Boolean? = null
        get() {
            if (field == null) field = checkOrthonormal()
            return field
        }
        private set

    /** 演算子のオーバーロード Matrix3 + Matrix3 */
    operator fun plus(other: Matrix3) =
        Matrix3(
            m11 + other.m11, m12 + other.m12, m13 + other.m13,
            m21 + other.m21, m22 + other.m22, m23 + other.m23,
            m31 + other.m31, m32 + other.m32, m33 + other.m33
        )

    /** 演算子のオーバーロード Matrix3 - Matrix3 */
    operator fun minus(other: Matrix3) =
        Matrix3(
            m11 - other.m11, m12 - other.m12, m13 - other.m13,
            m21 - other.m21, m22 - other.m22, m23 - other.m23,
            m31 - other.m31, m32 - other.m32, m33 - other.m33
        )

    /** 演算子のオーバーロード Matrix3 * Double */
    operator fun times(scalar: Double) =
        Matrix3(
            this.m11 * scalar, this.m12 * scalar, this.m13 * scalar,
            this.m21 * scalar, this.m22 * scalar, this.m23 * scalar,
            this.m31 * scalar, this.m32 * scalar, this.m33 * scalar
        )

    /** 演算子のオーバーロード Matrix3 * Vector3 */
    operator fun times(vec: Vector3): Vector3 {
        val (row1, row2, row3) = this.toRows()
        return Vector3(row1 dot vec, row2 dot vec, row3 dot vec)
    }

    /** 演算子のオーバーロード Matrix3 * Matrix3 */
    operator fun times(other: Matrix3): Matrix3 {
        val (row1, row2, row3) = this.toRows()
        val (col1, col2, col3) = other.toCols()
        val productIsOrthonormal = when {
            this.isOrthogonal == null -> null
            other.isOrthogonal == null -> null
            else -> this.isOrthogonal!! && other.isOrthogonal!!
        }
        return Matrix3(
            row1 dot col1, row1 dot col2, row1 dot col3,
            row2 dot col1, row2 dot col2, row2 dot col3,
            row3 dot col1, row3 dot col2, row3 dot col3
        ).apply { isOrthogonal = productIsOrthonormal }
    }

    /** 行を取得 */
    private fun toRows() = listOf(Vector3(m11, m12, m13), Vector3(m21, m22, m23), Vector3(m31, m32, m33))

    /** 列を取得 */
    private fun toCols() = listOf(Vector3(m11, m21, m31), Vector3(m12, m22, m32), Vector3(m13, m23, m33))

    /** 転置行列の計算 */
    private fun transpose() =
        Matrix3(
            m11, m21, m31,
            m12, m22, m32,
            m13, m23, m33
        ).also { it.isOrthogonal = this.isOrthogonal }

    /** 行列式の計算 */
    private fun computeDeterminant(): Double =
        m11 * m22 * m33 + m12 * m23 * m31 + m13 * m21 * m32 - m13 * m22 * m31 - m11 * m23 * m32 - m12 * m21 * m33

    /** 逆行列の計算 */
    private fun computeInverse(): Matrix3? = when {
        det.isClose(0.0) -> null
        else ->
            Matrix3(
                m22 * m33 - m32 * m23, -(m12 * m33 - m32 * m13), m12 * m23 - m22 * m13,
                -(m21 * m33 - m31 * m23), m11 * m33 - m31 * m13, -(m11 * m23 - m21 * m13),
                m21 * m32 - m31 * m22, -(m11 * m32 - m31 * m12), m11 * m22 - m21 * m12
            ) * (1 / det)
    }

    /** 正規直交行列になっているかどうかのチェック */
    private fun checkOrthonormal(): Boolean {
        val (i, j, k) = toCols()
        return i.isUnit && j.isUnit && k.isUnit
                && (i dot j).isClose(0.0) && (j dot k).isClose(0.0) && (k dot i).isClose(0.0)
    }

    /** ２つの行列が等しいことを判定する */
    fun isClose(other: Matrix3) =
        m11.isClose(other.m11) && m12.isClose(other.m12) && m13.isClose(other.m13) &&
                m21.isClose(other.m21) && m22.isClose(other.m22) && m23.isClose(other.m23) &&
                m31.isClose(other.m31) && m32.isClose(other.m32) && m33.isClose(m33)

    /** 表示用の文字列に変換 */
    override fun toString(): String {
        val precision = 8
        val elements = listOf(m11, m12, m13, m21, m22, m23, m31, m32, m33)
        val nDigits = max(log10(elements.max()!!).toInt() + 1, 1)
        val length = (if (elements.any { it < 0 }) 1 else 0) + nDigits + 1 + precision
        val strings = elements.map { it.format(length, precision) }
        val maxLength = strings.map { it.length }.max()!!
        return strings
            .map { it.padEnd(maxLength, ' ') }
            .chunked(3).map { it.joinToString(" ", "[", "]") }
            .joinToString("\n ", "[", "]")
    }

    /**
     * 行列の生成に使う機能
     */
    companion object {
        /** ゼロ行列 */
        val zero = Matrix3(
            0.0, 0.0, 0.0,
            0.0, 0.0, 0.0,
            0.0, 0.0, 0.0
        ).apply { isOrthogonal = false }

        /** 単位行列 */
        val identity = Matrix3(
            1.0, 0.0, 0.0,
            0.0, 1.0, 0.0,
            0.0, 0.0, 1.0
        ).apply { isOrthogonal = true }

        /** ３つの行から行列を作成する */
        fun ofRows(row1: Vector3, row2: Vector3, row3: Vector3) =
            Matrix3(
                row1.x, row1.y, row1.z,
                row2.x, row2.y, row2.z,
                row3.x, row3.y, row3.z
            )

        /** ３つの列から行列を作成する */
        fun ofCols(col1: Vector3, col2: Vector3, col3: Vector3) =
            Matrix3(
                col1.x, col2.x, col3.x,
                col1.y, col2.y, col3.y,
                col1.z, col2.z, col3.z
            )

        /** -1.0~1.0の成分を持つランダムな行列を作成する */
        fun random() = Matrix3.ofRows(Vector3.random(), Vector3.random(), Vector3.random())

        /**
         * 対角行列を作成する。
         */
        fun createDiag(x: Double, y: Double, z: Double) =
            Matrix3(
                x, 0.0, 0.0,
                0.0, y, 0.0,
                0.0, 0.0, z
            )

        /**
         * スケーリングを表す行列を作成する。
         */
        fun createScale(axis: Vector3, scale: Double): Matrix3 {
            val (x, y, z) = axis.unit
            return Matrix3(
                1 + (scale - 1) * x * x, (scale - 1) * x * y, (scale - 1) * z * x,
                (scale - 1) * x * y, 1 + (scale - 1) * y * y, (scale - 1) * y * z,
                (scale - 1) * z * x, (scale - 1) * y * z, 1 + (scale - 1) * z * z
            )
        }

        /**
         * 座標系を表す行列を作成する。
         */
        fun createCsys(i: Vector3, j: Vector3): Matrix3 {
            val jOrtho = j - i.unit * (i.unit dot j)    // iに垂直になるように修正したj
            val k = i.unit cross jOrtho.unit
            return Matrix3.ofCols(i.unit, jOrtho.unit, k).apply { isOrthogonal = true }
        }

        /**
         * 回転軸と回転角度から回転行列を作成する。
         * [ロドリゲスの回転公式 - Wikipedia](https://ja.wikipedia.org/wiki/ロドリゲスの回転公式)
         */
        fun createRotation(axis: Vector3, angle: Double): Matrix3? {
            if (axis.isClose(Vector3.zero)) {
                return null
            } else {
                val (x, y, z) = axis.unit
                val cos_ = cos(angle)
                val sin_ = sin(angle)
                return Matrix3(
                    cos_ + x * x * (1 - cos_), x * y * (1 - cos_) - z * sin_, z * x * (1 - cos_) + y * sin_,
                    x * y * (1 - cos_) + z * sin_, cos_ + y * y * (1 - cos_), y * z * (1 - cos_) - x * sin_,
                    z * x * (1 - cos_) - y * sin_, y * z * (1 - cos_) + x * sin_, cos_ + z * z * (1 - cos_)
                ).apply { isOrthogonal = true }
            }
        }
    }
}
