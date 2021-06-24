package no.nav.foreldrepenger.mottak.hendelser.freg;


import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

public record FødselForretningshendelse(List<AktørId> aktørIdListe,
                                        LocalDate fødselsdato,
                                        Endringstype endringstype) implements Forretningshendelse {
}
