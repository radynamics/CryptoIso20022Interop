package com.radynamics.CryptoIso20022Interop;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateTimeConvert {
    public static LocalDateTime toLocal(ZonedDateTime dt) {
        return dt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }
}
