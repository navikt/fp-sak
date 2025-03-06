package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record OmsorgOgRettDto(Søknad søknad, RegisterData registerdata, ManuellBehandlingResultat manuellBehandlingResultat,
                              RelasjonsRolleType relasjonsRolleType) {

    public record Søknad(Verdi søkerHarAleneomsorg, String annenpartIdent, Landkoder annenpartBostedsland, Rettighet annenpartRettighet) {
    }

    public record Rettighet(Verdi harRettNorge, Verdi harOppholdEØS, Verdi harRettEØS, Verdi harUføretrygd) {
    }
    public enum Verdi {
        JA, NEI, IKKE_RELEVANT;

        static Verdi fra(Boolean verdi) {
            return verdi == null ? IKKE_RELEVANT : verdi ? JA : NEI;
        }
    }

    public record RegisterData(Verdi harAnnenpartUføretrygd, Verdi harAnnenpartForeldrepenger, Verdi harAnnenpartEngangsstønad) {
    }

    public record ManuellBehandlingResultat(Verdi søkerHarAleneomsorg, Rettighet annenpartRettighet) {
    }
}
