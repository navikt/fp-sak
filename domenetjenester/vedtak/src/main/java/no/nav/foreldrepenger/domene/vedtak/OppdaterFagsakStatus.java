package no.nav.foreldrepenger.domene.vedtak;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;

public abstract class OppdaterFagsakStatus {

    protected FagsakRepository fagsakRepository;
    protected FagsakStatusEventPubliserer fagsakStatusEventPubliserer;
    protected BehandlingsresultatRepository behandlingsresultatRepository;

    public abstract FagsakStatusOppdateringResultat oppdaterFagsakNårBehandlingEndret(Behandling behandling);

    public abstract void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak);

    public void oppdaterFagsakStatus(Fagsak fagsak, Behandling behandling, FagsakStatus nyStatus) {
        FagsakStatus gammelStatus = fagsak.getStatus();
        Long fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);

        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(fagsak, behandling, gammelStatus, nyStatus);
        }
    }

    public boolean erBehandlingResultatAvslåttEllerOpphørt(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .map(resultat -> resultat.isBehandlingsresultatAvslåttOrOpphørt())
            .orElse(Boolean.FALSE);
    }
}
