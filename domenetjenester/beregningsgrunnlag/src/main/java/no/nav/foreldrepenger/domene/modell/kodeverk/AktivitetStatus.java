package no.nav.foreldrepenger.domene.modell.kodeverk;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;


public enum AktivitetStatus implements Kodeverdi {
    ARBEIDSAVKLARINGSPENGER("AAP", "Arbeidsavklaringspenger", Inntektskategori.ARBEIDSAVKLARINGSPENGER),
    ARBEIDSTAKER("AT", "Arbeidstaker", Inntektskategori.ARBEIDSTAKER),
    DAGPENGER("DP", "Dagpenger", Inntektskategori.DAGPENGER),
    FRILANSER("FL", "Frilanser", Inntektskategori.FRILANSER),
    MILITÆR_ELLER_SIVIL("MS", "Militær eller sivil", Inntektskategori.ARBEIDSTAKER),
    SELVSTENDIG_NÆRINGSDRIVENDE("SN", "Selvstendig næringsdrivende", Inntektskategori.SELVSTENDIG_NÆRINGSDRIVENDE),
    KOMBINERT_AT_FL("AT_FL", "Kombinert arbeidstaker og frilanser", Inntektskategori.UDEFINERT),
    KOMBINERT_AT_SN("AT_SN", "Kombinert arbeidstaker og selvstendig næringsdrivende", Inntektskategori.UDEFINERT),
    KOMBINERT_FL_SN("FL_SN", "Kombinert frilanser og selvstendig næringsdrivende", Inntektskategori.UDEFINERT),
    KOMBINERT_AT_FL_SN("AT_FL_SN", "Kombinert arbeidstaker, frilanser og selvstendig næringsdrivende", Inntektskategori.UDEFINERT),
    BRUKERS_ANDEL("BA", "Brukers andel", Inntektskategori.UDEFINERT),
    KUN_YTELSE("KUN_YTELSE", "Kun ytelse", Inntektskategori.UDEFINERT),

    TTLSTØTENDE_YTELSE("TY", "Tilstøtende ytelse", Inntektskategori.UDEFINERT),
    VENTELØNN_VARTPENGER("VENTELØNN_VARTPENGER", "Ventelønn/Vartpenger", Inntektskategori.UDEFINERT),

    UDEFINERT("-", "Ikke definert", Inntektskategori.UDEFINERT);

    public static final String KODEVERK = "AKTIVITET_STATUS";

    private static final Map<String, AktivitetStatus> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }


    private String navn;
    @JsonValue
    private String kode;

    private Inntektskategori inntektskategori;

    AktivitetStatus(String kode, String navn, Inntektskategori inntektskategori) {
        this.kode = kode;
        this.navn = navn;
        this.inntektskategori = inntektskategori;
    }

    public static AktivitetStatus fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AktivitetStatus: " + kode);
        }
        return ad;
    }

    public static Map<String, AktivitetStatus> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    private static final Set<AktivitetStatus> AT_STATUSER = new HashSet<>(Arrays.asList(ARBEIDSTAKER,
        KOMBINERT_AT_FL_SN, KOMBINERT_AT_SN, KOMBINERT_AT_FL));

    private static final Set<AktivitetStatus> SN_STATUSER = new HashSet<>(Arrays.asList(SELVSTENDIG_NÆRINGSDRIVENDE,
        KOMBINERT_AT_FL_SN, KOMBINERT_AT_SN, KOMBINERT_FL_SN));

    private static final Set<AktivitetStatus> FL_STATUSER = new HashSet<>(Arrays.asList(FRILANSER,
        KOMBINERT_AT_FL_SN, KOMBINERT_AT_FL, KOMBINERT_FL_SN));

    public boolean erArbeidstaker() {
        return AT_STATUSER.contains(this);
    }

    public boolean erSelvstendigNæringsdrivende() {
        return SN_STATUSER.contains(this);
    }

    public boolean erFrilanser() {
        return FL_STATUSER.contains(this);
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

    public Inntektskategori getInntektskategori() {
        return inntektskategori;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AktivitetStatus, String> {

        @Override
        public String convertToDatabaseColumn(AktivitetStatus attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AktivitetStatus convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }
    }
}
