package no.nav.foreldrepenger.behandlingslager.aktør.historikk;

import java.util.List;

public record Personhistorikkinfo(List<PersonstatusPeriode> personstatushistorikk,
                                  List<OppholdstillatelsePeriode> oppholdstillatelsehistorikk,
                                  List<StatsborgerskapPeriode> statsborgerskaphistorikk,
                                  List<AdressePeriode> adressehistorikk) {

}
