package no.nav.foreldrepenger.domene.prosess;

import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.BESTEBEREGNET;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FASTSATT;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FASTSATT_INN;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT_2;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT_2_UT;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.FORESLÅTT_UT;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.KOFAKBER_UT;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.VURDERT_REFUSJON;
import static no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand.VURDERT_VILKÅR;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.FaktaOmBeregningInput;
import no.nav.folketrygdloven.kalkulator.input.FordelBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeslåBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.ForeslåBesteberegningInput;
import no.nav.folketrygdloven.kalkulator.input.FortsettForeslåBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.VurderRefusjonBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.behandling.KoblingReferanse;
import no.nav.folketrygdloven.kalkulator.output.BeregningResultatAggregat;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingAggregat;
import no.nav.folketrygdloven.kalkulator.output.RegelSporingPeriode;
import no.nav.folketrygdloven.kalkulator.steg.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.kalkulus.kodeverk.BeregningsgrunnlagPeriodeRegelType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.entiteter.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.input.KalkulatorStegProsesseringInputTjeneste;
import no.nav.foreldrepenger.domene.mappers.BeregningAksjonspunktResultatMapper;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.BesteberegningMapper;
import no.nav.foreldrepenger.domene.mappers.fra_kalkulator_til_entitet.KalkulusTilBehandlingslagerMapper;
import no.nav.foreldrepenger.domene.modell.kodeverk.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.domene.output.BeregningsgrunnlagVilkårOgAkjonspunktResultat;

/**
 * Fasade tjeneste for å delegere alle kall fra steg
 */
@ApplicationScoped
public class BeregningsgrunnlagKopierOgLagreTjeneste {

    private static final String UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER = "Utvikler-feil: skal ha beregningsgrunnlag her";
    private static final Supplier<IllegalStateException> INGEN_BG_EXCEPTION_SUPPLIER = () -> new IllegalStateException(
        UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER);
    public static final Comparator<BeregningsgrunnlagTilstand> TILSTAND_COMPARATOR = (t1, t2) -> {
        if (t1.equals(t2)) {
            return 0;
        }
        return t1.erFør(t2) ? -1 : 1;
    };
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private final BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste = new BeregningsgrunnlagTjeneste();
    private KalkulatorStegProsesseringInputTjeneste kalkulatorStegProsesseringInputTjeneste;

    public BeregningsgrunnlagKopierOgLagreTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagKopierOgLagreTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                                   KalkulatorStegProsesseringInputTjeneste kalkulatorStegProsesseringInputTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.kalkulatorStegProsesseringInputTjeneste = kalkulatorStegProsesseringInputTjeneste;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fastsettBeregningsaktiviteter(BeregningsgrunnlagInput input) {
        var ref = input.getKoblingReferanse();
        var resultat = beregningsgrunnlagTjeneste.fastsettBeregningsaktiviteter(
            kalkulatorStegProsesseringInputTjeneste.lagStartInput(ref.getKoblingId(), input));
        return lagreOgKopier(ref, resultat);
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fastsettBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.fastsettBeregningsgrunnlag(
            kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(behandlingId, input,
                BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG));
        var beregningsgrunnlag = beregningResultatAggregat.getBeregningsgrunnlagGrunnlag()
            .getBeregningsgrunnlagHvisFinnes()
            .map(beregningsgrunnlagFraKalkulus -> KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(
                beregningsgrunnlagFraKalkulus,
                beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
                beregningResultatAggregat.getRegelSporingAggregat()))
            .orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, FASTSATT);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(Collections.emptyList());
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat vurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.vurderRefusjonskravForBeregninggrunnlag(
            (VurderRefusjonBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(behandlingId, input,
                BehandlingStegType.VURDER_REF_BERGRUNN));
        var nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag(),
            beregningResultatAggregat.getRegelSporingAggregat());
        beregningsgrunnlagRepository.lagre(input.getKoblingReferanse().getKoblingId(),
            BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), VURDERT_REFUSJON);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat vurderVilkårBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.vurderBeregningsgrunnlagvilkår(
            kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(behandlingId, input,
                BehandlingStegType.VURDER_VILKAR_BERGRUNN));
        var nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag(),
            beregningResultatAggregat.getRegelSporingAggregat());
        beregningsgrunnlagRepository.lagre(input.getKoblingReferanse().getKoblingId(),
            BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), VURDERT_VILKÅR);
        var beregningsgrunnlagVilkårOgAkjonspunktResultat = new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
        var vilkårSporing = finnRegelSporingVilkårVurdering(beregningResultatAggregat);
        beregningsgrunnlagVilkårOgAkjonspunktResultat.setVilkårOppfylt(beregningResultatAggregat.getBeregningVilkårResultat().getErVilkårOppfylt(),
            vilkårSporing.map(RegelSporingPeriode::regelEvaluering).orElse(null),
            vilkårSporing.map(RegelSporingPeriode::regelInput).orElse(null),
            vilkårSporing.map(RegelSporingPeriode::regelVersjon).orElse(null));
        return beregningsgrunnlagVilkårOgAkjonspunktResultat;
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var fordelInput = (FordelBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId, input, BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG);
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(fordelInput);
        var nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(
            beregningResultatAggregat.getBeregningsgrunnlag(),
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
            beregningResultatAggregat.getRegelSporingAggregat());
        lagreOgKopier(input, beregningResultatAggregat, nyttBg, OPPDATERT_MED_REFUSJON_OG_GRADERING, FASTSATT_INN);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    private Optional<RegelSporingPeriode> finnRegelSporingVilkårVurdering(BeregningResultatAggregat beregningResultatAggregat) {
        var regelSporingPerioder = beregningResultatAggregat.getRegelSporingAggregat()
            .map(RegelSporingAggregat::regelsporingPerioder)
            .orElse(Collections.emptyList());
        if (regelSporingPerioder.isEmpty()) {
            return Optional.empty();
        }
        var førstePeriode = beregningResultatAggregat.getBeregningsgrunnlag()
            .getBeregningsgrunnlagPerioder()
            .get(0)
            .getPeriode();
        return regelSporingPerioder.stream()
            .filter(p -> p.periode().overlapper(førstePeriode) && p.regelType()
                .equals(BeregningsgrunnlagPeriodeRegelType.VILKÅR_VURDERING))
            .findFirst();
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat foreslåBesteberegning(BeregningsgrunnlagInput input) {

        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var foreslåBeregningsgrunnlagInput = (ForeslåBesteberegningInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId, input, BehandlingStegType.FORESLÅ_BESTEBEREGNING);
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.foreslåBesteberegning(
            foreslåBeregningsgrunnlagInput);
        var nyttBg = BesteberegningMapper.mapBeregningsgrunnlagMedBesteberegning(
            beregningResultatAggregat.getBeregningsgrunnlag(),
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
            beregningResultatAggregat.getRegelSporingAggregat(),
            beregningResultatAggregat.getBesteberegningVurderingGrunnlag());

        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, BESTEBEREGNET);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat foreslåBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var foreslåBeregningsgrunnlagInput = (ForeslåBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId, input, BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG);
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.foreslåBeregningsgrunnlag(
            foreslåBeregningsgrunnlagInput);
        var nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(
            beregningResultatAggregat.getBeregningsgrunnlag(),
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
            beregningResultatAggregat.getRegelSporingAggregat());
        lagreOgKopier(input, beregningResultatAggregat, nyttBg, FORESLÅTT, FORESLÅTT_UT);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat fortsettForeslåBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var foreslåBeregningsgrunnlag2Input = (FortsettForeslåBeregningsgrunnlagInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId, input, BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG);
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.fortsettForeslåBeregningsgrunnlag(
            foreslåBeregningsgrunnlag2Input);
        var nyttBg = KalkulusTilBehandlingslagerMapper.mapBeregningsgrunnlag(
            beregningResultatAggregat.getBeregningsgrunnlag(),
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag().getFaktaAggregat(),
            beregningResultatAggregat.getRegelSporingAggregat());
        lagreOgKopier(input, beregningResultatAggregat, nyttBg, FORESLÅTT_2, FORESLÅTT_2_UT);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    public BeregningsgrunnlagVilkårOgAkjonspunktResultat kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var behandlingId = input.getKoblingReferanse().getKoblingId();
        var faktaOmBeregningInput = (FaktaOmBeregningInput) kalkulatorStegProsesseringInputTjeneste.lagFortsettInput(
            behandlingId, input, BehandlingStegType.KONTROLLER_FAKTA_BEREGNING);
        var beregningResultatAggregat = beregningsgrunnlagTjeneste.kontrollerFaktaBeregningsgrunnlag(
            faktaOmBeregningInput);
        var nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(
            beregningResultatAggregat.getBeregningsgrunnlagGrunnlag(),
            beregningResultatAggregat.getRegelSporingAggregat());
        var nyttBg = nyttGrunnlag.getBeregningsgrunnlag().orElseThrow(INGEN_BG_EXCEPTION_SUPPLIER);
        lagreOgKopier(input, beregningResultatAggregat, nyttBg);
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    public void kopierBeregningsresultatFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        kopierBeregningsgrunnlagFraStartTilOgMedTilstand(originalBehandlingId, behandlingId, FASTSATT);
    }

    private void kopierBeregningsgrunnlagFraStartTilOgMedTilstand(Long originalBehandlingId,
                                                                  Long behandlingId,
                                                                  BeregningsgrunnlagTilstand gjeldendeTilstand) {
        Stream.of(BeregningsgrunnlagTilstand.values())
            .filter(tilstand -> tilstand.erFør(gjeldendeTilstand) || tilstand.equals(gjeldendeTilstand))
            .sorted(TILSTAND_COMPARATOR)
            .forEach(
                tilstand -> beregningsgrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId,
                    behandlingId, tilstand));
    }

    public void kopierResultatForGRegulering(Long originalBehandlingId, Long behandlingId, LocalDate førsteUttaksdato) {
        Stream.of(BeregningsgrunnlagTilstand.values())
            .filter(tilstand -> tilstand.erFør(KOFAKBER_UT) || tilstand.equals(KOFAKBER_UT))
            .sorted(TILSTAND_COMPARATOR)
            .forEach(tilstand -> beregningsgrunnlagRepository.oppdaterGrunnlagMedGrunnbeløp(originalBehandlingId,
                behandlingId, tilstand, førsteUttaksdato));
    }

    /**
     * Kun til test, overgang til ft-kalkulus.
     */
    public void lagreBeregningsgrunnlag(Long behandlingId,
                                        BeregningsgrunnlagEntitet beregningsgrunnlag,
                                        BeregningsgrunnlagTilstand tilstand) {
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, tilstand);
    }

    private Optional<BeregningsgrunnlagEntitet> finnForrigeBgFraTilstand(BeregningsgrunnlagInput input,
                                                                         BeregningsgrunnlagTilstand tilstandFraSteg) {
        var behandlingReferanse = input.getKoblingReferanse();
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(
            behandlingReferanse.getKoblingId(), behandlingReferanse.getOriginalKoblingId(), tilstandFraSteg)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
    }

    private void lagreOgKopier(BeregningsgrunnlagInput input,
                               BeregningResultatAggregat beregningResultatAggregat,
                               BeregningsgrunnlagEntitet nyttBg,
                               BeregningsgrunnlagTilstand stegtilstand,
                               BeregningsgrunnlagTilstand bekreftetTilstand) {
        var ref = input.getKoblingReferanse();
        var behandlingId = ref.getKoblingId();
        var forrigeStegGrunnlag = finnForrigeBgFraTilstand(input, stegtilstand);
        var forrigeBekreftetGrunnlag = finnForrigeBekreftetGrunnlag(input, forrigeStegGrunnlag, bekreftetTilstand);
        var kanKopiereBekreftet = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater(), nyttBg, forrigeStegGrunnlag,
            forrigeBekreftetGrunnlag);
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, stegtilstand);
        if (kanKopiereBekreftet) {
            forrigeBekreftetGrunnlag.map(BeregningsgrunnlagEntitet::new)
                .ifPresent(bg -> beregningsgrunnlagRepository.lagre(behandlingId, bg, bekreftetTilstand));
        }
    }

    private Optional<BeregningsgrunnlagEntitet> finnForrigeBekreftetGrunnlag(BeregningsgrunnlagInput input,
                                                                             Optional<BeregningsgrunnlagEntitet> forrigeStegGrunnlag,
                                                                             BeregningsgrunnlagTilstand tilstand) {
        if (forrigeStegGrunnlag.isEmpty()) {
            return Optional.empty();
        }
        var behandlingId = input.getKoblingId();
        return beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetOpprettetEtter(behandlingId,
            forrigeStegGrunnlag.get().getOpprettetTidspunkt(), tilstand)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
    }

    private BeregningsgrunnlagVilkårOgAkjonspunktResultat lagreOgKopier(KoblingReferanse ref,
                                                                        BeregningResultatAggregat resultat) {
        var nyttGrunnlag = KalkulusTilBehandlingslagerMapper.mapGrunnlag(resultat.getBeregningsgrunnlagGrunnlag(),
            resultat.getRegelSporingAggregat());
        var forrigeBekreftetGrunnlag = beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(
            ref.getKoblingId(), BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        var beregningAksjonspunktResultater = resultat.getBeregningAvklaringsbehovResultater();
        var kanKopiereGrunnlag = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningAksjonspunktResultater, nyttGrunnlag,
            beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitet(ref.getKoblingId(),
                BeregningsgrunnlagTilstand.OPPRETTET), forrigeBekreftetGrunnlag);
        beregningsgrunnlagRepository.lagre(ref.getKoblingId(),
            BeregningsgrunnlagGrunnlagBuilder.oppdatere(nyttGrunnlag), BeregningsgrunnlagTilstand.OPPRETTET);
        if (kanKopiereGrunnlag) {
            forrigeBekreftetGrunnlag.ifPresent(
                gr -> beregningsgrunnlagRepository.lagre(ref.getKoblingId(), BeregningsgrunnlagGrunnlagBuilder.kopi(gr),
                    BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER));
        }
        return new BeregningsgrunnlagVilkårOgAkjonspunktResultat(
            resultat.getBeregningAvklaringsbehovResultater().stream().map(BeregningAksjonspunktResultatMapper::map).toList());
    }

    private void lagreOgKopier(BeregningsgrunnlagInput input,
                               BeregningResultatAggregat beregningResultatAggregat,
                               BeregningsgrunnlagEntitet nyttBg) {
        var ref = input.getKoblingReferanse();
        var behandlingId = ref.getKoblingId();
        var forrigeGrunnlagFraSteg = finnForrigeBgFraTilstand(input, OPPDATERT_MED_ANDELER);
        Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeBekreftetGrunnlag = forrigeGrunnlagFraSteg.isPresent()
            ? beregningsgrunnlagRepository.hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlingerEtterTidspunkt(ref.getKoblingId(), ref.getOriginalKoblingId(),
            forrigeGrunnlagFraSteg.get().getOpprettetTidspunkt(), KOFAKBER_UT)
            : Optional.empty();
        var kanKopiereFraBekreftet = KopierBeregningsgrunnlag.kanKopiereFraForrigeBekreftetGrunnlag(
            beregningResultatAggregat.getBeregningAvklaringsbehovResultater(), nyttBg,
            forrigeGrunnlagFraSteg,
            forrigeBekreftetGrunnlag.flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag));
        beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, OPPDATERT_MED_ANDELER);
        if (kanKopiereFraBekreftet) {
            beregningsgrunnlagRepository.lagre(behandlingId,
                BeregningsgrunnlagGrunnlagBuilder.kopi(forrigeBekreftetGrunnlag), KOFAKBER_UT);
        }
    }


}
