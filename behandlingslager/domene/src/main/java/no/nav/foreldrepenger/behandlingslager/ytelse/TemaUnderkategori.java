package no.nav.foreldrepenger.behandlingslager.ytelse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum TemaUnderkategori implements Kodeverdi {

    FORELDREPENGER("FP", "Foreldrepenger", "FP"),
    FORELDREPENGER_FODSEL("FØ", "Foreldrepenger fødsel", "FØ"),
    FORELDREPENGER_ADOPSJON("AP", "Foreldrepenger adopsjon", "AP"),
    FORELDREPENGER_SVANGERSKAPSPENGER("SV", "Svangerskapspenger", "SV"),
    SYKEPENGER_SYKEPENGER("SP", "Sykepenger", "SP"),
    PÅRØRENDE_OMSORGSPENGER("OM", "Pårørende omsorgsmpenger", "OM"),
    PÅRØRENDE_OPPLÆRINGSPENGER("OP", "Pårørende opplæringspenger", "OP"),
    PÅRØRENDE_PLEIETRENGENDE_SYKT_BARN("PB", "Pårørende pleietrengende sykt barn", "PB"),
    PÅRØRENDE_PLEIETRENGENDE("PI", "Pårørende pleietrengende", "PI"),
    PÅRØRENDE_PLEIETRENGENDE_PÅRØRENDE("PP", "Pårørende pleietrengende pårørende", "PP"),
    PÅRØRENDE_PLEIEPENGER("PN", "Pårørende pleiepenger", "PN"),
    SYKEPENGER_FORSIKRINGSRISIKO("SU", "Sykepenger utenlandsopphold", "SU"),
    SYKEPENGER_REISETILSKUDD("RT", "Reisetilskudd", "RT"),
    SYKEPENGER_UTENLANDSOPPHOLD("RS", "Forsikr.risiko sykefravær", "RS"),
    OVERGANGSSTØNAD("OG", "Overgangsstønad", "OG"),
    FORELDREPENGER_FODSEL_UTLAND("FU", "Foreldrepenger fødsel, utland", "FU"),
    ENGANGSSTONAD_ADOPSJON("AE", "Adopsjon engangsstønad", "AE"),
    ENGANGSSTONAD_FODSEL("FE", "Fødsel engangsstønad", "FE"),

    BT("BT", "Stønad til barnetilsyn", "BT"),
    FL("FL", "Tilskudd til flytting", "FL"),
    UT("UT", "Skolepenger", "UT"),

    UDEFINERT("-", "Udefinert", null),
    ;

    private static final Map<String, TemaUnderkategori> KODER = new LinkedHashMap<>();

    private static final String KODEVERK = "TEMA_UNDERKATEGORI";

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

    private final String offisiellKode;

    TemaUnderkategori(String kode, String navn, String offisiellKode) {
        this.kode = kode;
        this.navn = navn;
        this.offisiellKode = offisiellKode;
    }

    public static TemaUnderkategori fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent TemaUnderkategori: " + kode);
        }
        return ad;
    }

    public static Map<String, TemaUnderkategori> kodeMap() {
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

}
