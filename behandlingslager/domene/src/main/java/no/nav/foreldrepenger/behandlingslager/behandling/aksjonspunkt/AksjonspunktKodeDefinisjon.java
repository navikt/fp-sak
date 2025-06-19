package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import java.time.Period;

import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

public class AksjonspunktKodeDefinisjon {

    // Aksjonspunkt Nr

    public static final String AUTO_MANUELT_SATT_PÅ_VENT_KODE = "7001";
    public static final String AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE = "7003";
    public static final String AUTO_SATT_PÅ_VENT_REVURDERING_KODE = "7005";
    public static final String AUTO_VENT_PÅ_FØDSELREGISTRERING_KODE = "7002";
    public static final String AUTO_KØET_BEHANDLING_KODE = "7011";
    public static final String AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE = "7014";
    public static final String AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE = "7020";
    public static final String AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE = "7030";
    public static final String AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN_KODE = "7033";
    public static final String AUTO_VENT_PÅ_SYKEMELDING_KODE = "7037";
    public static final String AUTO_VENT_PÅ_KABAL_KLAGE_KODE = "7039";
    public static final String AUTO_VENT_PÅ_KABAL_ANKE_KODE = "7040";


    public static final String AVKLAR_DEKNINGSGRAD_KODE = "5002";

    public static final String AVKLAR_ADOPSJONSDOKUMENTAJON_KODE = "5004";
    public static final String AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN_KODE = "5005";
    public static final String AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE_KODE = "5006";
    public static final String AVKLAR_TERMINBEKREFTELSE_KODE = "5001";
    public static final String AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE_KODE = "5008";
    public static final String AVKLAR_VILKÅR_FOR_FORELDREANSVAR_KODE = "5054";
    public static final String AVKLAR_VERGE_KODE = "5030";
    public static final String AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE_KODE = "5031";

    public static final String FATTER_VEDTAK_KODE = "5016";

    public static final String FORESLÅ_VEDTAK_KODE = "5015";
    public static final String FORESLÅ_VEDTAK_MANUELT_KODE = "5028";


    public static final String MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD_KODE = "5013";
    public static final String MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD_KODE = "5014";
    public static final String MANUELL_VURDERING_AV_OMSORGSVILKÅRET_KODE = "5011";
    public static final String MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET_KODE = "5007";


    public static final String OVERSTYRING_AV_ADOPSJONSVILKÅRET_KODE = "6004";
    public static final String OVERSTYRING_AV_ADOPSJONSVILKÅRET_FP_KODE = "6010";
    public static final String OVERSTYRING_AV_FØDSELSVILKÅRET_KODE = "6003";
    public static final String OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR_KODE = "6009";
    public static final String OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE = "6005";
    public static final String OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR_KODE = "6017";
    public static final String OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE = "6006";
    public static final String OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE = "6011";
    public static final String OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE = "6014";
    public static final String OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE = "6015";
    public static final String OVERSTYRING_AV_DEKNINGSGRAD_KODE = "6016";

    public static final String REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE = "5012";

    public static final String SØKERS_OPPLYSNINGSPLIKT_MANU_KODE = "5017";
    public static final String SØKERS_OPPLYSNINGSPLIKT_OVST_KODE = "6002";

    public static final String VARSEL_REVURDERING_MANUELL_KODE = "5026";
    public static final String KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE = "5055";

    public static final String SJEKK_MANGLENDE_FØDSEL_KODE = "5027";

    public static final String VENT_PÅ_SCANNING_KODE = "7007";
    public static final String VENT_PGA_FOR_TIDLIG_SØKNAD_KODE = "7008";
    public static final String VENT_PÅ_SØKNAD_KODE = "7013";

    public static final String VURDERE_INNTEKTSMELDING_FØR_VEDTAK_KODE = "5003";
    public static final String VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE = "5033";
    public static final String VURDERE_DOKUMENT_FØR_VEDTAK_KODE = "5034";

    public static final String MANUELL_VURDERING_AV_KLAGE_NFP_KODE = "5035";
    public static final String VURDERING_AV_FORMKRAV_KLAGE_NFP_KODE = "5082";

    public static final String VURDER_INNSYN_KODE = "5037";
    public static final String FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE = "5038";
    public static final String FORDEL_BEREGNINGSGRUNNLAG_KODE = "5046";
    public static final String FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE = "5047";
    public static final String FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE = "5049";
    public static final String AVKLAR_AKTIVITETER_KODE = "5052";

    public static final String MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG_KODE = "5060";
    public static final String VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE = "5039";
    public static final String REGISTRER_PAPIRSØKNAD_FORELDREPENGER_KODE = "5040";
    public static final String REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER_KODE = "5057";
    public static final String VURDER_FAKTA_FOR_ATFL_SN_KODE = "5058";
    public static final String VURDER_REFUSJON_BERGRUNN = "5059";

    public static final String MANUELL_VURDERING_AV_SØKNADSFRIST_KODE = "5043";

    public static final String AVKLAR_LØPENDE_OMSORG = "5061";

    public static final String MANUELL_KONTROLL_AV_BESTEBEREGNING_KODE = "5062";

    public static final String OVERSTYRING_AV_AVKLART_STARTDATO_KODE = "6045";
    public static final String OVERSTYRING_FAKTA_UTTAK_KODE = "6065";
    public static final String FASTSETT_UTTAKPERIODER_KODE = "5071";
    public static final String OVERSTYRING_AV_UTTAKPERIODER_KODE = "6008";
    public static final String AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT_KODE = "5086";
    public static final String OVERSTYRING_AV_RETT_OG_OMSORG_KODE = "6018";
    public static final String VURDER_UTTAK_DOKUMENTASJON_KODE = "5074";
    public static final String FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET_KODE = "5063";
    public static final String FAKTA_UTTAK_INGEN_PERIODER_KODE = "5064";
    public static final String FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO_KODE = "5065";
    public static final String FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG_KODE = "5066";

    public static final String KONTROLLER_ANNENPART_EØS_KODE = "5069";
    public static final String VURDER_UTTAK_STORTINGSREPRESENTANT_KODE = "5072";
    public static final String KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE_KODE = "5073";
    public static final String KONTROLLER_OPPLYSNINGER_OM_DØD_KODE = "5076";
    public static final String KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE = "5077";

    public static final String VURDER_PERIODER_MED_OPPTJENING_KODE = "5051";
    public static final String VURDER_PERMISJON_UTEN_SLUTTDATO_KODE = "5041";
    public static final String VURDER_FEILUTBETALING_KODE = "5084";
    public static final String VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING_KODE = "5085";
    public static final String VURDER_OPPTJENINGSVILKÅRET_KODE = "5089";

    public static final String KONTROLLER_STOR_ETTERBETALING_SØKER_KODE = "5029";

    public static final String AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE = "5068";

    public static final String VURDER_SVP_TILRETTELEGGING_KODE = "5091";
    public static final String MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET_KODE = "5092";

    public static final String VURDER_FARESIGNALER_KODE = "5095";

    public static final String REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE = "5096";

    public static final String VURDER_MEDLEMSKAPSVILKÅRET = "5101";
    public static final String VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR = "5102";
    public static final String AVKLAR_UTTAK_I_EØS_FOR_ANNENPART = "5103";

    // Andre koder
    public static final SkjermlenkeType UTEN_SKJERMLENKE = null;
    public static final VilkårType UTEN_VILKÅR = null;
    public static final Period UTEN_FRIST = null;
    public static final boolean UTVID_BEHFRIST = true;
    public static final boolean SAMME_BEHFRIST = false;
    public static final boolean TOTRINN = true;
    public static final boolean ENTRINN = false;
    public static final boolean TILBAKE = true;
    public static final boolean FORBLI = false;

    // Ledige aksjonspunktkoder 5001-5102
    // 5018, 5100

    // Utgåtte aksjonspunktkoder. Helst ikke gjenbruk 5nnn til andre formål enn det opprinnelige før det har gått noe tid
    //  "5009"
    //  "5019"
    //  "5020"  "5021"  "5022"  "5023"  "5024"  "5025"
    //  "5032"  "5036"
    //  "5042"  "5044"  "5045"  "5048"
    //  "5050"  "5053"  "5056"
    //  "5067"
    //  "5070"  "5075"  "5078"  "5079"
    //  "5080"  "5081"  "5083"  "5088"
    //  "5090"  "5093"  "5094"  "5097" "5098"  "5099"

    //  "6007" "6012"  "6013"  "6068"  "6070"

    //  "7004"  "7006"  "7009"
    //  "7015"  "7016"  "7017"  "7018"  "7019"
    //  "7021"  "7022"  "7023"  "7024"  "7025"  "7026"  "7027"  "7028"  "7029"
    //  "7032"  "7034"  "7035"  "7036"  "7038"  "7041"

    private AksjonspunktKodeDefinisjon() {
    }
}
