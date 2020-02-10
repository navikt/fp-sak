package no.nav.foreldrepenger.mottak.hendelser;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.hendelser.ForretningshendelseType;

public interface ForretningshendelseHåndterer {

    void håndterÅpenBehandling(Behandling åpenBehandling, BehandlingÅrsakType behandlingÅrsakType);

    void håndterAvsluttetBehandling(Behandling avsluttetBehandling, ForretningshendelseType forretningshendelseType, BehandlingÅrsakType behandlingÅrsakType);

    void håndterKøetBehandling(Fagsak fagsak, BehandlingÅrsakType behandlingÅrsakType);

}
