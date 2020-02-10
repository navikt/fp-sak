package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.RegisterinnhentingHistorikkinnslagTjeneste;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.FPDateUtil;

/**
 * Oppdaterer registeropplysninger for engangsstønader og skrur behandlingsprosessen tilbake
 * til innhent-steget hvis det har skjedd endringer siden forrige innhenting.
 */
@ApplicationScoped
public class RegisterdataEndringshåndterer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterdataEndringshåndterer.class);
    private RegisterdataInnhenter registerdataInnhenter;
    private TemporalAmount oppdatereRegisterdataTidspunkt;
    private BehandlingRepository behandlingRepository;
    private Endringskontroller endringskontroller;
    private EndringsresultatSjekker endringsresultatSjekker;
    private RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    RegisterdataEndringshåndterer() {
        // for CDI proxy
    }

    /**
     * @param periode - Periode for hvor ofte registerdata skal oppdateres
     */
    @Inject
    public RegisterdataEndringshåndterer( // NOSONAR jobber med å redusere
                                          BehandlingRepositoryProvider repositoryProvider,
                                          RegisterdataInnhenter registerdataInnhenter,
                                          @KonfigVerdi(value = "oppdatere.registerdata.tidspunkt", defaultVerdi = "PT10H") String oppdaterRegisterdataEtterPeriode,
                                          Endringskontroller endringskontroller,
                                          EndringsresultatSjekker endringsresultatSjekker,
                                          RegisterinnhentingHistorikkinnslagTjeneste historikkinnslagTjeneste,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          BehandlingÅrsakTjeneste behandlingÅrsakTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.registerdataInnhenter = registerdataInnhenter;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.endringskontroller = endringskontroller;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.historikkinnslagTjeneste = historikkinnslagTjeneste;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        if (oppdaterRegisterdataEtterPeriode != null) {
            this.oppdatereRegisterdataTidspunkt = Duration.parse(oppdaterRegisterdataEtterPeriode);
        }
    }

    public boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        if (behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            return false;
        }
        LocalDateTime midnatt = FPDateUtil.iDag().atStartOfDay();
        Optional<LocalDateTime> opplysningerOppdatertTidspunkt = behandlingRepository.hentSistOppdatertTidspunkt(behandling.getId());
        if (oppdatereRegisterdataTidspunkt == null) {
            // konfig-verdien er ikke satt
            return erOpplysningerOppdatertTidspunktFør(midnatt, opplysningerOppdatertTidspunkt);
        }
        LocalDateTime nårOppdatereRegisterdata = FPDateUtil.nå().minus(oppdatereRegisterdataTidspunkt);
        if (nårOppdatereRegisterdata.isAfter(midnatt)) {
            // konfigverdien er etter midnatt, da skal midnatt gjelde
            return erOpplysningerOppdatertTidspunktFør(midnatt, opplysningerOppdatertTidspunkt);
        }
        // konfigverdien er før midnatt, da skal konfigverdien gjelde
        return erOpplysningerOppdatertTidspunktFør(nårOppdatereRegisterdata, opplysningerOppdatertTidspunkt);
    }

    public void sikreInnhentingRegisteropplysningerVedNesteOppdatering(Behandling behandling) {
        // Flytt oppdatert tidspunkt passe langt tilbale
        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, FPDateUtil.nå().minusWeeks(1).minusDays(1));
    }

    boolean erOpplysningerOppdatertTidspunktFør(LocalDateTime nårOppdatereRegisterdata,
                                                Optional<LocalDateTime> opplysningerOppdatertTidspunkt) {
        return opplysningerOppdatertTidspunkt.isPresent() && opplysningerOppdatertTidspunkt.get().isBefore(nårOppdatereRegisterdata);
    }

    public void reposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff endringsresultat) {
        if (!endringskontroller.erRegisterinnhentingPassert(behandling)) {
            return;
        }
        doReposisjonerBehandlingVedEndringer(behandling, endringsresultat, false);
    }

    private void doReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff endringsresultat, boolean utledÅrsaker) {
        boolean gåttOverTerminDatoOgIngenFødselsdato = isGåttOverTerminDatoOgIngenFødselsdato(behandling.getId());
        if (gåttOverTerminDatoOgIngenFødselsdato || endringsresultat.erSporedeFeltEndret()) {
            LOGGER.info("Starter behandlingId={} på nytt. gåttOverTerminDatoOgIngenFødselsdato={}, {}",
                behandling.getId(), gåttOverTerminDatoOgIngenFødselsdato, endringsresultat); // NOSONAR //$NON-NLS-1$
            if (utledÅrsaker) {
                lagBehandlingÅrsakerOgHistorikk(behandling, endringsresultat);
            }
            endringskontroller.spolTilStartpunkt(behandling, endringsresultat);
        }
    }

    public void oppdaterRegisteropplysningerOgReposisjonerBehandlingVedEndringer(Behandling behandling) {
        if (!endringskontroller.erRegisterinnhentingPassert(behandling)) {
            return;
        }
        boolean skalOppdatereRegisterdata = skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Utled diff hvis registerdata skal oppdateres
        EndringsresultatDiff endringsresultat = skalOppdatereRegisterdata ? oppdaterRegisteropplysninger(behandling) : opprettDiffUtenEndring();

        doReposisjonerBehandlingVedEndringer(behandling, endringsresultat, true);
    }

    private boolean isGåttOverTerminDatoOgIngenFødselsdato(Long behandlingId) {
        Optional<FamilieHendelseGrunnlagEntitet> fhGrunnlag = familieHendelseTjeneste.finnAggregat(behandlingId);
        return fhGrunnlag.isEmpty() || familieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(fhGrunnlag.get());
    }

    private EndringsresultatDiff oppdaterRegisteropplysninger(Behandling behandling) {
        EndringsresultatSnapshot grunnlagSnapshot = endringsresultatSjekker.opprettEndringsresultatPåBehandlingsgrunnlagSnapshot(behandling.getId());

        registerdataInnhenter.innhentPersonopplysninger(behandling);
        registerdataInnhenter.innhentMedlemskapsOpplysning(behandling);
        registerdataInnhenter.innhentIAYIAbakus(behandling);

        // oppdater alltid tidspunktet grunnlagene ble oppdater eller forsøkt oppdatert!
        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, FPDateUtil.nå());
        // Finn alle endringer som registerinnhenting har gjort på behandlingsgrunnlaget
        EndringsresultatDiff endringsresultat = endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), grunnlagSnapshot);
        return endringsresultat;
    }

    private EndringsresultatDiff opprettDiffUtenEndring() {
        return EndringsresultatDiff.opprettForSporingsendringer();
    }

    private void lagBehandlingÅrsakerOgHistorikk(Behandling behandling, EndringsresultatDiff endringsresultat) {
        Set<BehandlingÅrsakType> behandlingÅrsakTyper = new HashSet<>();
        behandlingÅrsakTyper.add(BehandlingÅrsakType.RE_REGISTEROPPLYSNING);
        if (!FagsakYtelseType.ENGANGSTØNAD.equals(behandling.getFagsakYtelseType())) {
            var ref = BehandlingReferanse.fra(behandling, this.skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()));
            behandlingÅrsakTyper.addAll(behandlingÅrsakTjeneste.utledBehandlingÅrsakerBasertPåDiff(ref, endringsresultat));
        }
        leggTilBehandlingsårsaker(behandling, behandlingÅrsakTyper);
        lagHistorikkinnslag(behandling, behandlingÅrsakTyper);
    }

    private void leggTilBehandlingsårsaker(Behandling behandling, Set<BehandlingÅrsakType> årsakTyper) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(new ArrayList<>(årsakTyper));
        behandling.getOriginalBehandling().ifPresent(builder::medOriginalBehandling);
        builder.buildFor(behandling);
    }

    private void lagHistorikkinnslag(Behandling behandling, Set<BehandlingÅrsakType> behandlingÅrsakTyper) {
        if (behandlingÅrsakTyper.contains(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD);
            return;
        }
        if (behandlingÅrsakTyper.contains(BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER)) {
            historikkinnslagTjeneste.opprettHistorikkinnslagForBehandlingMedNyeOpplysninger(behandling, BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER);
            return;
        }
        historikkinnslagTjeneste.opprettHistorikkinnslagForNyeRegisteropplysninger(behandling);
    }
}
