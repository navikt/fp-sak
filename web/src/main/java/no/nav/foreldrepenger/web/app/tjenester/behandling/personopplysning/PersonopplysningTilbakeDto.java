package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import no.nav.foreldrepenger.domene.typer.AktørId;

public class PersonopplysningTilbakeDto {

    private String aktoerId;
    private Integer antallBarn;

    public PersonopplysningTilbakeDto(String aktoerId, Integer antallBarn) {
        this.aktoerId = aktoerId;
        this.antallBarn = antallBarn;
    }

    public AktørId getAktoerId() {
        return aktoerId == null ? null : new AktørId(aktoerId);
    }

    public Integer getAntallBarn() {
        return antallBarn;
    }
}
