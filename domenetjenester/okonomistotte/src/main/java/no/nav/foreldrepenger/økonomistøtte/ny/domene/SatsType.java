package no.nav.foreldrepenger.økonomistøtte.ny.domene;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

@JsonFormat(shape = Shape.STRING)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public enum SatsType implements Kodeverdi {

    DAG("DAG"),
    ENGANG("ENG"),
    UDEFINERT("-"),
    ;

    @JsonValue
    private String kode;

    SatsType() {
        // Hibernate trenger den
    }

    SatsType(String kode) {
        this.kode = kode;
    }

    @JsonCreator
    public static SatsType fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        for (var value : values()) {
            if (value.getKode().equals(kode)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Ukjent SatsType: " + kode);
    }

    @Override
    public String getNavn() {
        return null;
    }

    @Override
    public String getKodeverk() {
        return "SATS_TYPE";
    }

    @Override
    public String getKode() {
        return kode;
    }

}
