package no.nav.foreldrepenger.domene.vedtak.svp;

import java.time.LocalDate;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
public class OppdaterFagsakStatusImpl extends OppdaterFagsakStatus {

    private BehandlingRepository behandlingRepository;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;

    OppdaterFagsakStatusImpl(){
        //CDI
    }

    @Inject
    public OppdaterFagsakStatusImpl(BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    FagsakStatusEventPubliserer fagsakStatusEventPubliserer,
                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                    @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) MaksDatoUttakTjeneste maksDatoUttakTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.maksDatoUttakTjeneste = maksDatoUttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;

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

        // ingen andre behandlinger er åpne
        if (alleÅpneBehandlinger.isEmpty()) {
            if (behandling == null || ingenLøpendeYtelsesvedtak(behandling)) {
                oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            }
            oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.LØPENDE);
            return FagsakStatusOppdateringResultat.FAGSAK_LØPENDE;
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    boolean ingenLøpendeYtelsesvedtak(Behandling behandling) {
        var sisteYtelsesvedtak = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

        if (sisteYtelsesvedtak.isPresent()) {
            if (erBehandlingResultatAvslåttEllerOpphørt(sisteYtelsesvedtak.get())) return true;
            var uttakInput = uttakInputTjeneste.lagInput(sisteYtelsesvedtak.get());
            var maksDatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);
            if (maksDatoUttak.isEmpty()) {
                // Kan ikke avgjøre om dato er utløpt
                return false;
            }
            return maksDatoUttak.get().isBefore(LocalDate.now());
        }
        return true;
    }
}
