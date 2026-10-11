package com.finsim.core

import kotlin.math.abs

/**
 * Arbitrary precision integer supporting basic arithmetic in base-10 digits.
 */
class BigInt private constructor(
    val sign: Int, // -1, 0, 1
    val digits: IntArray // Most significant first, no leading zeros unless [0]
) : Comparable<BigInt> {

    companion object {
        val ZERO = BigInt(0, intArrayOf(0))
        val ONE = BigInt(1, intArrayOf(1))
        val TEN = BigInt(1, intArrayOf(1, 0))

        fun fromLong(value: Long): BigInt {
            if (value == 0L) return ZERO
            val sign = if (value < 0) -1 else 1
            var v = if (value < 0) -value else value
            if (v < 0) { // Long.MIN_VALUE overflow case
                return parse("9223372036854775808").negate()
            }
            val list = mutableListOf<Int>()
            while (v > 0) {
                list.add((v % 10).toInt())
                v /= 10
            }
            return BigInt(sign, list.reversed().toIntArray())
        }

        fun parse(str: String): BigInt {
            val s = str.trim()
            if (s.isEmpty() || s == "0" || s == "+0" || s == "-0") return ZERO
            val sign = when {
                s.startsWith("-") -> -1
                s.startsWith("+") -> 1
                else -> 1
            }
            val start = if (s.startsWith("-") || s.startsWith("+")) 1 else 0
            var firstNonZero = start
            while (firstNonZero < s.length && s[firstNonZero] == '0') {
                firstNonZero++
            }
            if (firstNonZero == s.length) return ZERO
            val len = s.length - firstNonZero
            val digits = IntArray(len)
            for (i in 0 until len) {
                val c = s[firstNonZero + i]
                require(c in '0'..'9') { "Invalid character in integer: $c" }
                digits[i] = c - '0'
            }
            return BigInt(sign, digits)
        }

        private fun createNormalized(sign: Int, digits: IntArray): BigInt {
            var firstNonZero = 0
            while (firstNonZero < digits.size && digits[firstNonZero] == 0) {
                firstNonZero++
            }
            if (firstNonZero == digits.size) return ZERO
            val len = digits.size - firstNonZero
            val trimmed = if (firstNonZero == 0) digits else digits.copyOfRange(firstNonZero, digits.size)
            return BigInt(if (sign == 0) 1 else sign, trimmed)
        }
    }

    val isZero: Boolean get() = sign == 0

    fun negate(): BigInt = if (isZero) this else BigInt(-sign, digits)

    override fun compareTo(other: BigInt): Int {
        if (this.sign != other.sign) return this.sign.compareTo(other.sign)
        if (this.sign == 0) return 0
        val magCmp = compareMagnitude(this.digits, other.digits)
        return if (this.sign > 0) magCmp else -magCmp
    }

    private fun compareMagnitude(a: IntArray, b: IntArray): Int {
        if (a.size != b.size) return a.size.compareTo(b.size)
        for (i in a.indices) {
            if (a[i] != b[i]) return a[i].compareTo(b[i])
        }
        return 0
    }

    operator fun plus(other: BigInt): BigInt {
        if (this.isZero) return other
        if (other.isZero) return this
        if (this.sign == other.sign) {
            val sumDigits = addMagnitudes(this.digits, other.digits)
            return BigInt(this.sign, sumDigits)
        }
        val magCmp = compareMagnitude(this.digits, other.digits)
        if (magCmp == 0) return ZERO
        return if (magCmp > 0) {
            BigInt(this.sign, subtractMagnitudes(this.digits, other.digits))
        } else {
            BigInt(other.sign, subtractMagnitudes(other.digits, this.digits))
        }
    }

    operator fun minus(other: BigInt): BigInt = this + other.negate()

    private fun addMagnitudes(a: IntArray, b: IntArray): IntArray {
        val maxLen = maxOf(a.size, b.size)
        val res = IntArray(maxLen + 1)
        var carry = 0
        var iA = a.size - 1
        var iB = b.size - 1
        var iR = res.size - 1
        while (iA >= 0 || iB >= 0 || carry > 0) {
            val sum = (if (iA >= 0) a[iA--] else 0) + (if (iB >= 0) b[iB--] else 0) + carry
            res[iR--] = sum % 10
            carry = sum / 10
        }
        return if (res[0] == 0) res.copyOfRange(1, res.size) else res
    }

    private fun subtractMagnitudes(a: IntArray, b: IntArray): IntArray {
        // Assumes magnitude a >= magnitude b
        val res = IntArray(a.size)
        var borrow = 0
        var iA = a.size - 1
        var iB = b.size - 1
        var iR = res.size - 1
        while (iA >= 0) {
            var diff = a[iA--] - (if (iB >= 0) b[iB--] else 0) - borrow
            if (diff < 0) {
                diff += 10
                borrow = 1
            } else {
                borrow = 0
            }
            res[iR--] = diff
        }
        var firstNonZero = 0
        while (firstNonZero < res.size && res[firstNonZero] == 0) {
            firstNonZero++
        }
        return if (firstNonZero == res.size) intArrayOf(0) else res.copyOfRange(firstNonZero, res.size)
    }

    operator fun times(other: BigInt): BigInt {
        if (this.isZero || other.isZero) return ZERO
        val a = this.digits
        val b = other.digits
        val res = IntArray(a.size + b.size)
        for (i in a.indices.reversed()) {
            var carry = 0
            for (j in b.indices.reversed()) {
                val prod = a[i] * b[j] + res[i + j + 1] + carry
                res[i + j + 1] = prod % 10
                carry = prod / 10
            }
            res[i] += carry
        }
        return createNormalized(this.sign * other.sign, res)
    }

    fun multiplyByPowerOfTen(power: Int): BigInt {
        if (isZero || power == 0) return this
        require(power > 0) { "Power must be non-negative" }
        val newDigits = IntArray(digits.size + power)
        digits.copyInto(newDigits, 0, 0, digits.size)
        return BigInt(sign, newDigits)
    }

    data class DivResult(val quotient: BigInt, val remainder: BigInt)

    fun divMod(divisor: BigInt): DivResult {
        require(!divisor.isZero) { "Division by zero" }
        if (this.isZero) return DivResult(ZERO, ZERO)
        val magCmp = compareMagnitude(this.digits, divisor.digits)
        if (magCmp < 0) return DivResult(ZERO, this)
        if (magCmp == 0) {
            val q = BigInt(this.sign * divisor.sign, intArrayOf(1))
            return DivResult(q, ZERO)
        }

        val dividend = this.digits
        val divDigits = divisor.digits
        val quotientDigits = mutableListOf<Int>()
        var remainder = IntArray(0)

        for (digit in dividend) {
            // Append digit to remainder
            remainder = if (remainder.size == 1 && remainder[0] == 0) {
                intArrayOf(digit)
            } else {
                val nextRem = IntArray(remainder.size + 1)
                remainder.copyInto(nextRem, 0, 0, remainder.size)
                nextRem[remainder.size] = digit
                nextRem
            }
            // Trim leading zeros in remainder
            var start = 0
            while (start < remainder.size - 1 && remainder[start] == 0) start++
            if (start > 0) remainder = remainder.copyOfRange(start, remainder.size)

            // Find max q in 0..9 such that divDigits * q <= remainder
            var q = 0
            for (candidate in 9 downTo 1) {
                val prod = multiplyMagnitudeByInt(divDigits, candidate)
                if (compareMagnitude(prod, remainder) <= 0) {
                    q = candidate
                    remainder = subtractMagnitudes(remainder, prod)
                    break
                }
            }
            quotientDigits.add(q)
        }

        val qInt = createNormalized(this.sign * divisor.sign, quotientDigits.toIntArray())
        val rInt = createNormalized(this.sign, remainder)
        return DivResult(qInt, rInt)
    }

    private fun multiplyMagnitudeByInt(mag: IntArray, factor: Int): IntArray {
        if (factor == 0) return intArrayOf(0)
        if (factor == 1) return mag.copyOf()
        val res = IntArray(mag.size + 1)
        var carry = 0
        for (i in mag.indices.reversed()) {
            val prod = mag[i] * factor + carry
            res[i + 1] = prod % 10
            carry = prod / 10
        }
        res[0] = carry
        var start = 0
        while (start < res.size - 1 && res[start] == 0) start++
        return res.copyOfRange(start, res.size)
    }

    override fun toString(): String {
        if (isZero) return "0"
        val sb = StringBuilder()
        if (sign < 0) sb.append('-')
        for (d in digits) sb.append(d)
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BigInt) return false
        return this.compareTo(other) == 0
    }

    override fun hashCode(): Int = toString().hashCode()
}

/**
 * Arbitrary-precision decimal arithmetic.
 * Main precision: 40 significant digits, half-up rounding.
 */
class Decimal(
    val unscaled: BigInt,
    val scale: Int // value = unscaled * 10^(-scale)
) : Comparable<Decimal> {

    companion object {
        const val DEFAULT_PRECISION = 40
        val ZERO = Decimal(BigInt.ZERO, 0)
        val ONE = Decimal(BigInt.ONE, 0)
        val TWO = Decimal(BigInt.parse("2"), 0)
        val TEN = Decimal(BigInt.TEN, 0)

        fun parse(str: String): Decimal {
            val s = str.trim()
            require(s.isNotEmpty()) { "Decimal string cannot be empty" }
            val sign = if (s.startsWith("-")) -1 else 1
            val clean = if (s.startsWith("-") || s.startsWith("+")) s.substring(1) else s
            val dotIdx = clean.indexOf('.')
            return if (dotIdx < 0) {
                val big = BigInt.parse(clean)
                Decimal(if (sign < 0) big.negate() else big, 0)
            } else {
                val intPart = clean.substring(0, dotIdx).ifEmpty { "0" }
                val fracPart = clean.substring(dotIdx + 1)
                val fullStr = intPart + fracPart
                val big = BigInt.parse(fullStr)
                Decimal(if (sign < 0) big.negate() else big, fracPart.length)
            }
        }

        fun fromDouble(d: Double): Decimal = parse(d.toString())
        fun fromLong(l: Long): Decimal = Decimal(BigInt.fromLong(l), 0)
        fun fromInt(i: Int): Decimal = Decimal.fromLong(i.toLong())

        fun min(a: Decimal, b: Decimal): Decimal = if (a <= b) a else b
        fun max(a: Decimal, b: Decimal): Decimal = if (a >= b) a else b
    }

    val isZero: Boolean get() = unscaled.isZero
    val isPositive: Boolean get() = unscaled.sign > 0
    val isNegative: Boolean get() = unscaled.sign < 0

    operator fun unaryMinus(): Decimal = Decimal(unscaled.negate(), scale)

    operator fun plus(other: Decimal): Decimal {
        if (this.isZero) return other
        if (other.isZero) return this
        val targetScale = maxOf(this.scale, other.scale)
        val aUnscaled = this.alignScale(targetScale)
        val bUnscaled = other.alignScale(targetScale)
        return Decimal(aUnscaled + bUnscaled, targetScale).roundToPrecision(DEFAULT_PRECISION)
    }

    operator fun minus(other: Decimal): Decimal = this + (-other)

    operator fun times(other: Decimal): Decimal {
        if (this.isZero || other.isZero) return ZERO
        val newScale = this.scale + other.scale
        val newUnscaled = this.unscaled * other.unscaled
        return Decimal(newUnscaled, newScale).roundToPrecision(DEFAULT_PRECISION)
    }

    operator fun div(other: Decimal): Decimal = divide(other, DEFAULT_PRECISION)

    fun divide(other: Decimal, precision: Int = DEFAULT_PRECISION): Decimal {
        require(!other.isZero) { "Division by zero" }
        if (this.isZero) return ZERO

        // Target: produce quotient with at least precision + 5 digits
        val extraDigits = precision + 10
        val numeratorScale = this.scale
        val denominatorScale = other.scale

        // Shift numerator so that quotient has desired extra digits
        val shift = extraDigits + denominatorScale - numeratorScale
        val scaledNumerator = if (shift >= 0) {
            this.unscaled.multiplyByPowerOfTen(shift)
        } else {
            this.unscaled
        }
        val actualDenom = if (shift < 0) {
            other.unscaled.multiplyByPowerOfTen(-shift)
        } else {
            other.unscaled
        }

        val divRes = scaledNumerator.divMod(actualDenom)
        var q = divRes.quotient
        val r = divRes.remainder

        // Half-up rounding on remainder
        if (!r.isZero) {
            val doubledRem = (r * BigInt.parse("2")).compareTo(actualDenom)
            if (doubledRem >= 0) {
                q = if (q.sign >= 0) q + BigInt.ONE else q - BigInt.ONE
            }
        }

        val resultScale = if (shift >= 0) extraDigits else extraDigits + (-shift)
        return Decimal(q, resultScale).roundToPrecision(precision)
    }

    private fun alignScale(targetScale: Int): BigInt {
        val diff = targetScale - this.scale
        return if (diff > 0) this.unscaled.multiplyByPowerOfTen(diff) else this.unscaled
    }

    /**
     * Rounds unscaled digits to at most [precision] significant digits using ROUND_HALF_UP.
     */
    fun roundToPrecision(precision: Int): Decimal {
        if (isZero) return ZERO
        val numDigits = unscaled.digits.size
        if (numDigits <= precision) return this

        val excess = numDigits - precision
        var divPower = BigInt.ONE
        for (i in 0 until excess) {
            divPower = divPower * BigInt.TEN
        }
        val divRes = unscaled.divMod(divPower)
        var q = divRes.quotient
        val r = divRes.remainder
        // Half-up: if remainder * 2 >= divPower, round away from zero
        val rAbs = if (r.sign < 0) r.negate() else r
        if ((rAbs * BigInt.parse("2")) >= divPower) {
            q = if (unscaled.sign > 0) q + BigInt.ONE else q - BigInt.ONE
        }
        val newScale = maxOf(0, scale - excess)
        return Decimal(q, newScale)
    }

    fun toFixed(decimals: Int = 2): String {
        if (isZero) {
            return if (decimals == 0) "0" else "0." + "0".repeat(decimals)
        }
        val diff = decimals - this.scale
        val roundedBig = if (diff >= 0) {
            this.unscaled.multiplyByPowerOfTen(diff)
        } else {
            // Need to round to `decimals` decimal places
            val excess = -diff
            var divPower = BigInt.ONE
            for (i in 0 until excess) divPower = divPower * BigInt.TEN
            val divRes = this.unscaled.divMod(divPower)
            var q = divRes.quotient
            val r = divRes.remainder
            val rAbs = if (r.sign < 0) r.negate() else r
            if ((rAbs * BigInt.parse("2")) >= divPower) {
                q = if (this.unscaled.sign > 0) q + BigInt.ONE else q - BigInt.ONE
            }
            q
        }
        val signStr = if (roundedBig.sign < 0) "-" else ""
        val digitsStr = roundedBig.digits.joinToString("")
        return if (decimals == 0) {
            "$signStr$digitsStr"
        } else {
            val padded = digitsStr.padStart(decimals + 1, '0')
            val splitIdx = padded.length - decimals
            val intPart = padded.substring(0, splitIdx)
            val fracPart = padded.substring(splitIdx)
            "$signStr$intPart.$fracPart"
        }
    }

    override fun compareTo(other: Decimal): Int {
        val targetScale = maxOf(this.scale, other.scale)
        val aUnscaled = this.alignScale(targetScale)
        val bUnscaled = other.alignScale(targetScale)
        return aUnscaled.compareTo(bUnscaled)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Decimal) return false
        return this.compareTo(other) == 0
    }

    override fun hashCode(): Int = toFixed(8).hashCode()

    override fun toString(): String {
        if (scale == 0) return unscaled.toString()
        val digits = unscaled.digits.joinToString("")
        val signStr = if (unscaled.sign < 0) "-" else ""
        val padded = digits.padStart(scale + 1, '0')
        val splitIdx = padded.length - scale
        val intPart = padded.substring(0, splitIdx)
        val fracPart = padded.substring(splitIdx).trimEnd('0')
        return if (fracPart.isEmpty()) "$signStr$intPart" else "$signStr$intPart.$fracPart"
    }

    fun toDouble(): Double = toString().toDouble()

    /**
     * Integer power using binary exponentiation.
     */
    fun pow(n: Int): Decimal {
        if (n == 0) return ONE
        if (n < 0) return ONE / this.pow(-n)
        var base = this
        var exp = n
        var result = ONE
        while (exp > 0) {
            if (exp % 2 == 1) result = result * base
            base = base * base
            exp /= 2
        }
        return result
    }

    /**
     * Arbitrary power x^y = exp(y * ln(x)).
     * Accurate to 40 digits for rates calculation.
     */
    fun pow(y: Decimal): Decimal {
        if (y.isZero) return ONE
        if (this.isZero) return ZERO
        if (this == ONE) return ONE
        require(this.isPositive) { "Base must be positive for fractional power: $this" }

        // If y is an integer:
        if (y.scale == 0 || y.toFixed(6).endsWith(".000000")) {
            val intExp = y.toFixed(0).toIntOrNull()
            if (intExp != null) return this.pow(intExp)
        }

        // x^y = exp(y * ln(x))
        val lnX = this.ln(DEFAULT_PRECISION + 10)
        val exponent = y * lnX
        return exponent.exp(DEFAULT_PRECISION + 10).roundToPrecision(DEFAULT_PRECISION)
    }

    /**
     * Natural log ln(x) using series expansion:
     * ln(x) = ln((1+u)/(1-u)) = 2 * (u + u^3/3 + u^5/5 + ...) where u = (x-1)/(x+1).
     */
    fun ln(precision: Int = 45): Decimal {
        require(this.isPositive) { "ln requires positive number: $this" }
        if (this == ONE) return ZERO

        // Reduce range if x is large or small using ln(x * 2^k) = ln(x) + k * ln(2)
        var x = this
        var k = 0
        val two = Decimal.parse("2")
        val half = Decimal.parse("0.5")
        while (x > two) {
            x = x.divide(two, precision + 5)
            k++
        }
        while (x < half) {
            x = x * two
            k--
        }

        val u = (x - ONE).divide(x + ONE, precision + 5)
        val u2 = u * u
        var term = u
        var sum = u
        var n = 3
        while (n < 200) {
            term = (term * u2).roundToPrecision(precision + 5)
            if (term.isZero) break
            val termDiv = term.divide(Decimal.fromInt(n), precision + 5)
            sum = sum + termDiv
            n += 2
        }
        var result = (sum * TWO).roundToPrecision(precision)

        if (k != 0) {
            val ln2 = Decimal.parse("0.693147180559945309417232121458176568075500134360255")
            result = result + (Decimal.fromInt(k) * ln2)
        }
        return result.roundToPrecision(precision)
    }

    /**
     * Exponential exp(z) using Taylor series:
     * exp(z) = 1 + z + z^2/2! + z^3/3! + ...
     */
    fun exp(precision: Int = 45): Decimal {
        if (this.isZero) return ONE

        // Range reduction: exp(z) = exp(z/2^m)^(2^m)
        var z = this
        var m = 0
        val maxVal = Decimal.parse("0.25")
        while (z > maxVal || z < -maxVal) {
            z = z.divide(TWO, precision + 5)
            m++
        }

        var sum = ONE + z
        var term = z
        var n = 2
        while (n < 200) {
            term = (term * z).divide(Decimal.fromInt(n), precision + 5)
            if (term.isZero) break
            sum = sum + term
            n++
        }

        var result = sum
        for (i in 0 until m) {
            result = (result * result).roundToPrecision(precision + 5)
        }
        return result.roundToPrecision(precision)
    }
}
