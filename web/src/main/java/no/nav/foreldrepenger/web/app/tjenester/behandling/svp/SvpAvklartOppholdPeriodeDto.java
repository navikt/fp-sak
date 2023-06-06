package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;

import javax.validation.constraints.NotNull;

import java.time.LocalDate;

public record SvpAvklartOppholdPeriodeDto(@NotNull LocalDate fom, @NotNull LocalDate tom, @NotNull SvpOppholdÅrsak oppholdÅrsak) {
}
