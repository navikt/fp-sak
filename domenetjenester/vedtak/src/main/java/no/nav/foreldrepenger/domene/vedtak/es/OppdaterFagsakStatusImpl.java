package no.nav.foreldrepenger.domene.vedtak.es;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.ENGANGSTØNAD)
public class OppdaterFagsakStatusImpl extends OppdaterFagsakStatus {

    private BehandlingRepository behandlingRepository;

    OppdaterFagsakStatusImpl() {
        // CDI
    }

    @Inject
    public OppdaterFagsakStatusImpl(BehandlingRepository behandlingRepository ,
                                    FagsakRepository fagsakRepository,
                                    FagsakStatusEventPubliserer fagsakStatusEventPubliserer) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
    }

    @Override
    public FagsakStatusOppdateringResultat oppdaterFagsakNårBehandlingEndret(Behandling behandling) {
        return oppdaterFagsak(behandling);
    }

    @Override
    public void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak) {
        avsluttFagsakNårAlleBehandlingerErLukket(fagsak, null);
    }

    @Override
    public void settUnderBehandlingNårAktiveBehandlinger(Fagsak fagsak) {
        if (behandlingRepository.harÅpenOrdinærYtelseBehandlingerForFagsakId(fagsak.getId()) && FagsakStatus.LØPENDE.equals(fagsak.getStatus())) {
            var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsak.getId()).orElseThrow();
            oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.UNDER_BEHANDLING);
        }
    }


    private FagsakStatusOppdateringResultat oppdaterFagsak(Behandling behandling) {

        if (Objects.equals(BehandlingStatus.AVSLUTTET, behandling.getStatus())) {
            return avsluttFagsakNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        }
        // hvis en Behandling har noe annen status, setter Fagsak til Under behandling
        oppdaterFagsakStatus(behandling.getFagsak(), behandling, FagsakStatus.UNDER_BEHANDLING);
        return FagsakStatusOppdateringResultat.FAGSAK_UNDER_BEHANDLING;
    }

    private FagsakStatusOppdateringResultat avsluttFagsakNårAlleBehandlingerErLukket(Fagsak fagsak, Behandling behandling) {
        var alleÅpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsak.getId());
        if (behandling != null) {
            alleÅpneBehandlinger.remove(behandling);
        }

        if (alleÅpneBehandlinger.isEmpty()) {
            oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
            return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }
}
