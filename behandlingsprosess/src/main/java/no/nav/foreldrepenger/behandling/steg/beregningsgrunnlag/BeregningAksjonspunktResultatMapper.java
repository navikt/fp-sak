package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulator.output.BeregningAvklaringsbehovResultat;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Venteårsak;

class BeregningAksjonspunktResultatMapper {

    private BeregningAksjonspunktResultatMapper() {
    }

    static AksjonspunktResultat map(BeregningAvklaringsbehovResultat beregningResultat) {
        var apDef = mapTilAksjonspunkt(beregningResultat);
        if (beregningResultat.harFrist()) {
            return AksjonspunktResultat.opprettForAksjonspunktMedFrist(apDef, mapTilVenteårsak(beregningResultat),
                    beregningResultat.getVentefrist());
        }
        return AksjonspunktResultat.opprettForAksjonspunkt(apDef);
    }

    private static AksjonspunktDefinisjon mapTilAksjonspunkt(BeregningAvklaringsbehovResultat beregningResultat) {
        return switch(beregningResultat.getBeregningAvklaringsbehovDefinisjon()) {
            case FASTSETT_BG_AT_FL -> AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS;
            case VURDER_VARIG_ENDRT_NYOPPSTR_NAERNG_SN -> AksjonspunktDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE;
            case FORDEL_BG -> AksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG;
            case FASTSETT_BG_TB_ARB -> AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD;
            case FASTSETT_BG_SN_NY_I_ARB_LIVT -> AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET;
            case AVKLAR_AKTIVITETER -> AksjonspunktDefinisjon.AVKLAR_AKTIVITETER;
            case VURDER_FAKTA_ATFL_SN -> AksjonspunktDefinisjon.VURDER_FAKTA_FOR_ATFL_SN;
            case VURDER_REFUSJONSKRAV -> AksjonspunktDefinisjon.VURDER_REFUSJON_BERGRUNN;
            case OVST_BEREGNINGSAKTIVITETER -> AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER;
            case OVST_INNTEKT -> AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG;
            case AUTO_VENT_PÅ_INNTKT_RAP_FRST -> AksjonspunktDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST;
            case AUTO_VENT_PÅ_SISTE_AAP_DP_MELDKRT -> AksjonspunktDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT;
            default -> throw new IllegalStateException("Mottok ukjent aksjonspunkt fra kalkulus " + beregningResultat.getBeregningAvklaringsbehovDefinisjon());
        };
    }

    private static Venteårsak mapTilVenteårsak(BeregningAvklaringsbehovResultat beregningResultat) {
        return switch(beregningResultat.getVenteårsak()) {
            case VENT_PÅ_SISTE_AAP_MELDEKORT -> Venteårsak.VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT;
            case VENT_INNTEKT_RAPPORTERINGSFRIST -> Venteårsak.VENT_INNTEKT_RAPPORTERINGSFRIST;
            default -> throw new IllegalStateException("Mottok ukjent venteårsak fra kalkulus " + beregningResultat.getVenteårsak());
        };
    }

}
