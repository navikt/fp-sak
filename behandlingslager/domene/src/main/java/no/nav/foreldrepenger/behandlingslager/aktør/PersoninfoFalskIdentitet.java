package no.nav.foreldrepenger.behandlingslager.aktør;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.PersonIdent;

public record PersoninfoFalskIdentitet(AktørId aktørId,
                                       PersonIdent personIdent,
                                       String navn,
                                       LocalDate fødselsdato,
                                       NavBrukerKjønn kjønn,
                                       Landkoder statsborgerskap,
                                       PersonstatusType personstatus) {

}
