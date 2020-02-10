package no.nav.foreldrepenger.domene.vedtak;

import java.util.Optional;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;

@Dependent
public class OppdaterFagsakStatusFelles {

    private FagsakRepository fagsakRepository;
    private FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Inject
    public OppdaterFagsakStatusFelles(BehandlingRepositoryProvider provider,
                                      FagsakStatusEventPubliserer fagsakStatusEventPubliserer) {
        this.fagsakRepository = provider.getFagsakRepository();
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
    }

    public void oppdaterFagsakStatus(Fagsak fagsak, Behandling behandling, FagsakStatus nyStatus) {
        FagsakStatus gammelStatus = fagsak.getStatus();
        Long fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);

        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(fagsak, behandling, gammelStatus, nyStatus);
        }
    }

    public boolean ingenLøpendeYtelsesvedtak(Behandling behandling) {
        Optional<Behandling> sisteAvsluttedeIkkeHenlagteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

        if (sisteAvsluttedeIkkeHenlagteBehandling.isPresent()) {
            Behandling sisteBehandling = sisteAvsluttedeIkkeHenlagteBehandling.get();
            return erVedtakResultat(sisteBehandling, VedtakResultatType.AVSLAG) || erVedtakResultat(sisteBehandling, VedtakResultatType.OPPHØR);
        }
        return true;
    }

    private boolean erVedtakResultat(Behandling behandling, VedtakResultatType vedtakResultat) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .map(Behandlingsresultat::getBehandlingVedtak)
            .map(vedtak -> vedtak.getVedtakResultatType().equals(vedtakResultat))
            .orElse(Boolean.FALSE);
    }
}
