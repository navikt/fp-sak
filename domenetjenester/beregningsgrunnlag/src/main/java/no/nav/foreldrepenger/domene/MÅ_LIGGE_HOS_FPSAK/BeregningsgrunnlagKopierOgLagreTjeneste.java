package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK;

import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.FASTSATT;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.FASTSATT_INN;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.FORESLÅTT;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.FORESLÅTT_UT;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.KOFAKBER_UT;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING;
import static no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand.VURDERT_REFUSJON;

import java.time.LocalDate;
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
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulator.output.BeregningResultatAggregat;
import no.nav.folketrygdloven.kalkulator.steg.BeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.input.KalkulatorStegProsesseringInputTjeneste;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.mappers.fra_kalkulus.KalkulusTilBehandlingslagerMapper;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagTilstand;

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
            .map(KalkulusTilBehandlingslagerMapper::mapBeregningsgrunnlag)
            .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, FASTSATT);
    }

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        return beregningsgrunnlagRepository.finnEksaktSats(satsType, dato);
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat vurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        BeregningResultatAggregat beregningResultatAggregat =  beregningsgrunnlagTjeneste.vurderRefusjonskravForBeregninggrunnlag(
            kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
                behandlingId,
                input,
                BehandlingStegType.VURDER_REF_BERGRUNN)
        );
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(beregningResultatAggregat.getBeregningsgrunnlagGrunnlag());
        beregningsgrunnlagRepository.lagre(input.getKoblingReferanse().getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), VURDERT_REFUSJON);
        BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagVilkårOgAkjonspunktResultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
        beregningsgrunnlagVilkårOgAkjonspunktResultat.setVilkårOppfylt(getVilkårResultat(beregningResultatAggregat), getRegelEvalueringVilkårvurdering(beregningResultatAggregat), getRegelInputVilkårvurdering(beregningResultatAggregat));
        return beregningsgrunnlagVilkårOgAkjonspunktResultat;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fordelBeregningsgrunnlagUtenVilkårOgPeriodisering(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        FordelBeregningsgrunnlagInput fordelInput = (FordelBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG);
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.fordelBeregningsgrunnlagUtenPeriodisering(fordelInput);
        Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag = finnForrigeBgFraTilstand(input, FASTSATT_INN);
        BeregningsgrunnlagEntitet nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningResultatAggregat.getBeregningsgrunnlag());
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetBeregningsgrunnlag, nyttBg, OPPDATERT_MED_REFUSJON_OG_GRADERING, FASTSATT_INN);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        FordelBeregningsgrunnlagInput fordelInput = (FordelBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG);
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(fordelInput);
        Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag = finnForrigeBgFraTilstand(input, FASTSATT_INN);
        BeregningsgrunnlagEntitet nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningResultatAggregat.getBeregningsgrunnlag());
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetBeregningsgrunnlag, nyttBg, OPPDATERT_MED_REFUSJON_OG_GRADERING, FASTSATT_INN);
        BeregningsgrunnlagVilkårOgAkjonspunktResultat beregningsgrunnlagVilkårOgAkjonspunktResultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
        beregningsgrunnlagVilkårOgAkjonspunktResultat.setVilkårOppfylt(getVilkårResultat(beregningResultatAggregat), getRegelEvalueringVilkårvurdering(beregningResultatAggregat), getRegelInputVilkårvurdering(beregningResultatAggregat));
        return beregningsgrunnlagVilkårOgAkjonspunktResultat;
    }

    private boolean getVilkårResultat(BeregningResultatAggregat beregningResultatAggregat) {
        return beregningResultatAggregat.getBeregningVilkårResultat().getErVilkårOppfylt();
    }

    private String getRegelEvalueringVilkårvurdering(BeregningResultatAggregat beregningResultatAggregat) {
        return beregningResultatAggregat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0).getRegelEvalueringVilkårvurdering();
    }

    private String getRegelInputVilkårvurdering(BeregningResultatAggregat beregningResultatAggregat) {
        return beregningResultatAggregat.getBeregningsgrunnlag().getBeregningsgrunnlagPerioder().get(0).getRegelInputVilkårvurdering();
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat foreslåBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        ForeslåBeregningsgrunnlagInput foreslåBeregningsgrunnlagInput = (ForeslåBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId,
            input,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.foreslåBeregningsgrunnlag(
            foreslåBeregningsgrunnlagInput);
        Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag = finnForrigeBgFraTilstand(input, FORESLÅTT_UT);
        BeregningsgrunnlagEntitet nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningResultatAggregat.getBeregningsgrunnlag());
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetBeregningsgrunnlag, nyttBg, FORESLÅTT, FORESLÅTT_UT);
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
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(beregningResultatAggregat.getBeregningsgrunnlagGrunnlag());
        BeregningsgrunnlagEntitet nyttBg = nyttGrunnlag.getBeregningsgrunnlag().orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetGrunnlag, nyttGrunnlag, nyttBg);
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
                               Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag,
                               BeregningsgrunnlagEntitet nyttBg,
                               BeregningsgrunnlagTilstand tilstand, BeregningsgrunnlagTilstand bekreftetTilstand) {
        KoblingReferanse ref = input.getKoblingReferanse();
        Long behandlingId = ref.getKoblingId();
        boolean kanKopiereBekreftet = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningResultatAggregat.getBeregningAksjonspunktResultater(),
            nyttBg,
            finnForrigeBgFraTilstand(input, tilstand),
            forrigeBekreftetBeregningsgrunnlag
        );
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, tilstand);
        if (kanKopiereBekreftet) {
            forrigeBekreftetBeregningsgrunnlag.ifPresent(bg -> beregningsgrunnlagRepository.lagre(behandlingId, bg, bekreftetTilstand));
        }
    }

    private List<BeregningAksjonspunktResultat> lagreOgKopier(KoblingReferanse ref, BeregningResultatAggregat resultat) {
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(resultat.getBeregningsgrunnlagGrunnlag());
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
            forrigeBekreftetGrunnlag.ifPresent(gr -> beregningsgrunnlagRepository.lagre(ref.getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.oppdatere(gr), BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER));
        }
        return beregningAksjonspunktResultater;
    }

    private void lagreOgKopier(BeregningsgrunnlagInput input,
                               BeregningResultatAggregat beregningResultatAggregat,
                               Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag,
                               BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag,
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
            Optional<BeregningsgrunnlagGrunnlagBuilder> bekreftetGrunnlagBuilder = forrigeBekreftetGrunnlag
                .map(gr -> {
                        BeregningsgrunnlagGrunnlagBuilder b = BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag)
                            .medBeregningsgrunnlag(gr.getBeregningsgrunnlag().orElseThrow(() -> new IllegalStateException("Skal ha beregningsgrunnlag")));
                        gr.getRefusjonOverstyringer().ifPresent(b::medRefusjonOverstyring);
                        return b;
                    }
                );
            bekreftetGrunnlagBuilder
                .ifPresent(b -> beregningsgrunnlagRepository.lagre(behandlingId, b, KOFAKBER_UT));
        }
    }


}
