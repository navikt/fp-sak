package no.nav.foreldrepenger.mottak.hendelser;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.Forretningshendelse;

import java.util.List;
import java.util.Map;

public interface ForretningshendelseSaksvelger<T extends Forretningshendelse> {
    Map<BehandlingÅrsakType, List<Fagsak>> finnRelaterteFagsaker(T forretningshendelse);
}
