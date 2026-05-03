package com.example.forumapp.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateTimeUtils() {}

    public static String format(LocalDateTime value) {
        return value == null ? "-" : FORMATTER.format(value);
    }
}
