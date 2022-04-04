package no.nav.foreldrepenger.domene.vedtak.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.FagsakStatusEventPubliserer;
import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
public class OppdaterFagsakStatusImpl extends OppdaterFagsakStatus {

    private BehandlingRepository behandlingRepository;

    private FamilieHendelseRepository familieGrunnlagRepository;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private Period foreldelsesfrist;

    /**
     * @param foreldelsesfrist - Foreldelsesfrist i år (positivt heltall), før fagsak avsluttes
     */
    @Inject
    public OppdaterFagsakStatusImpl(BehandlingRepository behandlingRepository,
                                    FagsakRepository fagsakRepository,
                                    FagsakStatusEventPubliserer fagsakStatusEventPubliserer,
                                    BehandlingsresultatRepository behandlingsresultatRepository,
                                    FamilieHendelseRepository familieHendelseRepository,
                                    @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) MaksDatoUttakTjeneste maksDatoUttakTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    @KonfigVerdi(value = "fp.foreldelsesfrist", defaultVerdi = "P3Y") Period foreldelsesfrist) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.fagsakStatusEventPubliserer = fagsakStatusEventPubliserer;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.familieGrunnlagRepository = familieHendelseRepository;
        this.maksDatoUttakTjeneste = maksDatoUttakTjeneste;
        this.uttakInputTjeneste = uttakInputTjeneste;
        this.foreldelsesfrist = foreldelsesfrist;
    }

    OppdaterFagsakStatusImpl() {
        // CDI
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
        if (alleÅpneBehandlinger.isEmpty()) {//Edit
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
            var avslåttEllerOpphørt = erBehandlingResultatAvslåttEllerOpphørt(sisteYtelsesvedtak.get());

            var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(sisteYtelsesvedtak.get().getId());

            if (familieHendelseGrunnlag.isPresent()) {

                if (avslåttEllerOpphørt && levendeBarnFinnes(familieHendelseGrunnlag)) return true;

                var hendelseDato = familieHendelseGrunnlag
                    .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                    .map(FamilieHendelseEntitet::getSkjæringstidspunkt);
                var uttakInput = uttakInputTjeneste.lagInput(sisteYtelsesvedtak.get());
                var maksDatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);

                return erDatoUtløpt(maksDatoUttak, LocalDate.now())
                    || erDatoUtløpt(hendelseDato, LocalDate.now().minus(foreldelsesfrist));
            }
            return false;
        }
        return true;
    }

    private boolean levendeBarnFinnes(Optional<FamilieHendelseGrunnlagEntitet> familieHendelseGrunnlag) {
        var barna = familieHendelseGrunnlag
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getBarna);
        return barna.isEmpty() || barna.get().isEmpty() || barna.get().stream().anyMatch(barn -> barn.getDødsdato().isEmpty());
    }

    private boolean erDatoUtløpt(Optional<LocalDate> dato, LocalDate grensedato) {
        return dato.filter(d -> d.isBefore(grensedato)).isPresent();
    }
}
