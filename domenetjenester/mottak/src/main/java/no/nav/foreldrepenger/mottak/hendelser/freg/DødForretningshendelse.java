package no.nav.foreldrepenger.mottak.hendelser.freg;

import no.nav.foreldrepenger.behandlingslager.hendelser.Endringstype;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.domene.typer.AktørId;

import java.time.LocalDate;
import java.util.List;

public record DødForretningshendelse(List<AktørId> aktørIdListe,
                                     LocalDate dødsdato,
                                     Endringstype endringstype) implements Forretningshendelse {

}
