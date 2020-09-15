package no.nav.foreldrepenger.familiehendelse.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@JsonAutoDetect(getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, fieldVisibility= JsonAutoDetect.Visibility.ANY)
public class PeriodeDto implements AbacDto {

    @JsonProperty("periodeFom")
    @NotNull
    private LocalDate periodeFom;

    @JsonProperty("periodeTom")
    @NotNull
    private LocalDate periodeTom;

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }

    @Override
    public AbacDataAttributter abacAttributter() {
        return AbacDataAttributter.opprett();
    }
}
