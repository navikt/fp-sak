package no.nav.foreldrepenger.web.app.tjenester.aktoer;

import java.util.List;

import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.FagsakDto;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.PersonDto;

public class AktoerInfoDto {

    private String aktoerId;
    private PersonDto person;
    private List<FagsakDto> fagsaker;

    public String getAktoerId() {
        return aktoerId;
    }

    public void setAktoerId(String aktoerId) {
        this.aktoerId = aktoerId;
    }

    public void setFagsaker(List<FagsakDto> fagsaker) {
        this.fagsaker = fagsaker;
    }

    public PersonDto getPerson() {
        return person;
    }

    public List<FagsakDto> getFagsaker() {
        return fagsaker;
    }

    public void setPerson(PersonDto person) {
        this.person = person;
    }


}
