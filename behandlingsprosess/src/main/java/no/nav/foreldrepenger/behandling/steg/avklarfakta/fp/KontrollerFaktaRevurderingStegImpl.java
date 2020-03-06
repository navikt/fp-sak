package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.RyddRegisterData;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelsesFordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakRepository;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOFAK")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
class KontrollerFaktaRevurderingStegImpl implements KontrollerFaktaSteg {
    private static final Logger LOGGER = LoggerFactory.getLogger(KontrollerFaktaRevurderingStegImpl.class);

    private static final StartpunktType DEFAULT_STARTPUNKT = StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;

    private static final Set<AksjonspunktDefinisjon> AKSJONSPUNKT_SKAL_KOPIERES = Set.of(AksjonspunktDefinisjon.OVERSTYRING_AV_UTTAKPERIODER);

    private BehandlingRepository behandlingRepository;

    private KontrollerFaktaTjeneste tjeneste;

    private BehandlingRepositoryProvider repositoryProvider;

    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;

    private UttakRepository uttakRepository;

    private StartpunktTjeneste startpunktTjeneste;

    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    private BehandlingsresultatRepository behandlingsresultatRepository;

    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;

    KontrollerFaktaRevurderingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaRevurderingStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                       BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                       HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       @FagsakYtelseTypeRef("FP") KontrollerFaktaTjeneste tjeneste,
                                       @FagsakYtelseTypeRef("FP") StartpunktTjeneste startpunktTjeneste,
                                       BehandlingÅrsakTjeneste behandlingÅrsakTjeneste,
                                       BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                       MottatteDokumentTjeneste mottatteDokumentTjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tjeneste = tjeneste;
        this.hentBeregningsgrunnlagTjeneste = hentBeregningsgrunnlagTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.uttakRepository = repositoryProvider.getUttakRepository();
        this.startpunktTjeneste = startpunktTjeneste;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.harSattStartpunkt()) {
            // Startpunkt kan bare initieres én gang, og det gjøres i dette steget.
            // Suksessive eksekveringer av stegets aksjonspunktsutledere skjer utenfor steget
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        if (!behandling.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
            Set<BehandlingÅrsakType> behandlingÅrsaker = behandlingÅrsakTjeneste.utledBehandlingÅrsakerMotOriginalBehandling(ref);
            leggTilBehandlingsårsaker(behandling, behandlingÅrsaker);
        }

        StartpunktType startpunkt = utledStartpunkt(ref, behandling);
        behandling.setStartpunkt(startpunkt);

        // Kopier aksjonspunkter
        List<AksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        aksjonspunktResultater.addAll(kopierOverstyringerTilHøyreForStartpunkt(behandling, ref, startpunkt));
        aksjonspunktResultater.addAll(tjeneste.utledAksjonspunkterTilHøyreForStartpunkt(ref, startpunkt));
        kopierResultaterAvhengigAvStartpunkt(behandling, kontekst);

        TransisjonIdentifikator transisjon = TransisjonIdentifikator.forId(FellesTransisjoner.SPOLFREM_PREFIX + startpunkt.getBehandlingSteg().getKode());
        return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(transisjon, aksjonspunktResultater);
    }

    private List<AksjonspunktResultat> kopierOverstyringerTilHøyreForStartpunkt(Behandling behandling, BehandlingReferanse ref, StartpunktType startpunkt) {
        // Manuelle til høyre for startpunkt
        return behandling.getOriginalBehandling().map(Behandling::getAksjonspunkter).orElse(Collections.emptySet()).stream()
            .filter(Aksjonspunkt::erUtført)
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .filter(AKSJONSPUNKT_SKAL_KOPIERES::contains)
            .filter(apDef -> tjeneste.skalOverstyringLøsesTilHøyreForStartpunkt(ref, startpunkt, apDef))
            .map(AksjonspunktResultat::opprettForAksjonspunkt)
            .collect(Collectors.toList());
    }

    private StartpunktType utledStartpunkt(BehandlingReferanse ref, Behandling revurdering) {
        StartpunktType startpunkt = DEFAULT_STARTPUNKT; // Gjennomgå hele prosessen - for manuelle, etterkontroller og tidl.avslag
        if (!revurdering.erManueltOpprettet() && !erEtterkontrollRevurdering(revurdering)) {
            // Automatisk revurdering skal hoppe til utledet startpunkt. Unntaket er revurdering av avslåtte behandlinger
            if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING)) {
                startpunkt = StartpunktType.UTTAKSVILKÅR;
                LOGGER.info("Berørt behandling {} med  startpunkt {} ", revurdering.getId(), startpunkt.getKode());// NOSONAR //$NON-NLS-1$
                return startpunkt;
            } else {
                var orgBehandlingsresultat = getBehandlingsresultat(ref.getOriginalBehandlingId().get());
                if (orgBehandlingsresultat != null && !orgBehandlingsresultat.isVilkårAvslått()) {
                    // Revurdering av innvilget behandling. Hvis vilkår er avslått må man tillate re-evalueres
                    startpunkt = startpunktTjeneste.utledStartpunktMotOriginalBehandling(ref);
                    if (startpunkt.equals(StartpunktType.UDEFINERT)) {
                        startpunkt = inneholderEndringssøknadPerioderFørSkjæringstidspunkt(revurdering, ref) ?
                            StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP : StartpunktType.UTTAKSVILKÅR;
                    }
                }
            }
        }

        // Undersøk behov for GRegulering.
        if (!DEFAULT_STARTPUNKT.equals(startpunkt)) {
            StartpunktType greguleringStartpunkt = utledBehovForGRegulering(ref, revurdering);
            startpunkt = startpunkt.getRangering() < greguleringStartpunkt.getRangering() ? startpunkt : greguleringStartpunkt;
        }

        // Startpunkt for revurdering kan kun hoppe fremover; default dersom startpunkt passert
        if (behandlingskontrollTjeneste.erStegPassert(revurdering, startpunkt.getBehandlingSteg())) {
            startpunkt = DEFAULT_STARTPUNKT;
        }
        LOGGER.info("Revurdering {} har fått fastsatt startpunkt {} ", revurdering.getId(), startpunkt.getKode());// NOSONAR //$NON-NLS-1$
        return startpunkt;
    }

    private boolean inneholderEndringssøknadPerioderFørSkjæringstidspunkt(Behandling revurdering, BehandlingReferanse behandlingReferanse) {
        if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
            Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(revurdering.getId());
            if (ytelseFordelingAggregat.isPresent() && ytelseFordelingAggregat.get().getOppgittFordeling() != null) {
                return ytelseFordelingAggregat.get().getOppgittFordeling().getOppgittePerioder().stream()
                    .anyMatch(oppgittPeriode -> oppgittPeriode.getFom().isBefore(behandlingReferanse.getUtledetSkjæringstidspunkt()));
            }
        }
        return false;
    }

    private void leggTilBehandlingsårsaker(Behandling behandling, Set<BehandlingÅrsakType> behandlingÅrsaker) {
        BehandlingÅrsak.Builder builder = BehandlingÅrsak.builder(new ArrayList<>(behandlingÅrsaker));
        behandling.getOriginalBehandling().ifPresent(builder::medOriginalBehandling);
        builder.buildFor(behandling);
    }

    private boolean erEtterkontrollRevurdering(Behandling revurdering) {
        Set<BehandlingÅrsakType> etterkontrollTyper = BehandlingÅrsakType.årsakerForEtterkontroll();
        return revurdering.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).anyMatch(etterkontrollTyper::contains);
    }

    private StartpunktType utledBehovForGRegulering(BehandlingReferanse ref, Behandling revurdering) {
        StartpunktType startpunkt = StartpunktType.UDEFINERT;
        Optional<Behandling> opprinneligBehandling = revurdering.getOriginalBehandling();
        if (!opprinneligBehandling.isPresent()) {
            throw new IllegalStateException("Revurdering skal ha en basisbehandling - skal ikke skje");
        }
        BeregningSats grunnbeløp = beregningsgrunnlagKopierOgLagreTjeneste.finnEksaktSats(BeregningSatsType.GRUNNBELØP, ref.getFørsteUttaksdato());
        Optional<BeregningsgrunnlagEntitet> forrigeBeregning = hentBeregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(opprinneligBehandling.get().getId());
        if (forrigeBeregning.isPresent()) {
            long satsIBeregning = forrigeBeregning.get().getGrunnbeløp().getVerdi().longValue();
            // Kan evt spisse: Sjekke om det finnes BGPeriode med avkortet = 6G. Må evt også ta hensyn til SN og 1/2G
            if (grunnbeløp.getVerdi() - satsIBeregning > 1) {
                LOGGER.info("Revurdering {} skal G-reguleres", revurdering.getId());// NOSONAR //$NON-NLS-1$
                startpunkt = StartpunktType.BEREGNING;
            } else if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
                final Set<AktivitetStatus> reberegnes = Set.of(AktivitetStatus.DAGPENGER, AktivitetStatus.ARBEIDSAVKLARINGSPENGER);
                final boolean skalReberegnes = forrigeBeregning.get().getAktivitetStatuser().stream()
                    .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
                    .anyMatch(reberegnes::contains);
                if (skalReberegnes) {
                    LOGGER.info("Revurdering {} skal Arena-reguleres", revurdering.getId());// NOSONAR //$NON-NLS-1$
                    startpunkt = StartpunktType.BEREGNING;
                }
            }
        } else {
            // Finnes ikke beregningsgrunnlag forrige.
            startpunkt = StartpunktType.BEREGNING;
        }
        return startpunkt;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        RyddRegisterData rydder = new RyddRegisterData(repositoryProvider, kontekst);
        rydder.ryddRegisterdata();
    }

    private void kopierResultaterAvhengigAvStartpunkt(Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        Behandling origBehandling = revurdering.getOriginalBehandling()
            .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        revurdering = kopierVilkår(origBehandling, revurdering, kontekst);
        revurdering = kopierUttaksperiodegrense(revurdering, origBehandling);

        if (StartpunktType.UTTAKSVILKÅR.equals(revurdering.getStartpunkt())) {
            beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(origBehandling.getId(), revurdering.getId());
        }

        tilbakestillOppgittFordelingBasertPåBehandlingType(revurdering);
    }

    private void tilbakestillOppgittFordelingBasertPåBehandlingType(Behandling revurdering) {
        YtelsesFordelingRepository ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
        Long behandlingId = revurdering.getId();
        Optional<YtelseFordelingAggregat> ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(behandlingId);
        ytelsesFordelingRepository.tilbakestillFordeling(behandlingId);
        if (!revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) && !mottatteDokumentTjeneste.harMottattDokumentSet(revurdering.getId(), DokumentTypeId.getEndringSøknadTyper())) {
            boolean erAnnenForelderInformert = false;
            if (ytelseFordelingAggregat.isPresent() && ytelseFordelingAggregat.get().getOppgittFordeling() != null) {
                erAnnenForelderInformert = ytelseFordelingAggregat.get().getOppgittFordeling().getErAnnenForelderInformert();
                if (unntaManuellRevurderingMedAvslåttFørstegangsbehandlingSomOriginalBehandling(revurdering)) {
                    return;
                }
            }
            ytelsesFordelingRepository.lagre(behandlingId, new OppgittFordelingEntitet(Collections.emptyList(), erAnnenForelderInformert));
        }
    }

    private boolean unntaManuellRevurderingMedAvslåttFørstegangsbehandlingSomOriginalBehandling(Behandling revurdering) {
        if (revurdering.erManueltOpprettet() && revurdering.getOriginalBehandling().isPresent() && !revurdering.getOriginalBehandling().get().erRevurdering()) {
            Optional<Behandlingsresultat> behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(revurdering.getOriginalBehandling().get().getId());
            return behandlingsresultat.isPresent() && behandlingsresultat.get().isBehandlingsresultatAvslått();
        }
        return false;
    }

    private Behandling kopierUttaksperiodegrense(Behandling revurdering, Behandling origBehandling) {
        // Kopier Uttaksperiodegrense - må alltid ha en søknadsfrist angitt
        Optional<Uttaksperiodegrense> funnetUttaksperiodegrense = uttakRepository.hentUttaksperiodegrenseHvisEksisterer(origBehandling.getId());
        if (funnetUttaksperiodegrense.isPresent()) {
            Uttaksperiodegrense origGrense = funnetUttaksperiodegrense.get();
            var behandlingsresultat = behandlingsresultatRepository.hent(revurdering.getId());
            Uttaksperiodegrense uttaksperiodegrense = new Uttaksperiodegrense.Builder(behandlingsresultat)
                .medFørsteLovligeUttaksdag(origGrense.getFørsteLovligeUttaksdag())
                .medMottattDato(origGrense.getMottattDato())
                .build();
            uttakRepository.lagreUttaksperiodegrense(revurdering.getId(), uttaksperiodegrense);
            return behandlingRepository.hentBehandling(revurdering.getId());
        }
        return revurdering;
    }

    private Behandling kopierVilkår(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        VilkårResultat vilkårResultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId()))
            .map(Behandlingsresultat::getVilkårResultat)
            .orElseThrow(() -> new IllegalStateException("VilkårResultat skal alltid være opprettet ved revurdering"));
        VilkårResultat.Builder vilkårBuilder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        StartpunktType startpunkt = revurdering.getStartpunkt();
        Set<VilkårType> vilkårtyperFørStartpunkt = StartpunktType.finnVilkårHåndtertInnenStartpunkt(startpunkt);
        Objects.requireNonNull(vilkårtyperFørStartpunkt, "Startpunkt " + startpunkt.getKode() +
            " støttes ikke for kopiering av vilkår ved revurdering");

        Behandlingsresultat originaltBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(origBehandling.getId())).orElseThrow();
        Set<Vilkår> vilkårFørStartpunkt = originaltBehandlingsresultat.getVilkårResultat().getVilkårene().stream()
            .filter(vilkår -> vilkårtyperFørStartpunkt.contains(vilkår.getVilkårType()))
            .collect(Collectors.toSet());
        kopierVilkår(vilkårBuilder, vilkårFørStartpunkt);
        vilkårBuilder.buildFor(revurdering);

        Behandlingsresultat revurderingBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId())).orElseThrow();
        behandlingRepository.lagre(revurderingBehandlingsresultat.getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());
        return behandlingRepository.hentBehandling(revurdering.getId());
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private void kopierVilkår(VilkårResultat.Builder vilkårBuilder, Set<Vilkår> vilkårne) {
        vilkårne
            .forEach(vilkår -> vilkårBuilder.leggTilVilkårResultat(vilkår.getVilkårType(), vilkår.getGjeldendeVilkårUtfall(), vilkår.getVilkårUtfallMerknad(),
                vilkår.getMerknadParametere(), vilkår.getAvslagsårsak(), vilkår.erManueltVurdert(), vilkår.erOverstyrt(), vilkår.getRegelEvaluering(),
                vilkår.getRegelInput()));
    }
}
