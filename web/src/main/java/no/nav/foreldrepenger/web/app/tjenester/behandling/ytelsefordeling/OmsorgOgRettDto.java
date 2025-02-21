package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;

public record OmsorgOgRettDto(Søknad søknad, RegisterData registerdata, ManuellBehandlingResultat manuellBehandlingResultat) {

    public record Søknad(Boolean søkerHarAleneomsorg, String annenpartNavn, String annenpartIdent, Landkoder annenpartBostedsland,
                         Rettighet annenpartRettighet) {

        public record Rettighet(boolean harRettNorge, Boolean harOppholdEØS, Boolean harRettEØS, Boolean harUføretrygd) {
        }

    }

    public record RegisterData(Set<PersonadresseDto> søkersAdresser, Set<PersonadresseDto> annenpartAdresser, Set<PersonadresseDto> barnasAdresser,
                               SivilstandType sivilstand, Boolean harAnnenpartUføretrygd, Boolean harAnnenpartForeldrepenger,
                               Boolean harAnnenpartEngangsstønad) {

    }

    public record ManuellBehandlingResultat(Boolean harAleneomsorg, Boolean harAnnenpartRettNorge, Boolean harAnnenpartOppholdEØS,
                                            Boolean harAnnenpartRettEØS, Boolean harAnnenpartUføretrygd) {

    }
}
