package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.SatsRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.YtelseFordelingAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittFordelingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.OppgittPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.modell.FaktaAggregat;
import no.nav.foreldrepenger.domene.modell.FaktaAktør;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.modell.typer.FaktaVurdering;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.BehandlingÅrsakTjeneste;
import no.nav.foreldrepenger.domene.registerinnhenting.StartpunktTjeneste;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.KopierForeldrepengerUttaktjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.overganger.UtsettelseCore2021;

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
    private StartpunktTjeneste startpunktTjeneste;
    private BehandlingÅrsakTjeneste behandlingÅrsakTjeneste;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningTjeneste beregningTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;
    private KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste;
    private OpptjeningRepository opptjeningRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private SatsRepository satsRepository;

    KontrollerFaktaRevurderingStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaRevurderingStegImpl(BehandlingRepositoryProvider repositoryProvider,
                                       BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) KontrollerFaktaTjeneste tjeneste,
                                       @FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER) StartpunktTjeneste startpunktTjeneste,
                                       BehandlingÅrsakTjeneste behandlingÅrsakTjeneste,
                                       BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                       MottatteDokumentTjeneste mottatteDokumentTjeneste, BeregningTjeneste beregningTjeneste,
                                       ForeldrepengerUttakTjeneste uttakTjeneste,
                                       KopierForeldrepengerUttaktjeneste kopierForeldrepengerUttaktjeneste,
                                       DekningsgradTjeneste dekningsgradTjeneste, FagsakRelasjonTjeneste fagsakRelasjonTjeneste,
                                       SatsRepository satsRepository) {
        this.repositoryProvider = repositoryProvider;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.tjeneste = tjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.startpunktTjeneste = startpunktTjeneste;
        this.behandlingÅrsakTjeneste = behandlingÅrsakTjeneste;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.mottatteDokumentTjeneste = mottatteDokumentTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.kopierForeldrepengerUttaktjeneste = kopierForeldrepengerUttaktjeneste;
        this.opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.satsRepository = satsRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);

        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        // Spesialhåndtering for enkelte behandlinger
        if (SpesialBehandling.erSpesialBehandling(behandling)) {
            var startpunkt = SpesialBehandling.skalUttakVurderes(behandling) ? StartpunktType.UTTAKSVILKÅR : StartpunktType.TILKJENT_YTELSE;
            behandling.setStartpunkt(startpunkt);
            kopierResultaterAvhengigAvStartpunkt(behandling, kontekst, skjæringstidspunkter);
            return utledStegResultat(startpunkt, List.of());
        }

        behandlingÅrsakTjeneste.lagHistorikkForRegisterEndringerMotOriginalBehandling(behandling);

        var startpunkt = utledStartpunkt(ref, skjæringstidspunkter, behandling);
        behandling.setStartpunkt(startpunkt);

        // Kopier aksjonspunkter
        List<AksjonspunktResultat> aksjonspunktResultater = startpunkt.getRangering() <= StartpunktType.OPPTJENING.getRangering() ?
            tjeneste.utledAksjonspunkterTilHøyreForStartpunkt(ref, skjæringstidspunkter, startpunkt) : List.of();
        kopierResultaterAvhengigAvStartpunkt(behandling, kontekst, skjæringstidspunkter);

        return utledStegResultat(startpunkt, aksjonspunktResultater);
    }

    private boolean erDekningsgradEndring(BehandlingReferanse ref) {
        var dekningsgrad = dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref);
        var fagsakrelDekningsgrad = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer())
            .getDekningsgrad();
        return !Objects.equals(dekningsgrad, fagsakrelDekningsgrad);
    }

    private BehandleStegResultat utledStegResultat(StartpunktType startpunkt, List<AksjonspunktResultat> aksjonspunkt) {
        var transisjon = TransisjonIdentifikator
            .forId(FellesTransisjoner.SPOLFREM_PREFIX + startpunkt.getBehandlingSteg().getKode());
        return BehandleStegResultat.fremoverførtMedAksjonspunktResultater(transisjon, aksjonspunkt);
    }

    private StartpunktType utledStartpunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Behandling revurdering) {
        var startpunkt = initieltStartPunkt(ref, stp, revurdering);
        startpunkt = sjekkÅpneAksjonspunkt(ref, revurdering, startpunkt);

        if (startpunkt.getRangering() > StartpunktType.DEKNINGSGRAD.getRangering() && erDekningsgradEndring(ref)) {
            startpunkt = StartpunktType.DEKNINGSGRAD;
        }

        // Undersøk behov for GRegulering. Med mindre vi allerede skal til BEREGNING eller tidligere steg
        if (startpunkt.getRangering() > StartpunktType.BEREGNING.getRangering()) {
            var greguleringStartpunkt = utledBehovForGRegulering(ref, stp, revurdering);
            startpunkt = startpunkt.getRangering() < greguleringStartpunkt.getRangering() ? startpunkt : greguleringStartpunkt;
        }

        // Startpunkt for revurdering kan kun hoppe fremover; default dersom startpunkt passert
        if (behandlingskontrollTjeneste.erStegPassert(revurdering, startpunkt.getBehandlingSteg())) {
            startpunkt = DEFAULT_STARTPUNKT;
        }
        LOG.info("KOFAKREV Revurdering {} har fått fastsatt startpunkt {} ", revurdering.getId(), startpunkt.getKode());
        return startpunkt;
    }

    private StartpunktType initieltStartPunkt(BehandlingReferanse ref, Skjæringstidspunkt stp, Behandling revurdering) {
        if (revurdering.erManueltOpprettet() || erEtterkontrollRevurdering(revurdering)) {
            return DEFAULT_STARTPUNKT;
        }
        // Automatisk revurdering skal hoppe til utledet startpunkt. Unntaket er revurdering av avslåtte behandlinger
        var orgBehandlingsresultat = getBehandlingsresultat(ref.getOriginalBehandlingId().orElseThrow());
        if (orgBehandlingsresultat == null || orgBehandlingsresultat.isVilkårAvslått()) {
            return DEFAULT_STARTPUNKT;
        }
        // Revurdering av innvilget behandling. Hvis vilkår er avslått må man tillate re-evalueres
        var startpunkt = startpunktTjeneste.utledStartpunktMotOriginalBehandling(ref, stp);
        if (startpunkt.equals(StartpunktType.UDEFINERT)) {
            startpunkt = inneholderEndringssøknadPerioderFørSkjæringstidspunkt(revurdering, stp)
                ? StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP : StartpunktType.UTTAKSVILKÅR;
        }
        return startpunkt;
    }

    private StartpunktType sjekkÅpneAksjonspunkt(BehandlingReferanse ref, Behandling revurdering, StartpunktType gjeldendeStartpunkt) {
        var stegForÅpneAksjonspunktFørStartpunkt = revurdering.getÅpneAksjonspunkter().stream()
            .map(Aksjonspunkt::getAksjonspunktDefinisjon)
            .map(AksjonspunktDefinisjon::getBehandlingSteg)
            .filter(behandlingSteg -> sammenlignRekkefølge(ref, gjeldendeStartpunkt, behandlingSteg) > 0)
            .toList();
        if (stegForÅpneAksjonspunktFørStartpunkt.isEmpty()) {
            return gjeldendeStartpunkt;
        }
        return Arrays.stream(StartpunktType.values())
            .filter(stp -> !StartpunktType.UDEFINERT.equals(stp))
            .filter(stp -> stp.getRangering() >= DEFAULT_STARTPUNKT.getRangering()) // Se bort fra helt tidlige startpunkt her i KOFAK
            .filter(stp -> stegForÅpneAksjonspunktFørStartpunkt.stream().allMatch(steg -> sammenlignRekkefølge(ref, stp, steg) <= 0))
            .max(Comparator.comparing(StartpunktType::getRangering))
            .orElse(gjeldendeStartpunkt);
    }

    private int sammenlignRekkefølge(BehandlingReferanse ref, StartpunktType startpunkt, BehandlingStegType behandlingSteg) {
        return behandlingskontrollTjeneste.sammenlignRekkefølge(ref.fagsakYtelseType(), ref.behandlingType(),
            startpunkt.getBehandlingSteg(), behandlingSteg);
    }

    private boolean inneholderEndringssøknadPerioderFørSkjæringstidspunkt(Behandling revurdering, Skjæringstidspunkt stp) {
        if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)) {
            var ytelsesFordelingRepository = repositoryProvider.getYtelsesFordelingRepository();
            var oppgittPerioder = ytelsesFordelingRepository.hentAggregatHvisEksisterer(revurdering.getId())
                    .map(YtelseFordelingAggregat::getOppgittFordeling)
                    .map(OppgittFordelingEntitet::getPerioder).orElse(Collections.emptyList());
            var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
            return oppgittPerioder.stream()
                .filter(p -> UtsettelseCore2021.kreverSammenhengendeUttak(p) || frittUttakErPeriodeMedUttak(p))
                .anyMatch(oppgittPeriode -> oppgittPeriode.getFom().isBefore(skjæringstidspunkt));
        }
        return false;
    }

    private static boolean frittUttakErPeriodeMedUttak(OppgittPeriodeEntitet periode) {
        return !(periode.isUtsettelse() || periode.isOpphold());
    }

    private boolean erEtterkontrollRevurdering(Behandling revurdering) {
        var etterkontrollTyper = BehandlingÅrsakType.årsakerForEtterkontroll();
        return revurdering.getBehandlingÅrsaker().stream().map(BehandlingÅrsak::getBehandlingÅrsakType).anyMatch(etterkontrollTyper::contains);
    }

    private StartpunktType utledBehovForGRegulering(BehandlingReferanse ref, Skjæringstidspunkt stp, Behandling revurdering) {
        var opprinneligBehandlingId = revurdering.getOriginalBehandlingId()
                .orElseThrow(() -> new IllegalStateException("Revurdering skal ha en basisbehandling - skal ikke skje"));
        var opprinneligRef = BehandlingReferanse.fra(behandlingRepository.hentBehandling(opprinneligBehandlingId));
        var forrigeBeregning = beregningTjeneste.hent(opprinneligRef).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);

        if (forrigeBeregning.isEmpty()) {
            return StartpunktType.BEREGNING;
        }

        if (revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_SATS_REGULERING)) {
            return finnStartpunktForGRegulering(revurdering);
        }

        var grunnbeløp = satsRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, stp.getFørsteUttaksdatoGrunnbeløp());
        long satsIBeregning = forrigeBeregning.map(Beregningsgrunnlag::getGrunnbeløp).map(Beløp::getVerdi).map(BigDecimal::longValue)
                .orElse(0L);

        if (grunnbeløp.getVerdi() - satsIBeregning > 1) {
            var bruttoPrÅr = forrigeBeregning.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
                .orElse(Collections.emptyList())
                .stream()
                .map(BeregningsgrunnlagPeriode::getBruttoPrÅr)
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO);
            var avkortingOver = BeregningsresultatRepository.avkortingMultiplikatorG();
            var grenseAvkorting = new BigDecimal(satsIBeregning * avkortingOver);
            var militærUnder = BeregningsresultatRepository.militærMultiplikatorG();
            var grenseMilitær = new BigDecimal(grunnbeløp.getVerdi() * militærUnder);
            var over6G = bruttoPrÅr.compareTo(grenseAvkorting) >= 0;
            var erMilitær = forrigeBeregning.stream().flatMap(bg -> bg.getAktivitetStatuser().stream())
                .anyMatch(as -> as.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL));
            var erMilitærUnderGrense = erMilitær && forrigeBeregning.stream()
                .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
                .anyMatch(p -> p.getBruttoPrÅr().compareTo(grenseMilitær) < 0);
            var erNæringsdrivende = forrigeBeregning.stream()
                .flatMap(bg -> bg.getAktivitetStatuser().stream())
                .map(BeregningsgrunnlagAktivitetStatus::getAktivitetStatus)
                .anyMatch(SN_REGULERING::contains);
            if (over6G || erMilitærUnderGrense || erNæringsdrivende) {
                LOG.info("KOFAKREV Revurdering {} skal G-reguleres", revurdering.getId());
                return finnStartpunktForGRegulering(revurdering);
            }
            LOG.info("KOFAKREV Revurdering {} blir ikke G-regulert: brutto {} grense {}", revurdering.getId(), bruttoPrÅr, grenseAvkorting);
        }
        return StartpunktType.UDEFINERT;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var rydder = new RyddRegisterData(repositoryProvider, kontekst);
        rydder.ryddRegisterdataStartpunktRevurdering();
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
        var gr = beregningTjeneste.hent(BehandlingReferanse.fra(revurdering));
        if (mottarYtelseForAktivitet(gr) || harBesteberegning(gr)) {
            return StartpunktType.BEREGNING;
        }
        return StartpunktType.BEREGNING_FORESLÅ;
    }

    private boolean harBesteberegning(Optional<BeregningsgrunnlagGrunnlag> gr) {
        var bgPerioder = gr.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder)
            .orElse(List.of());
        return bgPerioder.stream().anyMatch(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream().anyMatch(a -> a.getBesteberegnetPrÅr() != null));
    }

    private boolean mottarYtelseForAktivitet(Optional<BeregningsgrunnlagGrunnlag> gr) {
        var mottarYtelseFL = gr.flatMap(BeregningsgrunnlagGrunnlag::getFaktaAggregat)
            .flatMap(FaktaAggregat::getFaktaAktør)
            .map(FaktaAktør::getHarFLMottattYtelse)
            .map(FaktaVurdering::getVurdering)
            .orElse(false);
        var mottarYtelseAT = gr.flatMap(BeregningsgrunnlagGrunnlag::getFaktaAggregat)
            .map(FaktaAggregat::getFaktaArbeidsforhold)
            .orElse(List.of())
            .stream()
            .anyMatch(a -> Boolean.TRUE.equals(a.getHarMottattYtelseVurdering()));
        return mottarYtelseFL || mottarYtelseAT;
    }

    private void kopierResultaterAvhengigAvStartpunkt(Behandling revurdering,
                                                      BehandlingskontrollKontekst kontekst,
                                                      Skjæringstidspunkt stp) {
        var origBehandling = revurdering.getOriginalBehandlingId().map(behandlingRepository::hentBehandling)
                .orElseThrow(() -> new IllegalStateException("Original behandling mangler på revurdering - skal ikke skje"));

        revurdering = kopierVilkårFørStartpunkt(origBehandling, revurdering, kontekst);
        // Skal være kopiert ved opprettelse av revurdering for å få tak i riktig STP.
        // Kan ha blitt nullstilt i denne revurderingen ved tilbakehopp til KOARB (fx pga IM).
        kopierOpptjeningVedBehov(origBehandling, revurdering);

        if (StartpunktType.BEREGNING_FORESLÅ.equals(revurdering.getStartpunkt())) {
            beregningsgrunnlagKopierOgLagreTjeneste.kopierResultatForGRegulering(finnBehandlingSomHarKjørtBeregning(origBehandling).getId(),
                    revurdering.getId(), stp.getFørsteUttaksdatoGrunnbeløp());
        }

        if (StartpunktType.UTTAKSVILKÅR.equals(revurdering.getStartpunkt()) || StartpunktType.TILKJENT_YTELSE.equals(revurdering.getStartpunkt())) {
            beregningTjeneste.kopier(BehandlingReferanse.fra(revurdering), BehandlingReferanse.fra(origBehandling), BeregningsgrunnlagTilstand.FASTSATT);
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
        if (!behandling.erRevurdering() || beregningTjeneste.hent(BehandlingReferanse.fra(behandling)).isPresent()) {
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
                var ytelseFordelingAggregat = ytelsesFordelingRepository.hentAggregat(revurdering.getId());
                var erAnnenForelderInformert = ytelseFordelingAggregat.getOppgittFordeling().getErAnnenForelderInformert();
                var ønskerJustertVedFødsel = ytelseFordelingAggregat.getOppgittFordeling().ønskerJustertVedFødsel();
                var tilbakeStiltFordeling = new OppgittFordelingEntitet(Collections.emptyList(), erAnnenForelderInformert, ønskerJustertVedFødsel);
                var yfBuilder = ytelsesFordelingRepository.opprettBuilder(revurdering.getId())
                    .medOppgittFordeling(tilbakeStiltFordeling);
                ytelsesFordelingRepository.lagre(revurdering.getId(), yfBuilder.build());
            }
        }
    }

    private boolean harUttak(Long behandlingId) {
        return uttakTjeneste.hentHvisEksisterer(behandlingId).isPresent();
    }

    private boolean erEndringssøknad(Behandling revurdering) {
        return revurdering.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)
                || mottatteDokumentTjeneste.harMottattDokumentSet(revurdering.getId(),
                        DokumentTypeId.getEndringSøknadTyper());
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
            .filter(v -> v.getVilkårType().erInngangsvilkår())
            .collect(Collectors.toSet());
        kopierVilkårFørStartpunkt(vilkårBuilder, vilkårFørStartpunkt);
        vilkårBuilder.fjernVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE);
        vilkårBuilder.buildFor(revurdering);

        var revurderingBehandlingsresultat = Optional.ofNullable(getBehandlingsresultat(revurdering.getId())).orElseThrow();
        behandlingRepository.lagre(revurderingBehandlingsresultat.getVilkårResultat(), kontekst.getSkriveLås());
        behandlingRepository.lagre(revurdering, kontekst.getSkriveLås());
        return behandlingRepository.hentBehandling(revurdering.getId());
    }

    private void kopierOpptjeningVedBehov(Behandling origBehandling, Behandling revurdering) {
        if (opptjeningRepository.finnOpptjening(origBehandling.getId()).isPresent() && opptjeningRepository.finnOpptjening(revurdering.getId()).isEmpty()) {
            opptjeningRepository.kopierGrunnlagFraEksisterendeBehandling(origBehandling, revurdering);
        }
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }

    private void kopierVilkårFørStartpunkt(VilkårResultat.Builder vilkårBuilder, Set<Vilkår> vilkårne) {
        vilkårne.forEach(vilkår -> vilkårBuilder.kopierVilkårFraAnnenBehandling(vilkår, false, false));
    }
}
