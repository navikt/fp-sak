package no.nav.foreldrepenger.domene.vedtak.fp;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.revurdering.ytelse.UttakInputTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.AdopsjonEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.domene.uttak.saldo.MaksDatoUttakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.FagsakStatusOppdateringResultat;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatus;
import no.nav.foreldrepenger.domene.vedtak.OppdaterFagsakStatusFelles;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
public class OppdaterFagsakStatusImpl implements OppdaterFagsakStatus {

    private BehandlingRepository behandlingRepository;
    private OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles;

    private FamilieHendelseRepository familieGrunnlagRepository;
    private MaksDatoUttakTjeneste maksDatoUttakTjeneste;
    private UttakInputTjeneste uttakInputTjeneste;
    private Period foreldelsesfrist;

    /**
     * @param foreldelsesfrist - Foreldelsesfrist i år (positivt heltall), før fagsak avsluttes
     */
    @Inject
    public OppdaterFagsakStatusImpl(BehandlingRepositoryProvider repositoryProvider,
                                    OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles,
                                    MaksDatoUttakTjeneste maksDatoUttakTjeneste,
                                    UttakInputTjeneste uttakInputTjeneste,
                                    @KonfigVerdi(value = "fp.foreldelsesfrist", defaultVerdi = "P3Y") Period foreldelsesfrist) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppdaterFagsakStatusFelles = oppdaterFagsakStatusFelles;
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
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

        // ingen andre behandlinger er åpne
        if (alleÅpneBehandlinger.isEmpty()) {
            if (behandling == null || ingenLøpendeYtelsesvedtak(behandling)) {
                oppdaterFagsakStatusFelles.oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
                return FagsakStatusOppdateringResultat.FAGSAK_AVSLUTTET;
            }
            oppdaterFagsakStatusFelles.oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.LØPENDE);
            return FagsakStatusOppdateringResultat.FAGSAK_LØPENDE;
        }
        return FagsakStatusOppdateringResultat.INGEN_OPPDATERING;
    }

    boolean ingenLøpendeYtelsesvedtak(Behandling behandling) {
        if (oppdaterFagsakStatusFelles.ingenLøpendeYtelsesvedtak(behandling)) {
            return true;
        }
        Optional<Behandling> sisteInnvilgedeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

        if (sisteInnvilgedeBehandling.isPresent()) {
            var familieHendelseGrunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(behandling.getId());
            if (familieHendelseGrunnlag.isPresent()) {
                Optional<LocalDate> fødselsdato = familieHendelseGrunnlag
                    .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                    .flatMap(FamilieHendelseEntitet::getFødselsdato);
                Optional<LocalDate> omsorgsovertalsesdato = familieHendelseGrunnlag
                    .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
                    .flatMap(FamilieHendelseEntitet::getAdopsjon)
                    .map(AdopsjonEntitet::getOmsorgsovertakelseDato);
                var uttakInput = uttakInputTjeneste.lagInput(behandling);
                var maksDatoUttak = maksDatoUttakTjeneste.beregnMaksDatoUttak(uttakInput);

                return erDatoUtløpt(maksDatoUttak, LocalDate.now())
                    || erDatoUtløpt(fødselsdato, LocalDate.now().minus(foreldelsesfrist))
                    || erDatoUtløpt(omsorgsovertalsesdato, LocalDate.now().minus(foreldelsesfrist));
            }
            return false;
        }
        return true;
    }

    private boolean erDatoUtløpt(Optional<LocalDate> dato, LocalDate grensedato) {
        if (dato.isEmpty()) {
            // Kan ikke avgjøre om dato er utløpt
            return false;
        }
        return dato.get().isBefore(grensedato);
    }
}
