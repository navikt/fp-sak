package no.nav.foreldrepenger.domene.iay.modell.kodeverk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum OffentligYtelseType implements Kodeverdi, YtelseType {

    UDEFINERT("-", "UNDEFINED", null),
    AAP("AAP", "Arbeidsavklaringspenger", "arbeidsavklaringspenger"),
    DAGPENGER_FISKER("DAGPENGER_FISKER", "Dagpenger til fisker som bare har hyre", "dagpengerTilFiskerSomBareHarHyre"),
    DAGPENGER_ARBEIDSLØS("DAGPENGER_ARBEIDSLØS", "Dagpenger ved arbeidsløshet", "dagpengerVedArbeidsloeshet"),
    FORELDREPENGER("FORELDREPENGER", "Foreldrepenger", "foreldrepenger"),
    OVERGANGSSTØNAD_ENSLIG("OVERGANGSSTØNAD_ENSLIG", "Overgangsstønad til enslig mor eller far",
            "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere"),
    SVANGERSKAPSPENGER("SVANGERSKAPSPENGER", "Svangerskapspenger", "svangerskapspenger"),
    SYKEPENGER("SYKEPENGER", "Sykepenger", "sykepenger"),
    SYKEPENGER_FISKER("SYKEPENGER_FISKER", "Sykepenger fisker", "sykepengerTilFiskerSomBareHarHyre"),
    UFØRETRYGD("UFØRETRYGD", "Uføretrygd", "ufoeretrygd"),
    UFØRETRYGD_ETTEROPPGJØR("UFØRETRYGD_ETTEROPPGJØR", "Uføretrygd etteroppgjør", "ufoereytelseEtteroppgjoer"),
    UNDERHOLDNINGSBIDRAG_BARN("UNDERHOLDNINGSBIDRAG_BARN", "Underholdningsbidrag til barn", "underholdsbidragTilBarn"),
    VENTELØNN("VENTELØNN", "Ventelønn", "venteloenn"),
    ;

    private static final Map<String, OffentligYtelseType> KODER = new LinkedHashMap<>();

    public static final String KODEVERK = "YTELSE_FRA_OFFENTLIGE";

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    @JsonIgnore
    private String navn;

    private String kode;
    @JsonIgnore
    private String offisiellKode;

    private OffentligYtelseType(String kode) {
        this.kode = kode;
    }

    private OffentligYtelseType(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    @JsonCreator
    public static OffentligYtelseType fraKode(@JsonProperty("kode") String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent OffentligYtelseType: " + kode);
        }
        return ad;
    }

    public static Map<String, OffentligYtelseType> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @JsonProperty
    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @JsonProperty
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getOffisiellKode() {
        return offisiellKode;
    }

}
