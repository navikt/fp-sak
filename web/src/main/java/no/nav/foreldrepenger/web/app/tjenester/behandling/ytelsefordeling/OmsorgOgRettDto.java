package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record OmsorgOgRettDto(Søknad søknad, RegisterData registerdata, ManuellBehandlingResultat manuellBehandlingResultat,
                              RelasjonsRolleType relasjonsRolleType) {

    public record Søknad(Boolean søkerHarAleneomsorg, String annenpartNavn, String annenpartIdent, Landkoder annenpartBostedsland,
                         Rettighet annenpartRettighet) {
    }

    public record Rettighet(boolean harRettNorge, Boolean harOppholdEØS, Boolean harRettEØS, Boolean harUføretrygd) {
    }

    public record RegisterData(Boolean harAnnenpartUføretrygd, Boolean harAnnenpartForeldrepenger, Boolean harAnnenpartEngangsstønad) {
    }

    public record ManuellBehandlingResultat(Boolean søkerHarAleneomsorg, Rettighet annenpartRettighet) {
    }
}
