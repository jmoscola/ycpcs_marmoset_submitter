package edu.ycp.cs.marmosetsubmitter.services

/**
 * Data class representing the user's Marmoset login credentials.
 *
 * @property username The student's Marmoset campus UID.
 * @property password The student's Marmoset password.
 */
data class MarmosetCredentials(val username: String, val password: String)