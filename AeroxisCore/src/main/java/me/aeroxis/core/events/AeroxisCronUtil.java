package me.aeroxis.core.events;

import java.time.LocalDateTime;

/**
 * ⏱️ AeroxisCronUtil - Evaluador ligero de expresiones Cron (Minuto, Hora, Día, Mes, DíaSemana)
 */
public class AeroxisCronUtil {

    public static boolean matches(String cronExpression, LocalDateTime time) {
        String[] parts = cronExpression.split(" ");
        if (parts.length != 5) return false; // Formato inválido

        return matchesPart(parts[0], time.getMinute()) &&
               matchesPart(parts[1], time.getHour()) &&
               matchesPart(parts[2], time.getDayOfMonth()) &&
               matchesPart(parts[3], time.getMonthValue()) &&
               matchesDayOfWeek(parts[4], time.getDayOfWeek().getValue());
    }

    private static boolean matchesPart(String part, int value) {
        if (part.equals("*")) return true;
        if (part.contains(",")) {
            for (String p : part.split(",")) {
                if (Integer.parseInt(p.trim()) == value) return true;
            }
            return false;
        }
        return Integer.parseInt(part) == value;
    }

    private static boolean matchesDayOfWeek(String part, int javaDayOfWeek) {
        // En Java 21: 1=Lunes, 7=Domingo. En Cron tradicional: 0 o 7 = Domingo.
        int cronDay = (javaDayOfWeek == 7) ? 0 : javaDayOfWeek;
        if (part.equals("*")) return true;
        
        if (part.contains(",")) {
            for (String p : part.split(",")) {
                int target = Integer.parseInt(p.trim());
                if (target == cronDay || (target == 7 && cronDay == 0)) return true;
            }
            return false;
        }
        int target = Integer.parseInt(part);
        return target == cronDay || (target == 7 && cronDay == 0);
    }
}