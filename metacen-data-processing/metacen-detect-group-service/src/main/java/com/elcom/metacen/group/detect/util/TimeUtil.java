package com.elcom.metacen.group.detect.util;

import java.time.LocalDateTime;

public class TimeUtil {
    public static boolean isOverlapping(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }
}
