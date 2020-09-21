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
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.kalkulator.output.BeregningResultatAggregat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
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
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningTilInputTjeneste beregningTilInputTjeneste;

    public BeregningsgrunnlagKopierOgLagreTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagKopierOgLagreTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                                   BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                   BeregningTilInputTjeneste beregningTilInputTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningTilInputTjeneste = beregningTilInputTjeneste;
    }

    public List<BeregningAksjonspunktResultat> fastsettBeregningsaktiviteter(BeregningsgrunnlagInput input) {
        var ref = input.getKoblingReferanse();
        BeregningResultatAggregat resultat = beregningsgrunnlagTjeneste.fastsettBeregningsaktiviteter(beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input));
        return lagreOgKopier(ref, resultat);
    }

    public void fastsettBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.fastsettBeregningsgrunnlag(beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input));
        Long behandlingId = input.getKoblingReferanse().getKoblingId();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningResultatAggregat.getBeregningsgrunnlagGrunnlag()
            .getBeregningsgrunnlag()
            .map(KalkulusTilBehandlingslagerMapper::mapBeregningsgrunnlag)
            .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, FASTSATT);
    }

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        return beregningsgrunnlagRepository.finnEksaktSats(satsType, dato);
    }

    public List<BeregningAksjonspunktResultat> vurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        BeregningResultatAggregat resultat = beregningsgrunnlagTjeneste.vurderRefusjonskravForBeregninggrunnlag(beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input));
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(resultat.getBeregningsgrunnlagGrunnlag());
        beregningsgrunnlagRepository.lagre(input.getKoblingReferanse().getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), VURDERT_REFUSJON);
        return resultat.getBeregningAksjonspunktResultater();
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input));
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
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.foreslåBeregningsgrunnlag(beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input));
        Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag = finnForrigeBgFraTilstand(input, FORESLÅTT_UT);
        BeregningsgrunnlagEntitet nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(beregningResultatAggregat.getBeregningsgrunnlag());
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetBeregningsgrunnlag, nyttBg, FORESLÅTT, FORESLÅTT_UT);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(beregningResultatAggregat.getBeregningAksjonspunktResultater());
    }

    public RyddBeregningsgrunnlag getRyddBeregningsgrunnlag(BehandlingskontrollKontekst kontekst) {
        return new RyddBeregningsgrunnlag(beregningsgrunnlagRepository, kontekst);
    }

    public List<BeregningAksjonspunktResultat> kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        BeregningResultatAggregat beregningResultatAggregat = beregningsgrunnlagTjeneste.kontrollerFaktaBeregningsgrunnlag(beregningTilInputTjeneste.lagInputMedVerdierFraBeregning(input));
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag = finnForrigeGrunnlagFraTilstand(input, KOFAKBER_UT);
        BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(beregningResultatAggregat.getBeregningsgrunnlagGrunnlag());
        BeregningsgrunnlagEntitet nyttBg = nyttGrunnlag.getBeregningsgrunnlag().orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        lagreOgKopier(input, beregningResultatAggregat, forrigeBekreftetGrunnlag, nyttGrunnlag, nyttBg);
        return beregningResultatAggregat.getBeregningAksjonspunktResultater();
    }

    public void kopierBeregningsresultatFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        beregningsgrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, behandlingId, FASTSATT);
    }

    public void kopierResultatForGRegulering(Long originalBehandlingId, Long behandlingId) {
        beregningsgrunnlagRepository.kopierGrunnlagForGRegulering(originalBehandlingId, behandlingId);
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
