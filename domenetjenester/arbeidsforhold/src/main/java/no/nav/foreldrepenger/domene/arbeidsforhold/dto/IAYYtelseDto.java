package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.util.Collections;
import java.util.List;

public class IAYYtelseDto {

    private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker = Collections.emptyList();
    private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();
    private List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

    void setRelatertTilgrensendeYtelserForSoker(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker) {
        this.relatertTilgrensendeYtelserForSoker = relatertTilgrensendeYtelserForSoker;
    }

    void setRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder) {
        this.relatertTilgrensendeYtelserForAnnenForelder = relatertTilgrensendeYtelserForAnnenForelder;
    }

    void setInnvilgetRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder) {
        this.innvilgetRelatertTilgrensendeYtelserForAnnenForelder = innvilgetRelatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForSoker() {
        return relatertTilgrensendeYtelserForSoker;
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForAnnenForelder() {
        return relatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<RelaterteYtelserDto> getInnvilgetRelatertTilgrensendeYtelserForAnnenForelder() {
        return innvilgetRelatertTilgrensendeYtelserForAnnenForelder;
    }
}
