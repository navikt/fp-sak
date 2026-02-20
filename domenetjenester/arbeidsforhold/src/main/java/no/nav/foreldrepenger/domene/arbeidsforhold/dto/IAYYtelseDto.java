package no.nav.foreldrepenger.domene.arbeidsforhold.dto;

import java.util.Collections;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class IAYYtelseDto {

    @NotNull private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSøker = Collections.emptyList();
    @NotNull private List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();
    @NotNull private List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder = Collections.emptyList();

    void setRelatertTilgrensendeYtelserForSøker(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForSøker) {
        this.relatertTilgrensendeYtelserForSøker = relatertTilgrensendeYtelserForSøker;
    }

    void setRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> relatertTilgrensendeYtelserForAnnenForelder) {
        this.relatertTilgrensendeYtelserForAnnenForelder = relatertTilgrensendeYtelserForAnnenForelder;
    }

    void setInnvilgetRelatertTilgrensendeYtelserForAnnenForelder(List<RelaterteYtelserDto> innvilgetRelatertTilgrensendeYtelserForAnnenForelder) {
        this.innvilgetRelatertTilgrensendeYtelserForAnnenForelder = innvilgetRelatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForSøker() {
        return relatertTilgrensendeYtelserForSøker;
    }

    public List<RelaterteYtelserDto> getRelatertTilgrensendeYtelserForAnnenForelder() {
        return relatertTilgrensendeYtelserForAnnenForelder;
    }

    public List<RelaterteYtelserDto> getInnvilgetRelatertTilgrensendeYtelserForAnnenForelder() {
        return innvilgetRelatertTilgrensendeYtelserForAnnenForelder;
    }
}
