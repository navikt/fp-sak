package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;

import java.time.LocalDate;

public record SvpAvklartOppholdPeriodeDto(LocalDate fom, LocalDate tom, SvpOppholdÅrsak oppholdÅrsak) {
}
