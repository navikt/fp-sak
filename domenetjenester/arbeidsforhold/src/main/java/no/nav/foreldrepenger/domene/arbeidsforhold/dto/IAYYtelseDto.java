package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Collections;
import java.util.List;

public class IAYYtelseDto {

    @NotNull private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSoker = Collections.emptyList();
    @NotNull private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();
    @NotNull private List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

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
