package no.nav.foreldrepenger.domene.registerinnhenting;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.registerinnhenting.impl.Endringskontroller;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.konfig.KonfigVerdi;

/**
 * Oppdaterer registeropplysninger for engangsstønader og skrur behandlingsprosessen tilbake
 * til innhent-steget hvis det har skjedd endringer siden forrige innhenting.
 */
@ApplicationScoped
public class RegisterdataEndringshåndterer {
    private static final Logger LOG = LoggerFactory.getLogger(RegisterdataEndringshåndterer.class);
    private RegisterdataInnhenter registerdataInnhenter;
    private TemporalAmount oppdatereRegisterdataTidspunkt;
    private BehandlingRepository behandlingRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private Endringskontroller endringskontroller;
    private EndringsresultatSjekker endringsresultatSjekker;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    RegisterdataEndringshåndterer() {
        // for CDI proxy
    }

    /**
     * @param periode - Periode for hvor ofte registerdata skal oppdateres
     */
    @Inject
    public RegisterdataEndringshåndterer( BehandlingRepositoryProvider repositoryProvider,
                                          RegisterdataInnhenter registerdataInnhenter,
                                          @KonfigVerdi(value = "oppdatere.registerdata.tidspunkt", defaultVerdi = "PT10H") String oppdaterRegisterdataEtterPeriode,
                                          Endringskontroller endringskontroller,
                                          EndringsresultatSjekker endringsresultatSjekker,
                                          FamilieHendelseTjeneste familieHendelseTjeneste,
                                          BehandlingÅrsakTjeneste behandlingÅrsakTjeneste) {

        this.registerdataInnhenter = registerdataInnhenter;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.endringskontroller = endringskontroller;
        this.endringsresultatSjekker = endringsresultatSjekker;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
        if (oppdaterRegisterdataEtterPeriode != null) {
            this.oppdatereRegisterdataTidspunkt = Duration.parse(oppdaterRegisterdataEtterPeriode);
        }
    }

    public boolean skalInnhenteRegisteropplysningerPåNytt(Behandling behandling) {
        if (!endringskontroller.erRegisterinnhentingPassert(behandling) || erAvslag(behandling)
            || behandling.erSaksbehandlingAvsluttet() || behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            return false;
        }
        var midnatt = LocalDate.now().atStartOfDay();
        var opplysningerOppdatertTidspunkt = behandlingRepository.hentSistOppdatertTidspunkt(behandling.getId());
        if (oppdatereRegisterdataTidspunkt == null) {
            // konfig-verdien er ikke satt
            return erOpplysningerOppdatertTidspunktFør(midnatt, opplysningerOppdatertTidspunkt);
        }
        var nårOppdatereRegisterdata = LocalDateTime.now().minus(oppdatereRegisterdataTidspunkt);
        if (nårOppdatereRegisterdata.isAfter(midnatt)) {
            // konfigverdien er etter midnatt, da skal midnatt gjelde
            return erOpplysningerOppdatertTidspunktFør(midnatt, opplysningerOppdatertTidspunkt);
        }
        // konfigverdien er før midnatt, da skal konfigverdien gjelde
        return erOpplysningerOppdatertTidspunktFør(nårOppdatereRegisterdata, opplysningerOppdatertTidspunkt);
    }

    public void sikreInnhentingRegisteropplysningerVedNesteOppdatering(Behandling behandling) {
        // Flytt oppdatert tidspunkt passe langt tilbale
        behandlingRepository.oppdaterSistOppdatertTidspunkt(behandling, LocalDateTime.now().minusWeeks(1).minusDays(1));
    }

    boolean erOpplysningerOppdatertTidspunktFør(LocalDateTime nårOppdatereRegisterdata,
                                                Optional<LocalDateTime> opplysningerOppdatertTidspunkt) {
        return opplysningerOppdatertTidspunkt.isPresent() && opplysningerOppdatertTidspunkt.get().isBefore(nårOppdatereRegisterdata);
    }

    private void doReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatDiff endringsresultat, boolean utledÅrsaker) {
        var gåttOverTerminDatoOgIngenFødselsdato = isGåttOverTerminDatoOgIngenFødselsdato(behandling.getId());
        if (gåttOverTerminDatoOgIngenFødselsdato || endringsresultat.erSporedeFeltEndret()) {
            LOG.info("Starter behandlingId={} på nytt. gåttOverTerminDatoOgIngenFødselsdato={}, {}",
                behandling.getId(), gåttOverTerminDatoOgIngenFødselsdato, endringsresultat); // NOSONAR //$NON-NLS-1$
            if (utledÅrsaker) {
                behandlingÅrsakTjeneste.lagHistorikkForRegisterEndringsResultat(behandling, endringsresultat);
            }
            // Sikre håndtering av manglende fødsel
            endringskontroller.spolTilStartpunkt(behandling, endringsresultat,
                senesteStartpunkt(behandling, gåttOverTerminDatoOgIngenFødselsdato));
        }
    }

    private StartpunktType senesteStartpunkt(Behandling behandling, boolean gåttOverTerminDatoOgIngenFødselsdato) {
        if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(behandling.getFagsakYtelseType()))
            return StartpunktType.UDEFINERT;
        return gåttOverTerminDatoOgIngenFødselsdato ? StartpunktType.SØKERS_RELASJON_TIL_BARNET : StartpunktType.UDEFINERT;
    }

    public void utledDiffOgReposisjonerBehandlingVedEndringer(Behandling behandling, EndringsresultatSnapshot grunnlagSnapshot, boolean utledÅrsaker) {
        if (!endringskontroller.erRegisterinnhentingPassert(behandling)) {
            return;
        }
        var endringsresultat = grunnlagSnapshot != null ?
            endringsresultatSjekker.finnSporedeEndringerPåBehandlingsgrunnlag(behandling.getId(), grunnlagSnapshot) : opprettDiffUtenEndring();

        doReposisjonerBehandlingVedEndringer(behandling, endringsresultat, utledÅrsaker);
    }

    private boolean isGåttOverTerminDatoOgIngenFødselsdato(Long behandlingId) {
        var fhGrunnlag = familieHendelseTjeneste.finnAggregat(behandlingId);
        return fhGrunnlag.isEmpty() || FamilieHendelseTjeneste.getManglerFødselsRegistreringFristUtløpt(fhGrunnlag.get());
    }

    private EndringsresultatDiff opprettDiffUtenEndring() {
        return EndringsresultatDiff.opprettForSporingsendringer();
    }

    private boolean erAvslag(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId())
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene).orElse(Collections.emptyList()).stream()
            .map(Vilkår::getGjeldendeVilkårUtfall)
            .anyMatch(VilkårUtfallType.IKKE_OPPFYLT::equals);
    }
}
