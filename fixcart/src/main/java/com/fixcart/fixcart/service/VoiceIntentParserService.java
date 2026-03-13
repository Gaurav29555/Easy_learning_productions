package com.fixcart.fixcart.service;

import com.fixcart.fixcart.entity.enums.WorkerType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class VoiceIntentParserService {

    public ParsedVoiceIntent parse(String transcript, String languageCode, String serviceAddress) {
        String normalized = normalize(transcript);
        String language = detectLanguage(normalized, languageCode);
        WorkerType workerType = detectWorkerType(normalized);
        VoiceAction action = detectAction(normalized);
        LocalDateTime requestedSchedule = detectSchedule(normalized);
        String addressHint = extractAddressHint(normalized, serviceAddress);
        return new ParsedVoiceIntent(language, action, workerType, requestedSchedule, addressHint, normalized);
    }

    private String detectLanguage(String normalized, String languageCode) {
        if (languageCode != null && !languageCode.isBlank()) {
            String lowered = languageCode.toLowerCase(Locale.ROOT);
            if (lowered.startsWith("hi")) return "hi";
            if (lowered.startsWith("es")) return "es";
            return "en";
        }
        if (containsAny(normalized, "mera", "mujhe", "batao", "kal", "aaj", "paas", "book karo", "reschedule karo", "bhugtan", "madad")) return "hi";
        if (containsAny(normalized, "reservar", "cancela", "estado", "trabajador", "cerca", "manana", "hoy", "pagar", "ayuda")) return "es";
        return "en";
    }

    private VoiceAction detectAction(String normalized) {
        if (containsAny(normalized, "pay", "payment", "checkout", "pago", "pagar", "bhugtan", "pay now")) return VoiceAction.PAYMENT;
        if (containsAny(normalized, "admin", "support", "escalate", "complaint", "manager", "madad", "ayuda")) return VoiceAction.ESCALATE;
        if (containsAny(normalized, "notify eta", "alert eta", "eta batao aur notify", "notificar eta", "arrival alert", "notify when arriving")) return VoiceAction.ETA_NOTIFY;
        if (containsAny(normalized, "cancel", "cancel karo", "cancela", "radd", "cancelar")) return VoiceAction.CANCEL;
        if (containsAny(normalized, "reschedule", "reschedule karo", "reprograma", "schedule change", "kal kar do")) return VoiceAction.RESCHEDULE;
        if (containsAny(normalized, "track", "where is my worker", "worker kaha hai", "trabajador donde", "tracking", "eta", "how long", "kitna time", "cuanto tarda")) return VoiceAction.TRACK;
        if (containsAny(normalized, "status", "booking status", "mera booking", "estado")) return VoiceAction.STATUS;
        if (containsAny(normalized, "nearest", "nearby", "find", "paas", "cerca")) return VoiceAction.FIND_NEARBY;
        if (containsAny(normalized, "book", "assign", "worker for me", "chahiye", "book karo", "bhejo", "reservar", "necesito", "book it", "reserve it", "isko book karo")) return VoiceAction.BOOK;
        return VoiceAction.UNKNOWN;
    }

    private WorkerType detectWorkerType(String normalized) {
        if (containsAny(normalized, "plumber", "pipe", "leak", "plomero", "fontanero", "nal", "paani")) return WorkerType.PLUMBER;
        if (containsAny(normalized, "carpenter", "wood", "furniture", "carpintero", "lakdi")) return WorkerType.CARPENTER;
        if (containsAny(normalized, "electrician", "electric", "light", "switch", "electricista", "bijli")) return WorkerType.ELECTRICIAN;
        if (containsAny(normalized, "cleaner", "cleaning", "safai", "limpieza", "limpiador")) return WorkerType.CLEANER;
        if (containsAny(normalized, "ac", "air conditioner", "aire acondicionado")) return WorkerType.AC_REPAIR;
        if (containsAny(normalized, "appliance", "fridge", "washing machine", "machine", "electrodomestico", "lavadora")) return WorkerType.APPLIANCE_REPAIR;
        if (containsAny(normalized, "paint", "painter", "deewar", "pintor", "pintura")) return WorkerType.PAINTER;
        return null;
    }

    private LocalDateTime detectSchedule(String normalized) {
        LocalDateTime base = LocalDateTime.now();
        boolean tomorrow = containsAny(normalized, "tomorrow", "kal", "manana");
        boolean today = containsAny(normalized, "today", "aaj", "hoy");

        if (!tomorrow
                && !today
                && !containsAny(normalized, "morning", "evening", "afternoon", "night", "subah", "shaam", "tarde", "noche", "in 1 hour", "ek ghante", "una hora", "in 2 hours", "do ghante", "dos horas")) {
            return null;
        }

        LocalDateTime target = tomorrow ? base.plusDays(1) : base;
        if (containsAny(normalized, "in 1 hour", "ek ghante", "una hora")) return base.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        if (containsAny(normalized, "in 2 hours", "do ghante", "dos horas")) return base.plusHours(2).withMinute(0).withSecond(0).withNano(0);
        if (containsAny(normalized, "morning", "subah", "manana")) return target.withHour(9).withMinute(0).withSecond(0).withNano(0);
        if (containsAny(normalized, "afternoon", "dopahar", "tarde")) return target.withHour(14).withMinute(0).withSecond(0).withNano(0);
        if (containsAny(normalized, "evening", "shaam", "noche")) return target.withHour(18).withMinute(0).withSecond(0).withNano(0);
        if (containsAny(normalized, "night", "raat")) return target.withHour(20).withMinute(0).withSecond(0).withNano(0);
        return target.plusHours(2).withMinute(0).withSecond(0).withNano(0);
    }

    private String extractAddressHint(String normalized, String explicitServiceAddress) {
        if (explicitServiceAddress != null && !explicitServiceAddress.isBlank()) {
            return explicitServiceAddress.trim();
        }
        for (String delimiter : List.of(" at ", " in ", " near ", " en ", " para ", " par ", " mein ")) {
            int index = normalized.indexOf(delimiter);
            if (index >= 0 && index + delimiter.length() < normalized.length()) {
                return normalized.substring(index + delimiter.length()).trim();
            }
        }
        return null;
    }

    private boolean containsAny(String normalized, String... tokens) {
        for (String token : tokens) {
            if (normalized.contains(token)) return true;
        }
        return false;
    }

    private String normalize(String transcript) {
        return transcript == null ? "" : transcript.trim().toLowerCase(Locale.ROOT).replaceAll("[\\p{Punct}]+", " ");
    }

    public boolean isFollowUpReference(String normalizedTranscript) {
        return containsAny(normalizedTranscript, "it", "that", "same", "same one", "same service", "isko", "usi", "ese", "eso", "mismo");
    }

    public enum VoiceAction {
        BOOK,
        FIND_NEARBY,
        STATUS,
        CANCEL,
        RESCHEDULE,
        TRACK,
        PAYMENT,
        ESCALATE,
        ETA_NOTIFY,
        UNKNOWN
    }

    public record ParsedVoiceIntent(
            String language,
            VoiceAction action,
            WorkerType workerType,
            LocalDateTime requestedSchedule,
            String addressHint,
            String normalizedTranscript
    ) {
    }
}
