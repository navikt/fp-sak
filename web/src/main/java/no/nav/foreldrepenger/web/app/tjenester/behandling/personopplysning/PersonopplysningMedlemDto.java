package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;

import java.util.ArrayList;
import java.util.List;

public class PersonopplysningMedlemDto extends PersonIdentDto {

    private PersonstatusType personstatus;
    private Region region;
    private List<PersonadresseDto> adresser = new ArrayList<>();

    public PersonstatusType getPersonstatus() {
        return personstatus;
    }

    public void setPersonstatus(PersonstatusType personstatus) {
        this.personstatus = personstatus;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public List<PersonadresseDto> getAdresser() {
        return adresser;
    }

    public void setAdresser(List<PersonadresseDto> adresser) {
        this.adresser = adresser;
    }

}
