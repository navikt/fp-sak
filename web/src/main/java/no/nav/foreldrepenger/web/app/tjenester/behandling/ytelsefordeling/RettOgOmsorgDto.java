package no.nav.foreldrepenger.web.app.tjenester.behandling.ytelsefordeling;

import java.util.Set;

import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.SivilstandType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.web.app.tjenester.behandling.personopplysning.PersonadresseDto;

public record RettOgOmsorgDto(OppgittAnnenpart oppgittAnnenpart, RegisterData registerdata, ManuellBehandlingResultat manuellBehandlingResultat) {

    public record OppgittAnnenpart(String navn, String ident, Landkoder bostedsland, Boolean harAleneomsorg, Rettighet rettighet) {

        public record Rettighet(boolean harRettNorge, Boolean harOppholdEØS, Boolean harRettEØS, Boolean harUføretrygd) {
        }

    }

    public record RegisterData(Set<PersonadresseDto> adresser, Set<PersonadresseDto> annenpartAdresser, SivilstandType sivilstand,
                               Boolean harAnnenpartUføretrygd, Boolean harAnnenpartForeldrepenger, Boolean harAnnenpartEngangsstønad) {

    }

    public record ManuellBehandlingResultat(Boolean harAleneomsorg, Boolean harAnnenpartRettNorge, Boolean harAnnenpartOppholdEØS,
                                            Boolean harAnnenpartRettEØS, Boolean harAnnenpartUføretrygd) {

    }
}
