package no.nav.foreldrepenger.domene.migrering;

import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovDefinisjon;
import no.nav.folketrygdloven.kalkulus.kodeverk.AvklaringsbehovStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;

public class BeregningAvklaringsbehovMigreringMapper {
    private BeregningAvklaringsbehovMigreringMapper() {
        // Skjuler default
    }

    public static AvklaringsbehovStatus mapAvklaringStatus(AksjonspunktStatus status) {
        return switch (status) {
            case AVBRUTT -> AvklaringsbehovStatus.AVBRUTT;
            case OPPRETTET -> AvklaringsbehovStatus.OPPRETTET;
            case UTFØRT -> AvklaringsbehovStatus.UTFØRT;
        };
    }

    public static AvklaringsbehovDefinisjon finnBeregningAvklaringsbehov(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        return switch (aksjonspunktDefinisjon) {
            case FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS -> AvklaringsbehovDefinisjon.FASTSETT_BG_AT_FL;
            case VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE -> AvklaringsbehovDefinisjon.VURDER_VARIG_ENDRT_NYOPPSTR_NAERNG_SN;
            case FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET -> AvklaringsbehovDefinisjon.FASTSETT_BG_SN_NY_I_ARB_LIVT;
            case FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD -> AvklaringsbehovDefinisjon.FASTSETT_BG_TB_ARB;
            case VURDER_FAKTA_FOR_ATFL_SN -> AvklaringsbehovDefinisjon.VURDER_FAKTA_ATFL_SN;
            case AVKLAR_AKTIVITETER -> AvklaringsbehovDefinisjon.AVKLAR_AKTIVITETER;
            case OVERSTYRING_AV_BEREGNINGSAKTIVITETER -> AvklaringsbehovDefinisjon.OVST_BEREGNINGSAKTIVITETER;
            case OVERSTYRING_AV_BEREGNINGSGRUNNLAG -> AvklaringsbehovDefinisjon.OVST_INNTEKT;
            case VURDER_REFUSJON_BERGRUNN -> AvklaringsbehovDefinisjon.VURDER_REFUSJONSKRAV;
            case FORDEL_BEREGNINGSGRUNNLAG -> AvklaringsbehovDefinisjon.FORDEL_BG;
            case AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST -> AvklaringsbehovDefinisjon.AUTO_VENT_PÅ_INNTKT_RAP_FRST;
            case AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT -> AvklaringsbehovDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_DP_MELDKRT;
            default -> null;
        };
    }
}
