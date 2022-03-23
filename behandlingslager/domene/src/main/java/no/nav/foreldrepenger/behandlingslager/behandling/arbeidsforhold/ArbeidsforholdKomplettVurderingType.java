package no.nav.foreldrepenger.behandlingslager.behandling.arbeidsforhold;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum ArbeidsforholdKomplettVurderingType implements Kodeverdi {

    KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING("KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_INNTEKTSMELDING", "Saksbehandler kontakter arbeidsgiver for å avklare manglende inntektsmelding"),
    FORTSETT_UTEN_INNTEKTSMELDING("FORTSETT_UTEN_INNTEKTSMELDING", "Behandlingen kan fortsette uten inntektsmelding for dette arbeidsforholdet"),

    KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_ARBEIDSFORHOLD("KONTAKT_ARBEIDSGIVER_VED_MANGLENDE_ARBEIDSFORHOLD", "Saksbehandler kontakter arbeidsgiver for å avklare manglende arbeidsforhold"),
    IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING("IKKE_OPPRETT_BASERT_PÅ_INNTEKTSMELDING", "Arbeidsforhold som tilhører inntetksmeldingen skal ikke opprettes manuelt."),
    OPPRETT_BASERT_PÅ_INNTEKTSMELDING("OPPRETT_BASERT_PÅ_INNTEKTSMELDING", "Arbeidsforholdet er opprettet av saksbehandler på bakgrunn av motatt inntektsmelding"),

    MANUELT_OPPRETTET_AV_SAKSBEHANDLER("MANUELT_OPPRETTET_AV_SAKSBEHANDLER", "Arbeidsforholdet er manuelt opprettet" +
        " av sksbehandler og skal brukes videre i behandlingen."),
    FJERN_FRA_BEHANDLINGEN("FJERN_FRA_BEHANDLINGEN", "Saksbehandler har slettet dette arbeidsforholdet fra behandlingen." +
        " Kan kun velges for arbeidsforhold som er manuelt opprettet, ikke registerdata."),

    UDEFINERT("-", "Ikke definert"),
    ;
    public static final String KODEVERK = "ARBEIDSFORHOLD_KOMPLETT_VURDERING_TYPE";

    private static final Map<String, ArbeidsforholdKomplettVurderingType> KODER = new LinkedHashMap<>();

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

    ArbeidsforholdKomplettVurderingType(String kode, String navn) {
        this.kode = kode;
        this.navn = navn;
    }

    public static Map<String, ArbeidsforholdKomplettVurderingType> kodeMap() {
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

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<ArbeidsforholdKomplettVurderingType, String> {

        @Override
        public String convertToDatabaseColumn(ArbeidsforholdKomplettVurderingType attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public ArbeidsforholdKomplettVurderingType convertToEntityAttribute(String dbData) {
            return dbData == null ? null : fraKode(dbData);
        }

        private static ArbeidsforholdKomplettVurderingType fraKode(String kode) {
            if (kode == null) {
                return null;
            }
            var ad = KODER.get(kode);
            if (ad == null) {
                throw new IllegalArgumentException("Ukjent ArbeidsforholdKomplettVurderingType: " + kode);
            }
            return ad;
        }
    }
}
