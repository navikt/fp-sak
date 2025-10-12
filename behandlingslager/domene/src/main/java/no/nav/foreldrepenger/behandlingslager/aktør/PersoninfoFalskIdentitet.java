package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;

public record PersoninfoFalskIdentitet(String navn,
                                       LocalDate fødselsdato,
                                       NavBrukerKjønn kjønn,
                                       Landkoder statsborgerskap,
                                       PersonstatusType personstatus) {

}
