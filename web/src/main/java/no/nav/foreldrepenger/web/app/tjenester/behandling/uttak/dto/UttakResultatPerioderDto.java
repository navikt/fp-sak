package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.time.LocalDate;
import java.util.List;

public record UttakResultatPerioderDto(List<UttakResultatPeriodeDto> perioderSøker,
                                       List<UttakResultatPeriodeDto> perioderAnnenpart,
                                       boolean annenForelderHarRett,
                                       boolean aleneomsorg,
                                       boolean annenForelderRettEØS,
                                       boolean oppgittAnnenForelderRettEØS,
                                       FilterDto årsakFilter,
                                       LocalDate endringsdato) {

    public record FilterDto(LocalDate kreverSammenhengendeUttakTom, boolean utenMinsterett, boolean søkerErMor) {}
}
