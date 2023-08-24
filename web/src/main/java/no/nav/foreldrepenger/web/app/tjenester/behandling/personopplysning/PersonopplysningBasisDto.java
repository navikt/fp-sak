package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBrukerKjønn;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PersonopplysningBasisDto extends PersonIdentDto {

    private NavBrukerKjønn kjønn;
    private SivilstandType sivilstand;
    private LocalDate dødsdato;
    private LocalDate fødselsdato;
    private List<PersonadresseDto> adresser = new ArrayList<>();

    public PersonopplysningBasisDto(AktørId aktørId) {
        this.setAktoerId(aktørId);
    }

    public NavBrukerKjønn getKjønn() {
        return kjønn;
    }

    public void setKjønn(NavBrukerKjønn kjønn) {
        this.kjønn = kjønn;
    }

    public SivilstandType getSivilstand() {
        return sivilstand;
    }

    public void setSivilstand(SivilstandType sivilstand) {
        this.sivilstand = sivilstand;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public void setDødsdato(LocalDate dødsdato) {
        this.dødsdato = dødsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public void setFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }
}
