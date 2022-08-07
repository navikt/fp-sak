package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.uttak.Uttaksperiodegrense;
import no.nav.foreldrepenger.behandlingslager.uttak.UttaksperiodegrenseRepository;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(BehandlingStegType.KONTROLLER_FAKTA)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
class KontrollerFaktaRevurderingStegImpl implements KontrollerFaktaSteg {
    private static final Logger LOG = LoggerFactory.getLogger(KontrollerFaktaRevurderingStegImpl.class);

    private static final StartpunktType DEFAULT_STARTPUNKT = StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;

    private static final Set<AktivitetStatus> SN_REGULERING = Set.of(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE, AktivitetStatus.KOMBINERT_AT_SN,
            AktivitetStatus.KOMBINERT_FL_SN, AktivitetStatus.KOMBINERT_AT_FL_SN);

    private BehandlingRepository behandlingRepository;
    private KontrollerFaktaTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste;
    private UttaksperiodegrenseRepository uttaksperiodegrenseRepository;
    private StartpunktTjeneste startpunktTjeneste;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste;

    KontrollerFaktaRevurderingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaRevurderingStegImpl(BehandlingRepositoryProvider repositoryProvider,
            BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
            HentOgLagreBeregningsgrunnlagTjeneste hentBeregningsgrunnlagTjeneste,
            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) KontrollerFaktaTjeneste tjeneste,
            @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) StartpunktTjeneste startpunktTjeneste,
            BehandlingÅrsakTjeneste behandlingÅrsakTjeneste,
            BehandlingskontrollTjeneste behandlingskontrollTjeneste,
            MottatteDokumentTjeneste mottatteDokumentTjeneste,
            ForeldrepengerUttakTjeneste uttakTjeneste,
            KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste) {
        this.repositoryProvider = repositoryProvider;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tjeneste = tjeneste;
        this.hentBeregningsgrunnlagTjeneste = hentBeregningsgrunnlagTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.uttaksperiodegrenseRepository = repositoryProvider.getUttaksperiodegrenseRepository();
        this.startpunktTjeneste = startpunktTjeneste;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.kopierForeldrepengerUttaktjeneste = kopierForeldrepengerUttaktjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.harSattStartpunkt()) {
            // Startpunkt kan bare initieres én gang, og det gjøres i dette steget.
            // Suksessive eksekveringer av stegets aksjonspunktsutledere skjer utenfor steget
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        // Spesialhåndtering for enkelte behandlinger
        if (SpesialBehandling.erSpesialBehandling(behandling)) {
            var startpunkt = SpesialBehandling.skalUttakVurderes(behandling) ?
                StartpunktType.UTTAKSVILKÅR : StartpunktType.TILKJENT_YTELSE;
            behandling.setStartpunkt(startpunkt);
            kopierResultaterAvhengigAvStartpunkt(behandling, kontekst, ref);
            return utledStegResultat(startpunkt, List.of());
        }

        behandlingÅrsakTjeneste.lagHistorikkForRegisterEndringerMotOriginalBehandling(behandling);

        var startpunkt = utledStartpunkt(ref, behandling);
        behandling.setStartpunkt(startpunkt);

        // Kopier aksjonspunkter
        List<AksjonspunktResultat> aksjonspunktResultater = startpunkt.getRangering() <= StartpunktType.OPPTJENING.getRangering() ?
            tjeneste.utledAksjonspunkterTilHøyreForStartpunkt(ref, startpunkt) : List.of();
        kopierResultaterAvhengigAvStartpunkt(behandling, kontekst, ref);

        return utledStegResultat(startpunkt, aksjonspunktResultater);
    }

    private BehandleStegResultat utledStegResultat(StartpunktType startpunkt, List<AksjonspunktResultat> aksjonspunkt) {
        var transisjon = TransisjonIdentifikator
            .forId(FellesTransisjoner.SPOLFREM_PREFIX + startpunkt.getBehandlingSteg().getKode());
        return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(transisjon, aksjonspunkt);
    }

    private StartpunktType utledStartpunkt(BehandlingReferanse ref, Behandling revurdering) {
        var startpunkt = DEFAULT_STARTPUNKT; // Gjennomgå hele prosessen - for manuelle, etterkontroller og tidl.avslag
        if (!revurdering.erManueltOpprettet() && !erEtterkontrollRevurdering(revurdering)) {
            // Automatisk revurdering skal hoppe til utledet startpunkt. Unntaket er revurdering av avslåtte behandlinger
            var orgBehandlingsresultat = getBehandlingsresultat(ref.getOriginalBehandlingId().get());
            if ((orgBehandlingsresultat != null) && !orgBehandlingsresultat.isVilkårAvslått()) {
                // Revurdering av innvilget behandling. Hvis vilkår er avslått må man tillate re-evalueres
                startpunkt = startpunktTjeneste.utledStartpunktMotOriginalBehandling(ref);
                if (startpunkt.equals(StartpunktType.UDEFINERT)) {
                    startpunkt = inneholderEndringssøknadPerioderFørSkjæringstidspunkt(revurdering, ref)
                            ? StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP
                            : StartpunktType.UTTAKSVILKÅR;
                }
            }
        }

        // Undersøk behov for GRegulering. Med mindre vi allerede skal til BEREGNING eller tidligere steg
        if (startpunkt.getRangering() > StartpunktType.BEREGNING.getRangering()) {
            var greguleringStartpunkt = utledBehovForGRegulering(ref, revurdering);
            startpunkt = startpunkt.getRangering() < greguleringStartpunkt.getRangering() ? startpunkt : greguleringStartpunkt;
        }

        // Startpunkt for revurdering kan kun hoppe fremover; default dersom startpunkt passert
        if (behandlingskontrollTjeneste.erStegPassert(revurdering, startpunkt.getBehandlingSteg())) {
            startpunkt = DEFAULT_STARTPUNKT;
        }
        LOG.info("KOFAKREV Revurdering {} har fått fastsatt startpunkt {} ", revurdering.getId(), startpunkt.getKode());// NOSONAR //$NON-NLS-1$
        return startpunkt;
    }

    private boolean inneholderEndringssøknadPerioderFørSkjæringstidspunkt(Behandling revurdering, BehandlingReferanse behandlingReferanse) {
        if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
            var oppgittPerioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(revurdering.getId())
                    .map(YtelseFordelingAggregat::getOppgittFordeling)
                    .map(OppgittFordelingEntitet::getOppgittePerioder).orElse(Collections.emptyList());
            var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
            return oppgittPerioder.stream()
                    .anyMatch(oppgittPeriode -> oppgittPeriode.getFom().isBefore(skjæringstidspunkt));
        }
        return false;
    }

    private boolean erEtterkontrollRevurdering(Behandling revurdering) {
        var etterkontrollTyper = BehandlingÅrsakType.årsakerForEtterkontroll();
        return revurdering.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).anyMatch(etterkontrollTyper::contains);
    }

    private StartpunktType utledBehovForGRegulering(BehandlingReferanse ref, Behandling revurdering) {
        var opprinneligBehandlingId = revurdering.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Revurdering skal ha en basisbehandling - skal ikke skje"));
        var forrigeBeregning = hentBeregningsgrunnlagTjeneste
                .hentBeregningsgrunnlagEntitetForBehandling(opprinneligBehandlingId);

        if (forrigeBeregning.isEmpty()) {
            return StartpunktType.BEREGNING;
        }

        if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
            return finnStartpunktForGRegulering(revurdering);
        }

        var grunnbeløp = beregningsgrunnlagKopierOgLagreTjeneste.finnEksaktSats(BeregningSatsType.GRUNNBELØP, ref.getSkjæringstidspunkt().getFørsteUttaksdatoGrunnbeløp());
        long satsIBeregning = forrigeBeregning.map(BeregningsgrunnlagEntitet::getGrunnbeløp).map(Beløp::getVerdi).map(BigDecimal::longValue)
                .orElse(0L);

        if ((grunnbeløp.getVerdi() - satsIBeregning) > 1) {
            var bruttoPrÅr = forrigeBeregning.map(BeregningsgrunnlagEntitet::getBeregningsgrunnlagPerioder).orElse(Collections.emptyList())
                    .stream()
                    .map(BeregningsgrunnlagPeriode::getBruttoPrÅr)
                    .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
            var multiplikator = BeregningsresultatRepository.avkortingMultiplikatorG(grunnbeløp.getPeriode().getFomDato().minusDays(1));
            var grenseverdi = new BigDecimal(satsIBeregning * multiplikator);
            var over6G = bruttoPrÅr.compareTo(grenseverdi) >= 0;
            var erMilitærUnder3G = forrigeBeregning.stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
                    .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                    .anyMatch(a -> a.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL)
                            && (a.getBeregningsgrunnlagPeriode().getBruttoPrÅr()
                                    .compareTo(BigDecimal.valueOf(3).multiply(BigDecimal.valueOf(grunnbeløp.getVerdi()))) < 0));
            var erNæringsdrivende = forrigeBeregning.stream().flatMap(bg -> bg.getAktivitetStatuser().stream())
                    .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
                    .anyMatch(SN_REGULERING::contains);
            if (over6G || erMilitærUnder3G || erNæringsdrivende) {
                LOG.info("KOFAKREV Revurdering {} skal G-reguleres", revurdering.getId());
                return finnStartpunktForGRegulering(revurdering);
            }
            LOG.info("KOFAKREV Revurdering {} blir ikke G-regulert: brutto {} grense {}", revurdering.getId(), bruttoPrÅr, grenseverdi);
        }
        return StartpunktType.UDEFINERT;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var rydder = new RyddRegisterData(repositoryProvider, kontekst);
        rydder.ryddRegisterdata();
    }

    /**
     * Om saken skal G-reguleres kan den enten starte fra start eller foreslå.
     * Dette avhenger av om man har avklart informasjon i fakta om beregning som avhenger av størrelsen på G-beløpet.
     * Dette gjelder søkere som har mottatt ytelse for en aktivitet (arbeidsforhold uten inntektsmelding eller frilans) og
     * der denne ytelsen har blitt g-regulert. Derfor lar vi alle slike saker starte fra start av beregning.
     *
     * @param revurdering Revurdering
     * @return Startpunkt for g-regulering
     */
    private StartpunktType finnStartpunktForGRegulering(Behandling revurdering) {
        var beregningsgrunnlagEntitet = hentBeregningsgrunnlagTjeneste
                .hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId());
        if (mottarYtelseForAktivitet(beregningsgrunnlagEntitet) || harBesteberegning(beregningsgrunnlagEntitet)) {
            return StartpunktType.BEREGNING;
        }
        return StartpunktType.BEREGNING_FORESLÅ;
    }

    private boolean harBesteberegning(Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagEntitet) {
        return beregningsgrunnlagEntitet.stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
                .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .anyMatch(a -> a.getBesteberegningPrÅr() != null);
    }

    private boolean mottarYtelseForAktivitet(Optional<BeregningsgrunnlagEntitet> beregningsgrunnlagEntitet) {
        return beregningsgrunnlagEntitet.stream().flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
                .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
                .anyMatch(a -> a.mottarYtelse().orElse(false));
    }

    private void kopierResultaterAvhengigAvStartpunkt(Behandling revurdering,
                                                      BehandlingskontrollKontekst kontekst,
                                                      BehandlingReferanse ref) {
        var origBehandling = revurdering.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        revurdering = kopierVilkårFørStartpunkt(origBehandling, revurdering, kontekst);
        revurdering = kopierUttaksperiodegrense(revurdering, origBehandling);

        if (StartpunktType.BEREGNING_FORESLÅ.equals(revurdering.getStartpunkt())) {
            beregningsgrunnlagKopierOgLagreTjeneste.kopierResultatForGRegulering(finnBehandlingSomHarKjørtBeregning(origBehandling).getId(),
                    revurdering.getId(), ref.getSkjæringstidspunkt().getFørsteUttaksdatoGrunnbeløp());
        }

        if (StartpunktType.UTTAKSVILKÅR.equals(revurdering.getStartpunkt()) || StartpunktType.TILKJENT_YTELSE.equals(revurdering.getStartpunkt())) {
            beregningsgrunnlagKopierOgLagreTjeneste.kopierBeregningsresultatFraOriginalBehandling(origBehandling.getId(), revurdering.getId());
        }

        if (StartpunktType.TILKJENT_YTELSE.equals(revurdering.getStartpunkt())) {
            if (SpesialBehandling.erOppsagtUttak(revurdering)) {
                kopierForeldrepengerUttaktjeneste.lagreTomtUttakResultat(revurdering.getId());
            } else {
                kopierForeldrepengerUttaktjeneste.kopierUttakFraOriginalBehandling(origBehandling.getId(), revurdering.getId());
            }
        } else {
            tilbakestillOppgittFordelingBasertPåBehandlingType(revurdering);
        }
    }

    private Behandling finnBehandlingSomHarKjørtBeregning(Behandling behandling) {
        if (!behandling.erRevurdering() || hentBeregningsgrunnlagTjeneste
                .hentSisteBeregningsgrunnlagGrunnlagEntitet(behandling.getId(), BeregningsgrunnlagTilstand.OPPRETTET).isPresent()) {
            return behandling;
        }
        return finnBehandlingSomHarKjørtBeregning(behandling.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("Forventer å finne original behandling")));
    }

    private void tilbakestillOppgittFordelingBasertPåBehandlingType(Behandling revurdering) {
        if (!erEndringssøknad(revurdering)) {
            var originalBehandling = revurdering.getOriginalBehandlingId().orElseThrow();
            // Hvis original behandling har vært innom uttak så skal periodene fra uttaket brukes for å lage ny YF
            // Se FastsettUttaksgrunnlagOgVurderSøknadsfristSteg
            if (harUttak(originalBehandling)) {
                var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
                var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregatHvisEksisterer(revurdering.getId());
                var erAnnenForelderInformert = hentAnnenForelderErInformert(ytelseFordelingAggregat);
                var tilbakeStiltFordeling = new OppgittFordelingEntitet(Collections.emptyList(), erAnnenForelderInformert);
                var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
                    .medOppgittFordeling(tilbakeStiltFordeling);
                ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());
            }
        }
    }

    private boolean harUttak(Long behandlingId) {
        return uttakTjeneste.hentUttakHvisEksisterer(behandlingId).isPresent();
    }

    private boolean hentAnnenForelderErInformert(Optional<YtelseFordelingAggregat> ytelseFordelingAggregat) {
        if (ytelseFordelingAggregat.isPresent() && (ytelseFordelingAggregat.get().getOppgittFordeling() != null)) {
            return ytelseFordelingAggregat.get().getOppgittFordeling().getErAnnenForelderInformert();
        }
        // Default false
        return false;
    }

    private boolean erEndringssøknad(Behandling revurdering) {
        return revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                || mottatteDokumentTjeneste.harMottattDokumentSet(revurdering.getId(),
                        DokumentTypeId.getEndringSøknadTyper());
    }

    private Behandling kopierUttaksperiodegrense(Behandling revurdering, Behandling origBehandling) {
        // Kopier Uttaksperiodegrense - må alltid ha en søknadsfrist angitt
        var funnetUttaksperiodegrense = uttaksperiodegrenseRepository.hentHvisEksisterer(origBehandling.getId());
        if (funnetUttaksperiodegrense.isPresent()) {
            var origGrense = funnetUttaksperiodegrense.get();
            var uttaksperiodegrense = new Uttaksperiodegrense(origGrense.getMottattDato());
            uttaksperiodegrenseRepository.lagre(revurdering.getId(), uttaksperiodegrense);
            return behandlingRepository.hentBehandling(revurdering.getId());
        }
        return revurdering;
    }

    private Behandling kopierVilkårFørStartpunkt(Behandling origBehandling, Behandling revurdering, BehandlingskontrollKontekst kontekst) {
        var vilkårResultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId()))
                .map(Behandlingsresultat::getVilkårResultat)
                .orElseThrow(() -> new IllegalStateException("VilkårResultat skal alltid være opprettet ved revurdering"));
        var vilkårBuilder = VilkårResultat.builderFraEksisterende(vilkårResultat);

        var startpunkt = revurdering.getStartpunkt();
        var vilkårtyperFørStartpunkt = StartpunktType.finnVilkårHåndtertInnenStartpunkt(startpunkt);
        Objects.requireNonNull(vilkårtyperFørStartpunkt, "Startpunkt " + startpunkt.getKode() +
                " støttes ikke for kopiering av vilkår ved revurdering");

        var originaltBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(origBehandling.getId())).orElseThrow();
        var vilkårFørStartpunkt = originaltBehandlingsresultat.getVilkårResultat().getVilkårene().stream()
                .filter(vilkår -> vilkårtyperFørStartpunkt.contains(vilkår.getVilkårType()))
                .collect(Collectors.toSet());
        kopierVilkårFørStartpunkt(vilkårBuilder, vilkårFørStartpunkt);
        vilkårBuilder.buildFor(revurdering);

        var revurderingBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId())).orElseThrow();
        behandlingRepository.lagre(revurderingBehandlingsresultat.getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());
        return behandlingRepository.hentBehandling(revurdering.getId());
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private void kopierVilkårFørStartpunkt(VilkårResultat.Builder vilkårBuilder, Set<Vilkår> vilkårne) {
        vilkårne.forEach(vilkår -> vilkårBuilder.kopierVilkårFraAnnenBehandling(vilkår, false, false));
    }
}
