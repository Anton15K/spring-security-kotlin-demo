package com.hadiyarajesh.spring_security_demo.security

import com.hadiyarajesh.spring_security_demo.app.exception.InvalidJwtException
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import java.util.*

@Component
class TokenProvider(
    private val securityProperties: SecurityProperties
) {
    private fun extractAllClaims(token: String): Claims {

        return Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(securityProperties.secret.toByteArray()))
            .build()
            .parseClaimsJws(token)
            .body
    }

    private fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T {
        val claims = extractAllClaims(token)
        return claimsResolver(claims)
    }

    fun extractEmail(token: String): String {
        return extractClaim(token) { claims -> claims.subject }
    }

    private fun generateTokenFromClaims(
        claims: Claims,
        tokenValidity: Long
    ): String {
        val now = Date()

        return Jwts.builder()
            .setClaims(claims)
            .setIssuer("SpringSecurityKotlinDemoApplication")
            .setIssuedAt(now)
            .setExpiration(Date(now.time + tokenValidity))
            .signWith(Keys.hmacShaKeyFor(securityProperties.secret.toByteArray()))
            .compact()
    }

    fun generateToken(
        email: String,
        roles: List<String>
    ): String {
        val claims = Jwts.claims()
            .setSubject(email).apply {
                this["role"] = roles
            }
        return generateTokenFromClaims(claims, securityProperties.expiration)
    }

    fun generateRefreshToken(claims: Claims): String {
        return generateTokenFromClaims(claims, securityProperties.expiration)
    }

    fun validateToken(token: String): Boolean {
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(securityProperties.secret.toByteArray()))
                .build()
                .parseClaimsJws(token)
            return true
        } catch (e: MalformedJwtException) {
            throw InvalidJwtException("JWT token is malformed.")
        } catch (e: ExpiredJwtException) {
            throw InvalidJwtException("JWT token is expired.")
        } catch (e: Exception) {
            throw InvalidJwtException("JWT token validation failed.")
        }
    }

    fun getTokenFromHeader(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")

        return if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7, bearerToken.length)
        } else {
            null
        }
    }

    fun getClaimsFromToken(token: String): Claims? {
        return try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(securityProperties.secret.toByteArray()))
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            null
        }
    }
}
