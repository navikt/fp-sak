package no.nav.foreldrepenger.mottak.hendelser;

import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;
import no.nav.foreldrepenger.mottak.hendelser.kontrakt.ForretningshendelseDto;

public interface ForretningshendelseOversetter<T extends Forretningshendelse> {
    T oversett(ForretningshendelseDto forretningshendelse);
}
