package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.util.Objects;

public class ArbeidsgiverLagreDto {

    @Pattern(regexp = "[\\d]{9}")
    private String identifikator;

    @Valid
    private AktørId aktørId;

    ArbeidsgiverLagreDto() {
        //jackson
    }

    public ArbeidsgiverLagreDto(String identifikator, AktørId aktørId) {
        this.identifikator = identifikator;
        this.aktørId = aktørId;
    }

    public ArbeidsgiverLagreDto(String identifikator) {
        this(identifikator, null);
    }

    public ArbeidsgiverLagreDto(AktørId aktørId) {
        this(null, aktørId);
    }

    public String getIdentifikator() {
        return identifikator;
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        var that = (ArbeidsgiverLagreDto) o;
        return Objects.equals(identifikator, that.identifikator) &&
            Objects.equals(aktørId, that.aktørId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifikator, aktørId);
    }

    public boolean erVirksomhet() {
        return aktørId == null;
    }
}
