package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import jakarta.validation.constraints.NotNull;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record OmsorgOgRettDto(@NotNull Søknad søknad,
                              RegisterData registerdata,
                              ManuellBehandlingResultat manuellBehandlingResultat,
                              Rettighetstype rettighetstype,
                              @NotNull RelasjonsRolleType relasjonsRolleType) {

    public record Søknad(@NotNull Verdi søkerHarAleneomsorg, String annenpartIdent, Landkoder annenpartBostedsland, Rettighet annenpartRettighet) {
    }

    public record Rettighet(@NotNull Verdi harRettNorge, @NotNull Verdi harOppholdEØS, @NotNull Verdi harRettEØS, @NotNull Verdi harUføretrygd) {
    }
    public enum Verdi {
        JA, NEI, IKKE_RELEVANT;

        static Verdi fra(Boolean verdi) {
            return verdi == null ? IKKE_RELEVANT : verdi ? JA : NEI;
        }
    }

    public record RegisterData(@NotNull Verdi harAnnenpartUføretrygd, @NotNull Verdi harAnnenpartForeldrepenger, @NotNull Verdi harAnnenpartEngangsstønad) {
    }

    public record ManuellBehandlingResultat(@NotNull Verdi søkerHarAleneomsorg, Rettighet annenpartRettighet) {
    }
}
