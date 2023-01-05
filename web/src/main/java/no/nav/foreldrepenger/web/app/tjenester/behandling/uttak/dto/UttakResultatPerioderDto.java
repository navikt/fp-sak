package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto;

import java.util.List;

public record UttakResultatPerioderDto(List<UttakResultatPeriodeDto> perioderSøker,
                                       List<UttakResultatPeriodeDto> perioderAnnenpart,
                                       boolean annenForelderHarRett,
                                       boolean aleneomsorg,
                                       boolean annenForelderRettEØS,
                                       boolean oppgittAnnenForelderRettEØS,
                                       FilterDto årsakFilter) {

    public record FilterDto(boolean kreverSammenhengendeUttak, boolean utenMinsterett, boolean søkerErMor) {}
}
