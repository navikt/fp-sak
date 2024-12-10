package no.nav.foreldrepenger.behandlingslager.behandling.klage;

import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.ES;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.FP;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.SVP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KlageHjemmel implements Kodeverdi {

    MEDLEM("14-02", "§ 14-2 Medlemskap", "FTRL_14_2", Set.of(ES, FP, SVP)),
    SVANGERSKAP("14-04", "§ 14-4 Svangerskapspenger", "FTRL_14_4", Set.of(SVP)),
    FORELDRE("14-05", "§ 14-5 Rett på foreldrepenger", "FTRL_14_5", Set.of(FP)),
    OPPTJENING("14-06", "§ 14-6 Opptjening", "FTRL_14_6", Set.of(FP)),
    BEREGNING("14-07", "§ 14-7 Beregning", "FTRL_14_7", Set.of(FP)),
    DAGER("14-09", "§ 14-9 Stønadsperioden", "FTRL_14_9", Set.of(FP)),
    UTTAK("14-10", "§ 14-10 Uttaksperiodene", "FTRL_14_10", Set.of(FP)),
    UTSETTELSE("14-11", "§ 14-11 Utsettelse", "FTRL_14_11", Set.of(FP)),
    KVOTER("14-12", "§ 14-12 Uttak av kvoter", "FTRL_14_12", Set.of(FP)),
    AKTIVITET("14-13", "§ 14-13 Fars uttak", "FTRL_14_13", Set.of(FP)),
    BFHR("14-14", "§ 14-14 Bare far har rett", "FTRL_14_14", Set.of(FP)),
    FARALENE("14-15", "§ 14-15 Far aleneomsorg", "FTRL_14_15", Set.of(FP)),
    GRADERING("14-16", "§ 14-16 Gradert uttak", "FTRL_14_16", Set.of(FP)),
    ENGANGS("14-17", "§ 14-17 Rett på engangsstønad", "FTRL_14_17", Set.of(ES)),
    OPPTJENINGSTID("8-2", "§ 8-2 Opptjeningstid", "FTRL_8_2", Set.of(FP, SVP)),
    OPPLYSNINGSPLIKT("21-3", "§ 21-3 Opplysningsplikt", "FTRL_21_3", Set.of(ES, FP, SVP)),
    FREMSETT("22-13", "§ 22-13 Fremsetning av krav", "FTRL_22_13", Set.of(ES, FP, SVP)),
    TILBAKE("22-15", "§ 22-15 Tilbakekreving", "FTRL_22_15", Set.of(ES, FP, SVP)),
    EØS_YTELSE("883-5", "EØS 883/2004 artikkel 5", "EOES_883_2004_5", Set.of(FP)),
    EØS_OPPTJEN("883-6", "EØS 883/2004 artikkel 6", "EOES_883_2004_6", Set.of(FP, SVP)),

    UDEFINERT("-", "Ikke definert", "", Set.of()),
    ;

    private static final Map<String, KlageHjemmel> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "KLAGE_HJEMMEL";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    private final String navn;

    @JsonValue
    private final String kode;

    private final String kabal;
    private final Set<FagsakYtelseType.YtelseType> ytelser;

    KlageHjemmel(String kode, String navn, String kabal, Set<FagsakYtelseType.YtelseType> ytelser) {
        this.kode = kode;
        this.navn = navn;
        this.kabal = kabal;
        this.ytelser = ytelser;
    }

    public static KlageHjemmel fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KlageHjemmel: " + kode);
        }
        return ad;
    }

    public static Map<String, KlageHjemmel> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Override
    public String getKode() {
        return kode;
    }

    public String getKabal() {
        return kabal;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KlageHjemmel, String> {
        @Override
        public String convertToDatabaseColumn(KlageHjemmel attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public KlageHjemmel convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }

    public static List<KlageHjemmel> getHjemlerForYtelse(FagsakYtelseType fagsakYtelseType) {
        if (fagsakYtelseType == null) {
            return List.of();
        } else if (FagsakYtelseType.UDEFINERT.equals(fagsakYtelseType)) {
            return new ArrayList<>(KODER.values());
        } else {
            var ytelse = FagsakYtelseType.YtelseType.valueOf(fagsakYtelseType.getKode());
            return KODER.values().stream().filter(h -> h.ytelser.contains(ytelse)).toList();
        }
    }

    public static KlageHjemmel standardHjemmelForYtelse(FagsakYtelseType ytelseType) {
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return KlageHjemmel.FORELDRE;
        } else if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return KlageHjemmel.ENGANGS;
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return KlageHjemmel.SVANGERSKAP;
        } else {
            return KlageHjemmel.UDEFINERT;
        }
    }

}
