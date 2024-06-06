package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad;

import java.time.LocalDate;

public record OppgittFordelingDto(LocalDate startDatoForPermisjon, DekningsgradInfoDto dekningsgrader) {

    public record DekningsgradInfoDto(Integer avklartDekningsgrad, OppgittDekningsgradDto søker, OppgittDekningsgradDto annenPart) {
    }

    public record OppgittDekningsgradDto(LocalDate søknadsdato, Integer dekningsgrad) {
    }
}
