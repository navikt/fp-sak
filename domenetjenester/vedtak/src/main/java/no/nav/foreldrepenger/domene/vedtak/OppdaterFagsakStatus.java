package no.nav.foreldrepenger.domene.vedtak;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
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

    public abstract void settUnderBehandlingNårAktiveBehandlinger(Fagsak fagsak);

    public void oppdaterFagsakStatus(Fagsak fagsak, Behandling behandling, FagsakStatus nyStatus) {
        var gammelStatus = fagsak.getStatus();
        var fagsakId = fagsak.getId();
        fagsakRepository.oppdaterFagsakStatus(fagsakId, nyStatus);

        if (fagsakStatusEventPubliserer != null) {
            fagsakStatusEventPubliserer.fireEvent(fagsak, behandling, gammelStatus, nyStatus);
        }
    }

    public boolean erBehandlingResultatAvslåttEllerOpphørt(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .filter(Behandlingsresultat::isBehandlingsresultatAvslåttOrOpphørt)
            .isPresent();
    }
}
