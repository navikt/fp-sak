package no.nav.foreldrepenger.domene.rest;

import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.folketrygdloven.kalkulator.avklaringsbehov.AvklarAktiviteterHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.BeregningFaktaOgOverstyringHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBGTidsbegrensetArbeidsforholdHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.FordelBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.VurderVarigEndretEllerNyoppstartetHåndterer;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.AvklarteAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.BeregningsaktivitetLagreDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.avklaringsbehov.refusjon.VurderRefusjonBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulus.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.domene.input.KalkulatorHåndteringInputTjeneste;
import no.nav.foreldrepenger.domene.prosess.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.rest.dto.VurderVarigEndringEllerNyoppstartetSNDto;

/**
 * Fasadetjeneste for håndtering av aksjonspunkt
 */
@ApplicationScoped
public class BeregningHåndterer {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste;

    public BeregningHåndterer() {
        // CDI
    }

    @Inject
    public BeregningHåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                              KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.kalkulatorHåndteringInputTjeneste = kalkulatorHåndteringInputTjeneste;
    }

    public void håndterAvklarAktiviteter(BeregningsgrunnlagInput input, AvklarteAktiviteterDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input, AksjonspunktDefinisjon.AVKLAR_AKTIVITETER);
        var resultatFraKalkulus = AvklarAktiviteterHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBGTidsbegrensetArbeidsforhold(BeregningsgrunnlagInput input,
                                                             FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
        var resultatFraKalkulus = FastsettBGTidsbegrensetArbeidsforholdHåndterer.håndter(håndterBeregningsgrunnlagInput,
            dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBeregningsgrunnlagATFL(BeregningsgrunnlagInput input,
                                                      FastsettBeregningsgrunnlagATFLDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS);
        var resultatFraKalkulus = FastsettBeregningsgrunnlagATFLHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterBeregningAktivitetOverstyring(BeregningsgrunnlagInput input,
                                                     List<BeregningsaktivitetLagreDto> overstyrAktiviteter) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER);
        var grunnlag = AvklarAktiviteterHåndterer.håndterOverstyring(overstyrAktiviteter,
            håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), grunnlag);
    }

    public void håndterBeregningsgrunnlagOverstyring(BeregningsgrunnlagInput input,
                                                     OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG);
        var resultat = BeregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(håndterBeregningsgrunnlagInput,
            overstyrBeregningsgrunnlagDto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBruttoForSNNyIArbeidslivet(BeregningsgrunnlagInput input,
                                                          FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET);
        var resultat = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer.oppdater(
            håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFordelBeregningsgrunnlag(BeregningsgrunnlagInput input, FordelBeregningsgrunnlagDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
        var resultat = FordelBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input,
                                                        VurderRefusjonBeregningsgrunnlagDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input, AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN);
        var resultat = VurderRefusjonBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }


    public void håndterVurderFaktaOmBeregning(BeregningsgrunnlagInput input, FaktaBeregningLagreDto dto) {
        var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
            input.getKoblingReferanse().getKoblingId(), input,
            AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN);
        var resultat = BeregningFaktaOgOverstyringHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderVarigEndretNyoppstartetSN(BeregningsgrunnlagInput input,
                                                       VurderVarigEndringEllerNyoppstartetSNDto dto) {
        if (dto.getErVarigEndretNaering()) {
            var bruttoSn = Objects.requireNonNull(dto.getBruttoBeregningsgrunnlag(), "næringsinntekt");
            var håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(
                input.getKoblingReferanse().getKoblingId(), input,
                AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE);
            var resultat = VurderVarigEndretEllerNyoppstartetHåndterer.håndter(håndterBeregningsgrunnlagInput, bruttoSn, AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE);
            beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
        }
    }

    private Long getBehandlingId(BeregningsgrunnlagInput input) {
        return input.getKoblingReferanse().getKoblingId();
    }

}
