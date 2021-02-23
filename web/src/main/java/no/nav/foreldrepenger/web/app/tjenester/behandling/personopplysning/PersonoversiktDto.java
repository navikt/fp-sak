package no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PersonoversiktDto {

    private PersonopplysningBasisDto bruker;
    private PersonopplysningBasisDto annenPart;
    private List<PersonopplysningBasisDto> barn = new ArrayList<>();

    public PersonoversiktDto() {
    }

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

    public List<PersonopplysningBasisDto> getPersoner() {
        var alle = new ArrayList<>(List.of(bruker));
        Optional.ofNullable(annenPart).ifPresent(alle::add);
        alle.addAll(barn);
        return alle;
    }
}
