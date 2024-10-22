package no.nav.foreldrepenger.web.app.tjenester.behandling.svp;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpOppholdÅrsak;

public record SvpAvklartOppholdPeriodeDto(@NotNull LocalDate fom,
                                          @NotNull LocalDate tom,
                                          @NotNull SvpOppholdÅrsak oppholdÅrsak,
                                          SvpOppholdKilde oppholdKilde,
                                          boolean forVisning) {

    public enum SvpOppholdKilde {
        SØKNAD,
        INNTEKTSMELDING,
        REGISTRERT_AV_SAKSBEHANDLER
    }
}
