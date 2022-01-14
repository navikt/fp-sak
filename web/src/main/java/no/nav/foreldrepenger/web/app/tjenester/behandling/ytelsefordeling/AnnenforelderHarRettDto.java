package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.List;

import no.nav.foreldrepenger.familiehendelse.rest.PeriodeDto;

public record AnnenforelderHarRettDto(String begrunnelse,
                                      Boolean annenforelderHarRett,
                                      List<PeriodeDto> annenforelderHarRettPerioder,
                                      Boolean annenforelderMottarUføretrygd,
                                      boolean avklarAnnenforelderMottarUføretrygd) {
}
