package com.example.incidentmanagement.repository;

import java.time.LocalDate;

/**
 * Interface-based projection for the two native daily-count queries below. Native queries
 * don't go through HQL's semantic constructor-matching (the thing that made a SELECT NEW
 * DailyCountResponse(date(...), COUNT(...)) constructor expression fail to resolve even though
 * date() is a real, correctly-typed HQL function) -- Spring Data instead binds each getter here
 * to the matching result column alias directly off the JDBC ResultSet, which is far more
 * forgiving about temporal type conversion.
 */
public interface DailyIncidentCountProjection {

    LocalDate getDay();

    Long getCount();
}
