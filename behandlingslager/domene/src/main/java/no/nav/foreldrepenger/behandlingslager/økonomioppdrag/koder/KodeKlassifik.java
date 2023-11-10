package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

public enum KodeKlassifik {

    //Engangsstønad fødsel
    ES_FØDSEL("FPENFOD-OP"),
    //Engangsstønad adopsjon
    ES_ADOPSJON("FPENAD-OP"),

    //Feriepenger både FP adopsjon, fødsel og SVP
    FERIEPENGER_BRUKER("FPATFER"), // Bruker - Feriepenger.

    //Fødsel
    FPF_ARBEIDSTAKER("FPATORD"), // FP (foreldrepenger), AT - arbeidstaker, ORD - ordinær
    FPF_FRILANSER("FPATFRI"),
    FPF_SELVSTENDIG("FPSND-OP"),
    FPF_DAGPENGER("FPATAL"),
    FPF_SJØMANN("FPATSJO"),
    FPF_DAGMAMMA("FPSNDDM-OP"),
    FPF_JORDBRUKER("FPSNDJB-OP"),
    FPF_FISKER("FPSNDFI"),
    FPF_REFUSJON_AG("FPREFAG-IOP"), //FP (foreldrepenger), REFAG - arbeidsgiver
    FPF_FERIEPENGER_AG("FPREFAGFER-IOP"), // Arbeidsgiver - Feriepenger

    //Adopsjon
    FPA_ARBEIDSTAKER("FPADATORD"), // FP (foreldrepenger), AD - adopsjon, AT - arbeidstaker, ORD - ordinær
    FPA_FRILANSER("FPADATFRI"),
    FPA_SELVSTENDIG("FPADSND-OP"),
    FPA_DAGPENGER("FPADATAL"),
    FPA_SJØMANN("FPADATSJO"),
    FPA_DAGMAMMA("FPADSNDDM-OP"),
    FPA_JORDBRUKER("FPADSNDJB-OP"),
    FPA_FISKER("FPADSNDFI"),
    FPA_REFUSJON_AG("FPADREFAG-IOP"), //FP (foreldrepenger), AD - adopsjon, REFAG - arbeidsgiver
    FPA_FERIEPENGER_AG("FPADREFAGFER-IOP"), // Arbeidsgiver - Feriepenger

    //Svangerskapsenger
    SVP_ARBEDISTAKER("FPSVATORD"), // FPSV (svangerskapsenger), AT - arbeidstaker, ORD - ordinær
    SVP_FRILANSER("FPSVATFRI"),
    SVP_SELVSTENDIG("FPSVSND-OP"),
    SVP_DAGPENGER("FPSVATAL"),
    SVP_SJØMANN("FPSVATSJO"),
    SVP_DAGMAMMA("FPSVSNDDM-OP"),
    SVP_JORDBRUKER("FPSVSNDJB-OP"),
    SVP_FISKER("FPSVSNDFI"),
    SVP_REFUSJON_AG("FPSVREFAG-IOP"), //FPSV (svangerskapsenger), REFAG - arbeidsgiver
    SVP_FERIEPENGER_AG("FPSVREFAGFER-IOP"); // Arbeidsgiver - Feriepenger

    private static final Map<String, KodeKlassifik> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    KodeKlassifik(String kodeKlassifik) {
        this.kode = kodeKlassifik;
    }

    public static KodeKlassifik fraKode(String kode) {
        Objects.requireNonNull(kode, "kodeKlassifik");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KodeKlassifik: " + kode);
        }
        return ad;
    }

    public String getKode() {
        return kode;
    }

    public boolean gjelderFeriepenger() {
        return this.equals(FERIEPENGER_BRUKER) || this.equals(FPF_FERIEPENGER_AG) || this.equals(FPA_FERIEPENGER_AG) || this.equals(
            SVP_FERIEPENGER_AG);
    }

    public boolean gjelderArbeidsgiver() {
        return this.equals(FPF_REFUSJON_AG) || this.equals(FPA_REFUSJON_AG) || this.equals(SVP_REFUSJON_AG);
    }

    public boolean gjelderEngangsstønad() {
        return this.equals(ES_FØDSEL) || this.equals(ES_ADOPSJON);
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KodeKlassifik, String> {
        @Override
        public String convertToDatabaseColumn(KodeKlassifik attribute) {
            return attribute.getKode();
        }

        @Override
        public KodeKlassifik convertToEntityAttribute(String dbData) {
            return fraKode(dbData);
        }
    }
}
