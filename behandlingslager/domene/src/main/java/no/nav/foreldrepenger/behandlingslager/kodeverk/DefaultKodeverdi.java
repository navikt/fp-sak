package no.nav.foreldrepenger.behandlingslager.kodeverk;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class DefaultKodeverdi implements BasisKodeverdi {

    @JsonProperty("kode")
    private String kode;

    @JsonProperty("navn")
    private String navn;

    @JsonProperty("kodeverk")
    private String kodeverk;

    @JsonProperty("gyldigFom")
    private LocalDate gyldigFom;

    @JsonProperty("gyldigTom")
    private LocalDate gyldigtom;

    public DefaultKodeverdi(String kodeverk, String kode, String navn) {
        this(kodeverk, kode, navn, null, null);
    }

    public DefaultKodeverdi(String kodeverk, String kode, String navn, LocalDate gyldigFom, LocalDate gyldigtom) {
        this.kodeverk = kodeverk;
        this.kode = kode;
        this.navn = navn;
        this.gyldigFom = gyldigFom;
        this.gyldigtom = gyldigtom;
    }

    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getKodeverk() {
        return kodeverk;
    }

    @Override
    public String getNavn() {
        return navn;
    }

    public LocalDate getGyldigFom() {
        return gyldigFom;
    }

    public LocalDate getGyldigTom() {
        return gyldigtom;
    }

}
