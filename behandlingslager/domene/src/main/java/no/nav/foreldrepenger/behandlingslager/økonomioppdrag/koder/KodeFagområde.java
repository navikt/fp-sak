package no.nav.foreldrepenger.behandlingslager.økonomioppdrag.koder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

public enum KodeFagområde implements Kodeverdi {
    ENGANGSSTØNAD("REFUTG"),
    FORELDREPENGER_BRUKER("FP"),
    FORELDREPENGER_AG("FPREF"),
    SVANGERSKAPSPENGER_BRUKER("SVP"),
    SVANGERSKAPSPENGER_AG("SVPREF")
    ;

    private static final String KODEVERK = "KODE_FAGOMRÅDE_TYPE";
    private static final Map<String, KodeFagområde> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private final String kode;

    KodeFagområde(String kode) {
        this.kode = kode;
    }

    public static KodeFagområde fraKode(String kode) {
        Objects.requireNonNull(kode, "kodeFagområde");
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent KodeFagområde: " + kode);
        }
        return ad;
    }

    public boolean gjelderEngangsstønad() {
        return ENGANGSSTØNAD.equals(this);
    }

    public boolean gjelderForeldrepenger() {
        return FORELDREPENGER_BRUKER.equals(this) || FORELDREPENGER_AG.equals(this);
    }

    public boolean gjelderRefusjonTilArbeidsgiver() {
        return FORELDREPENGER_AG.equals(this) || SVANGERSKAPSPENGER_AG.equals(this);
    }

    @Override
    public String getNavn() {
        return null;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<KodeFagområde, String> {
        @Override
        public String convertToDatabaseColumn(KodeFagområde attribute) {
            return attribute.getKode();
        }

        @Override
        public KodeFagområde convertToEntityAttribute(String dbData) {
            return fraKode(dbData);
        }
    }
}
