package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.BESTEBEREGNET;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.FASTSATT;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.FASTSATT_INN;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.FORESLÅTT;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.FORESLÅTT_UT;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.KOFAKBER_UT;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING;
import static no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand.VURDERT_REFUSJON;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.FaktaOmBeregningInput;
import no.nav.folketrygdloven.kalkulator.input.FordelBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeslåBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeslåBesteberegningInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulator.output.BeregningResultatAggregat;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingAggregat;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingPeriode;
import no.nav.folketrygdloven.kalkulator.steg.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulator.steg.besteberegning.BesteberegningResultat;
import no.nav.folketrygdloven.kalkulator.tid.Intervall;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.KalkulatorStegProsesseringInputTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus.BesteberegningMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagTilstand;

/**
 * Fasade tjeneste for å delegere alle kall fra steg
 */
@ApplicationScoped
public class BeregningsgrunnlagKopierOgLagreTjeneste {

    private static final String UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER = "Utvikler-feil: skal ha beregningsgrunnlag her";
    private static final Supplier<IllegalStateException> INGEN_BG_EXCEPTION_SUPPLIER = () -> new IllegalStateException(UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER);
    public static final Comparator<BeregningsgrunnlagTilstand> TILSTAND_COMPARATOR = (t1, t2) -> {
        if (t1.equals(t2)) {
            return 0;
        }
        return t1.erFør(t2) ? -1 : 1;
    };
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private KalkulatorStegProsesseringInputTjeneste kalkulatorStegProsesseringInputTjeneste;

    public BeregningsgrunnlagKopierOgLagreTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagKopierOgLagreTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                                   BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   KalkulatorStegProsesseringInputTjeneste kalkulatorStegProsesseringInputTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.kalkulatorStegProsesseringInputTjeneste = kalkulatorStegProsesseringInputTjeneste;
    }

    public List<BeregningAksjonspunktResultat> fastsettBeregningsaktiviteter(BeregningsgrunnlagInput input) {
        var ref = input.getKoblingReferanse();
        BeregningResultatAggregat resultat = beregningsgrunnlagTjeneste.fastsettBeregningsaktiviteter(
            kalkulatorStegProsesseringInputTjeneste.lagStartInput(
                ref.getKoblingId(),
                input)
        );
        return lagreOgKopier(ref, resultat);
    }

    public void fastsettBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.fastsettBeregningsgrunnlag(
            kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
                behandlingId,
                input,
                BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG)
        );
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningResultatAggregat.getBeregningsgrunnlagGrunnlag()
            .getBeregningsgrunnlag()
            .map(beregningsgrunnlagFraKalkulus -> KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(
                beregningsgrunnlagFraKalkulus,
                beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
                beregningResultatAggregat.getRegelSporingAggregat()))
            .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, FASTSATT);
    }

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        return beregningsgrunnlagRepository.finnEksaktSats(satsType, dato);
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat vurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.vurderRefusjonskravForBeregninggrunnlag(
            kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
                behandlingId,
                input,
                BehandlingStegType.VURDER_REF_BERGRUNN)
        );
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(beregningResultatAggregat.getBeregningsgrunnlagGrunnlag(), beregningResultatAggregat.getRegelSporingAggregat());
        beregningsgrunnlagRepository.lagre(input.getKoblingReferanse().getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), VURDERT_REFUSJON);
        BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagVilkårOgAkjonspunktResultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
        beregningsgrunnlagVilkårOgAkjonspunktResultat.setVilkårOppfylt(getVilkårResultat(beregningResultatAggregat), getRegelEvalueringVilkårvurdering(beregningResultatAggregat), getRegelInputVilkårvurdering(beregningResultatAggregat));
        return beregningsgrunnlagVilkårOgAkjonspunktResultat;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        FordelBeregningsgrunnlagInput fordelInput = (FordelBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG);
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(fordelInput);
        BeregningsgrunnlagEntitet nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningResultatAggregat.getBeregningsgrunnlag(), beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(), beregningResultatAggregat.getRegelSporingAggregat());
        lagreOgKopier(input, beregningResultatAggregat, nyttBg, OPPDATERT_MED_REFUSJON_OG_GRADERING, FASTSATT_INN);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
    }

    private boolean getVilkårResultat(BeregningResultatAggregat beregningResultatAggregat) {
        return beregningResultatAggregat.getBeregningVilkårResultat().getErVilkårOppfylt();
    }

    private String getRegelEvalueringVilkårvurdering(BeregningResultatAggregat beregningResultatAggregat) {
        Optional<RegelSporingPeriode> regelSporing = finnRegelSporingVilkårVurdering(beregningResultatAggregat);
        return regelSporing.map(RegelSporingPeriode::getRegelEvaluering).orElse(null);
    }

    private String getRegelInputVilkårvurdering(BeregningResultatAggregat beregningResultatAggregat) {
        Optional<RegelSporingPeriode> regelSporing = finnRegelSporingVilkårVurdering(beregningResultatAggregat);
        return regelSporing.map(RegelSporingPeriode::getRegelInput).orElse(null);
    }

    private Optional<RegelSporingPeriode> finnRegelSporingVilkårVurdering(BeregningResultatAggregat beregningResultatAggregat) {
        List<RegelSporingPeriode> regelSporingPerioder = beregningResultatAggregat.getRegelSporingAggregat()
            .map(RegelSporingAggregat::getRegelsporingPerioder).orElse(Collections.emptyList());
        if (regelSporingPerioder.isEmpty()) {
            return Optional.empty();
        }
        Intervall førstePeriode = beregningResultatAggregat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0).getPeriode();
        return regelSporingPerioder.stream()
            .filter(p -> p.getPeriode().overlapper(førstePeriode) && p.getRegelType().equals(BeregningsgrunnlagPeriodeRegelType.VILKÅR_VURDERING))
            .findFirst();
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat foreslåBesteberegning(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        ForeslåBesteberegningInput foreslåBeregningsgrunnlagInput = (ForeslåBesteberegningInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.FORESLÅ_BESTEBEREGNING);
        BesteberegningResultat beregningResultatAggregat = beregningsgrunnlagTjeneste.foreslåBesteberegning(foreslåBeregningsgrunnlagInput);
        BeregningsgrunnlagEntitet nyttBg = BesteberegningMapper.mapBeregningsgrunnlagMedBesteberegning(
            beregningResultatAggregat.getBeregningsgrunnlag(),
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
            beregningResultatAggregat.getRegelSporingAggregat(),
            beregningResultatAggregat.getBesteberegningVurderingGrunnlag());
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, BESTEBEREGNET);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
    }


    public BeregningsgrunnlagVilkårOgAkjonspunktResultat foreslåBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        ForeslåBeregningsgrunnlagInput foreslåBeregningsgrunnlagInput = (ForeslåBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.foreslåBeregningsgrunnlag(
            foreslåBeregningsgrunnlagInput);
        BeregningsgrunnlagEntitet nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningResultatAggregat.getBeregningsgrunnlag(), beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(), beregningResultatAggregat.getRegelSporingAggregat());
        lagreOgKopier(input, beregningResultatAggregat, nyttBg, FORESLÅTT, FORESLÅTT_UT);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
    }

    public RyddBeregningsgrunnlag getRyddBeregningsgrunnlag(BehandlingskontrollKontekst kontekst) {
        return new RyddBeregningsgrunnlag(beregningsgrunnlagRepository, kontekst);
    }

    public List<BeregningAksjonspunktResultat> kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        FaktaOmBeregningInput faktaOmBeregningInput = (FaktaOmBeregningInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING);
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.kontrollerFaktaBeregningsgrunnlag(faktaOmBeregningInput);
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag = finnForrigeGrunnlagFraTilstand(input, KOFAKBER_UT);
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(beregningResultatAggregat.getBeregningsgrunnlagGrunnlag(), beregningResultatAggregat.getRegelSporingAggregat());
        BeregningsgrunnlagEntitet nyttBg = nyttGrunnlag.getBeregningsgrunnlag().orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetGrunnlag, nyttBg);
        return beregningResultatAggregat.getBeregningAksjonspunktResultater();
    }

    public void kopierBeregningsresultatFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        kopierBeregningsgrunnlagFraStartTilOgMedTilstand(originalBehandlingId, behandlingId, FASTSATT);
    }

    private void kopierBeregningsgrunnlagFraStartTilOgMedTilstand(Long originalBehandlingId, Long behandlingId, BeregningsgrunnlagTilstand gjeldendeTilstand) {
        Stream.of(BeregningsgrunnlagTilstand.values())
            .filter(tilstand -> tilstand.erFør(gjeldendeTilstand) || tilstand.equals(gjeldendeTilstand))
            .sorted(TILSTAND_COMPARATOR)
            .forEach(tilstand -> beregningsgrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, behandlingId, tilstand));
    }

    public void kopierResultatForGRegulering(Long originalBehandlingId, Long behandlingId) {
        Stream.of(BeregningsgrunnlagTilstand.values())
            .filter(tilstand -> tilstand.erFør(KOFAKBER_UT) || tilstand.equals(KOFAKBER_UT))
            .sorted(TILSTAND_COMPARATOR)
            .forEach(tilstand -> beregningsgrunnlagRepository.oppdaterGrunnlagMedGrunnbeløp(originalBehandlingId, behandlingId, tilstand));
    }

    /**
     * Kun til test, overgang til ft-kalkulus.
     */
    public void lagreBeregningsgrunnlag(Long behandlingId, BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningsgrunnlagTilstand tilstand) {
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, tilstand);
    }

    private Optional<BeregningsgrunnlagEntitet> finnForrigeBgFraTilstand(BeregningsgrunnlagInput input, BeregningsgrunnlagTilstand tilstandFraSteg) {
        KoblingReferanse behandlingReferanse = input.getKoblingReferanse();
        return beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingReferanse.getKoblingId(), behandlingReferanse.getOriginalKoblingId(), tilstandFraSteg)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
    }

    private Optional<BeregningsgrunnlagGrunnlagEntitet> finnForrigeGrunnlagFraTilstand(BeregningsgrunnlagInput input, BeregningsgrunnlagTilstand tilstandFraSteg) {
        KoblingReferanse referanse = input.getKoblingReferanse();
        return beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(referanse.getKoblingId(), referanse.getOriginalKoblingId(), tilstandFraSteg);
    }

    private void lagreOgKopier(BeregningsgrunnlagInput input,
                               BeregningResultatAggregat beregningResultatAggregat,
                               BeregningsgrunnlagEntitet nyttBg,
                               BeregningsgrunnlagTilstand stegtilstand, BeregningsgrunnlagTilstand bekreftetTilstand) {
        KoblingReferanse ref = input.getKoblingReferanse();
        Long behandlingId = ref.getKoblingId();
        Optional<BeregningsgrunnlagEntitet> forrigeStegGrunnlag = finnForrigeBgFraTilstand(input, stegtilstand);
        Optional<BeregningsgrunnlagEntitet> forrigeBekreftetGrunnlag = finnForrigeBekreftetGrunnlag(input, forrigeStegGrunnlag, bekreftetTilstand);
        boolean kanKopiereBekreftet = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningResultatAggregat.getBeregningAksjonspunktResultater(),
            nyttBg,
            forrigeStegGrunnlag,
            forrigeBekreftetGrunnlag
        );
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, stegtilstand);
        if (kanKopiereBekreftet) {
            forrigeBekreftetGrunnlag
                .map(BeregningsgrunnlagEntitet::new).ifPresent(bg -> beregningsgrunnlagRepository.lagre(behandlingId, bg, bekreftetTilstand));
        }
    }

    private Optional<BeregningsgrunnlagEntitet> finnForrigeBekreftetGrunnlag(BeregningsgrunnlagInput input,
                                                                             Optional<BeregningsgrunnlagEntitet> forrigeStegGrunnlag,
                                                                             BeregningsgrunnlagTilstand tilstand) {
        if (forrigeStegGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        Long behandlingId = input.getKoblingId();
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetOpprettetEtter(behandlingId, forrigeStegGrunnlag.get().getOpprettetTidspunkt(), tilstand)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
    }

    private List<BeregningAksjonspunktResultat> lagreOgKopier(KoblingReferanse ref, BeregningResultatAggregat resultat) {
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(resultat.getBeregningsgrunnlagGrunnlag(), resultat.getRegelSporingAggregat());
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(
            ref.getKoblingId(),
            BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        List<BeregningAksjonspunktResultat> beregningAksjonspunktResultater = resultat.getBeregningAksjonspunktResultater();
        boolean kanKopiereGrunnlag = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningAksjonspunktResultater,
            nyttGrunnlag,
            beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId(), BeregningsgrunnlagTilstand.OPPRETTET),
            forrigeBekreftetGrunnlag
        );
        beregningsgrunnlagRepository.lagre(ref.getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), BeregningsgrunnlagTilstand.OPPRETTET);
        if (kanKopiereGrunnlag) {
            forrigeBekreftetGrunnlag.ifPresent(gr -> beregningsgrunnlagRepository.lagre(ref.getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.kopi(gr), BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER));
        }
        return beregningAksjonspunktResultater;
    }

    private void lagreOgKopier(BeregningsgrunnlagInput input,
                               BeregningResultatAggregat beregningResultatAggregat,
                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag,
                               BeregningsgrunnlagEntitet nyttBg) {
        KoblingReferanse ref = input.getKoblingReferanse();
        Long behandlingId = ref.getKoblingId();
        boolean kanKopiereFraBekreftet = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningResultatAggregat.getBeregningAksjonspunktResultater(),
            nyttBg,
            finnForrigeBgFraTilstand(input, OPPDATERT_MED_ANDELER),
            forrigeBekreftetGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, OPPDATERT_MED_ANDELER);
        if (kanKopiereFraBekreftet) {
            beregningsgrunnlagRepository.lagre(behandlingId, BeregningsgrunnlagGrunnlagBuilder.kopi(forrigeBekreftetGrunnlag), KOFAKBER_UT);
        }
    }


}
