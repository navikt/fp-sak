package no.nav.foreldrepenger.domene.rest;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.aksjonspunkt.AvklarAktiviteterHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.BeregningFaktaOgOverstyringHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.FastsettBGTidsbegrensetArbeidsforholdHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.FordelBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.VurderVarigEndretNyoppstartetSNHåndterer;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.AvklarteAktiviteterDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.BeregningsaktivitetLagreDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.FastsettBGTidsbegrensetArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.FastsettBeregningsgrunnlagATFLDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.OverstyrBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.VurderRefusjonBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.dto.VurderVarigEndringEllerNyoppstartetSNDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.fordeling.FordelBeregningsgrunnlagDto;
import no.nav.folketrygdloven.kalkulator.aksjonspunkt.refusjon.VurderRefusjonBeregningsgrunnlagHåndterer;
import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.input.HåndterBeregningsgrunnlagInput;
import no.nav.folketrygdloven.kalkulator.modell.beregningsgrunnlag.BeregningsgrunnlagGrunnlagDto;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.input.KalkulatorHåndteringInputTjeneste;

/**
 * Fasadetjeneste for håndtering av aksjonspunkt
 */
@ApplicationScoped
public class BeregningHåndterer {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer;
    private KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste;

    public BeregningHåndterer() {
        // CDI
    }

    @Inject
    public BeregningHåndterer(HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                              BeregningFaktaOgOverstyringHåndterer beregningFaktaOgOverstyringHåndterer,
                              KalkulatorHåndteringInputTjeneste kalkulatorHåndteringInputTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningFaktaOgOverstyringHåndterer = beregningFaktaOgOverstyringHåndterer;
        this.kalkulatorHåndteringInputTjeneste = kalkulatorHåndteringInputTjeneste;
    }

    public void håndterAvklarAktiviteter(BeregningsgrunnlagInput input, AvklarteAktiviteterDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE);
        BeregningsgrunnlagGrunnlagDto resultatFraKalkulus = AvklarAktiviteterHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBGTidsbegrensetArbeidsforhold(BeregningsgrunnlagInput input, FastsettBGTidsbegrensetArbeidsforholdDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE);
        BeregningsgrunnlagGrunnlagDto resultatFraKalkulus = FastsettBGTidsbegrensetArbeidsforholdHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterFastsettBeregningsgrunnlagATFL(BeregningsgrunnlagInput input, FastsettBeregningsgrunnlagATFLDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE);
        BeregningsgrunnlagGrunnlagDto resultatFraKalkulus = FastsettBeregningsgrunnlagATFLHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultatFraKalkulus);
    }

    public void håndterBeregningAktivitetOverstyring(BeregningsgrunnlagInput input, List<BeregningsaktivitetLagreDto> overstyrAktiviteter) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE);
        BeregningsgrunnlagGrunnlagDto grunnlag = AvklarAktiviteterHåndterer.håndterOverstyring(overstyrAktiviteter, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), grunnlag);
    }

    public void håndterBeregningsgrunnlagOverstyring(BeregningsgrunnlagInput input, OverstyrBeregningsgrunnlagDto overstyrBeregningsgrunnlagDto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE);
        BeregningsgrunnlagGrunnlagDto resultat = beregningFaktaOgOverstyringHåndterer.håndterMedOverstyring(håndterBeregningsgrunnlagInput, overstyrBeregningsgrunnlagDto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBruttoForSNNyIArbeidslivet(BeregningsgrunnlagInput input, FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE);
        BeregningsgrunnlagGrunnlagDto resultat = FastsettBruttoBeregningsgrunnlagSNforNyIArbeidslivetHåndterer.oppdater(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFastsettBeregningsgrunnlagSN(BeregningsgrunnlagInput input, FastsettBruttoBeregningsgrunnlagSNDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE_KODE);
        BeregningsgrunnlagGrunnlagDto resultat = FastsettBruttoBeregningsgrunnlagSNHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterFordelBeregningsgrunnlag(BeregningsgrunnlagInput input, FordelBeregningsgrunnlagDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE);
        BeregningsgrunnlagGrunnlagDto resultat = FordelBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderRefusjonBeregningsgrunnlag(BeregningsgrunnlagInput input, VurderRefusjonBeregningsgrunnlagDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN);
        BeregningsgrunnlagGrunnlagDto resultat = VurderRefusjonBeregningsgrunnlagHåndterer.håndter(dto, håndterBeregningsgrunnlagInput);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }


    public void håndterVurderFaktaOmBeregning(BeregningsgrunnlagInput input, FaktaBeregningLagreDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE);
        BeregningsgrunnlagGrunnlagDto resultat = beregningFaktaOgOverstyringHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
    }

    public void håndterVurderVarigEndretNyoppstartetSN(BeregningsgrunnlagInput input, VurderVarigEndringEllerNyoppstartetSNDto dto) {
        HåndterBeregningsgrunnlagInput håndterBeregningsgrunnlagInput = kalkulatorHåndteringInputTjeneste.lagInput(input.getKoblingReferanse().getKoblingId(), input, AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE);
        BeregningsgrunnlagGrunnlagDto resultat = VurderVarigEndretNyoppstartetSNHåndterer.håndter(håndterBeregningsgrunnlagInput, dto);
        //TODO(OJR) hvorfor kan denne gi null????
        if (resultat != null) {
            beregningsgrunnlagTjeneste.lagre(getBehandlingId(input), resultat);
        }
    }

    private Long getBehandlingId(BeregningsgrunnlagInput input) {
        return input.getKoblingReferanse().getKoblingId();
    }

}
