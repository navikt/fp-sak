package no.nav.foreldrepenger.domene.vedtak.es;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusFelles;

@ApplicationScoped
@FagsakYtelseTypeRef("ES")
public class OppdaterFagsakStatusImpl implements OppdaterFagsakStatus {

    private BehandlingRepository behandlingRepository;
    private OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles;

    OppdaterFagsakStatusImpl() {
        // CDI
    }

    @Inject
    public OppdaterFagsakStatusImpl(BehandlingRepository behandlingRepository,
                                  OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles) {
        this.behandlingRepository = behandlingRepository;
        this.oppdaterFagsakStatusFelles = oppdaterFagsakStatusFelles;
    }

    @Override
    public FagsakStatusOppdateringResultat oppdaterFagsakNårBehandlingEndret(Behandling behandling) {
        return oppdaterFagsak(behandling);
    }

    @Override
    public void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak) {
        avsluttFagsakNårAlleBehandlingerErLukket(fagsak, null);
    }

    private FagsakStatusOppdateringResultat oppdaterFagsak(Behandling behandling) {

        if (Objects.equals(BehandlingStatus.AVSLUTTET, behandling.getStatus())) {
            return avsluttFagsakNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        }
        // hvis en Behandling har noe annen status, setter Fagsak til Under behandling
        oppdaterFagsakStatusFelles.oppdaterFagsakStatus(behandling.getFagsak(), behandling, FagsakStatus.UNDER_BEHANDLING);
        return FagsakStatusOppdateringResultat.FAGSAK_UNDER_BEHANDLING;
    }

    private FagsakStatusOppdateringResultat avsluttFagsakNårAlleBehandlingerErLukket(Fagsak fagsak, Behandling behandling) {
        List<Behandling> alleÅpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsak.getId());
        if (behandling != null) {
            alleÅpneBehandlinger.remove(behandling);
        }

        if (alleÅpneBehandlinger.isEmpty()) {
            oppdaterFagsakStatusFelles.oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
            return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }
}
