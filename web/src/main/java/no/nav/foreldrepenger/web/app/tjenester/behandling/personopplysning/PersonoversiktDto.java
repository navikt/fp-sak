package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PersonoversiktDto {

    @NotNull private PersonopplysningBasisDto bruker;
    private PersonopplysningBasisDto annenPart;
    @NotNull private List<PersonopplysningBasisDto> barn = new ArrayList<>();

    public PersonopplysningBasisDto getBruker() {
        return bruker;
    }

    public void setBruker(PersonopplysningBasisDto bruker) {
        this.bruker = bruker;
    }

    public PersonopplysningBasisDto getAnnenPart() {
        return annenPart;
    }

    public void setAnnenPart(PersonopplysningBasisDto annenPart) {
        this.annenPart = annenPart;
    }

    public List<PersonopplysningBasisDto> getBarn() {
        return barn;
    }

    public void leggTilBarn(PersonopplysningBasisDto barn) {
        this.barn.add(barn);
    }

}
