package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.ENTRINN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.FORBLI;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TILBAKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TOTRINN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_FRIST;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_SKJERMLENKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_VILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType.FAKTA_OM_AKTIVITETSKRAV;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.ES;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.FP;
import static no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType.SVP;

import java.time.Period;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Definerer mulige Aksjonspunkter inkludert hvilket Vurderingspunkt de må løses i.
 * Inkluderer også konstanter for å enklere kunne referere til dem i eksisterende logikk.
 */
public enum AksjonspunktDefinisjon implements Kodeverdi {

    // Gruppe : 500

    AVKLAR_TERMINBEKREFTELSE(AksjonspunktKodeDefinisjon.AVKLAR_TERMINBEKREFTELSE_KODE,
            AksjonspunktType.MANUELL, "Avklar terminbekreftelse", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN,
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.FAKTA_OM_FOEDSEL, ENTRINN, EnumSet.of(ES, FP)),
    AVKLAR_ADOPSJONSDOKUMENTAJON(AksjonspunktKodeDefinisjon.AVKLAR_ADOPSJONSDOKUMENTAJON_KODE,
            AksjonspunktType.MANUELL, "Avklar adopsjonsdokumentasjon", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN,
            VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, SkjermlenkeType.FAKTA_OM_ADOPSJON, ENTRINN, EnumSet.of(ES, FP)),
    AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN(
            AksjonspunktKodeDefinisjon.AVKLAR_OM_ADOPSJON_GJELDER_EKTEFELLES_BARN_KODE, AksjonspunktType.MANUELL, "Avklar om adopsjon gjelder ektefelles barn",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN, VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
            SkjermlenkeType.FAKTA_OM_ADOPSJON, TOTRINN, EnumSet.of(ES, FP)),
    AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE(
            AksjonspunktKodeDefinisjon.AVKLAR_OM_SØKER_ER_MANN_SOM_ADOPTERER_ALENE_KODE, AksjonspunktType.MANUELL, "Avklar om søker er mann adopterer alene",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN, VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
            SkjermlenkeType.FAKTA_OM_ADOPSJON, ENTRINN, EnumSet.of(ES, FP)),
    MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av søknadsfristvilkåret",
            BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR, VurderingspunktType.UT, VilkårType.SØKNADSFRISTVILKÅRET, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, EnumSet.of(ES)),
    AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE(
            AksjonspunktKodeDefinisjon.AVKLAR_VILKÅR_FOR_OMSORGSOVERTAKELSE_KODE, AksjonspunktType.MANUELL, "Avklar fakta for omsorgs/foreldreansvarsvilkåret",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN, VilkårType.OMSORGSVILKÅRET, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP)),
    MANUELL_VURDERING_AV_OMSORGSVILKÅRET(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_OMSORGSVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av omsorgsvilkåret",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT, VilkårType.OMSORGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OMSORG, TOTRINN,
            EnumSet.of(ES, FP)),
    REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE, AksjonspunktType.MANUELL, "Registrer papirsøknad engangsstønad",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_2_LEDD_KODE, AksjonspunktType.MANUELL,
            "Manuell vurdering av foreldreansvarsvilkåret 2.ledd", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD, SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR, TOTRINN, EnumSet.of(ES, FP)),
    MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_FORELDREANSVARSVILKÅRET_4_LEDD_KODE, AksjonspunktType.MANUELL,
            "Manuell vurdering av foreldreansvarsvilkåret 4.ledd", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD, SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR, TOTRINN, EnumSet.of(ES, FP)),
    FORESLÅ_VEDTAK(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Vurder om ytelse allerede er innvilget", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VEDTAK, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FATTER_VEDTAK(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Fatter vedtak", BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.VEDTAK, ENTRINN,
            EnumSet.of(ES, FP, SVP)),
    SØKERS_OPPLYSNINGSPLIKT_MANU(
            AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_MANU_KODE, AksjonspunktType.MANUELL,
            "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
            VurderingspunktType.UT, VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VEDTAK_UTEN_TOTRINNSKONTROLL(
            AksjonspunktKodeDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL_KODE, AksjonspunktType.MANUELL, "Foreslå vedtak uten totrinnskontroll",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_LOVLIG_OPPHOLD(AksjonspunktKodeDefinisjon.AVKLAR_LOVLIG_OPPHOLD_KODE,
            AksjonspunktType.MANUELL, "Avklar lovlig opphold.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OM_ER_BOSATT(AksjonspunktKodeDefinisjon.AVKLAR_OM_ER_BOSATT_KODE,
            AksjonspunktType.MANUELL, "Avklar om bruker er bosatt.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE(
            AksjonspunktKodeDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE, AksjonspunktType.MANUELL, "Avklar om bruker har gyldig periode.",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP,
            ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OPPHOLDSRETT(AksjonspunktKodeDefinisjon.AVKLAR_OPPHOLDSRETT_KODE,
            AksjonspunktType.MANUELL, "Avklar oppholdsrett.", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN,
            VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VARSEL_REVURDERING_ETTERKONTROLL( // Brukes ikke lenger
            AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_ETTERKONTROLL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering ved automatisk etterkontroll",
            BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP)),
    VARSEL_REVURDERING_MANUELL( // Kun ES
            AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering opprettet manuelt",
            BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES)),
    SJEKK_MANGLENDE_FØDSEL(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE,
            AksjonspunktType.MANUELL, "Sjekk manglende fødsel", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN,
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.FAKTA_OM_FOEDSEL, ENTRINN, EnumSet.of(ES, FP)),
    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
            AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VEDTAK,
            ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_VERGE(AksjonspunktKodeDefinisjon.AVKLAR_VERGE_KODE, AksjonspunktType.MANUELL,
            "Avklar verge", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_VERGE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE(
            AksjonspunktKodeDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE_KODE, AksjonspunktType.MANUELL, "Vurdere om søkers ytelse gjelder samme barn",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, TOTRINN, EnumSet.of(ES, FP)),
    AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE(
            AksjonspunktKodeDefinisjon.AVKLAR_OM_ANNEN_FORELDRE_HAR_MOTTATT_STØTTE_KODE, AksjonspunktType.MANUELL,
            "Vurdere om annen forelder sin ytelse gjelder samme barn", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE,
            TOTRINN, EnumSet.of(ES, FP)),
    VURDERE_ANNEN_YTELSE_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere annen ytelse før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDERE_DOKUMENT_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere dokument før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK,
            VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_KLAGE_NFP(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av klage (NFP)",
            BehandlingStegType.KLAGE_NFP, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.KLAGE_BEH_NFP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_KLAGE_NK(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NK_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av klage (NK)",
            BehandlingStegType.KLAGE_NK,
            VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.KLAGE_BEH_NK, TOTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_INNSYN(AksjonspunktKodeDefinisjon.VURDER_INNSYN_KODE,
            AksjonspunktType.MANUELL, "Vurder innsyn", BehandlingStegType.VURDER_INNSYN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, AksjonspunktType.MANUELL,
            "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
            AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
            "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    REGISTRER_PAPIRSØKNAD_FORELDREPENGER(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER_KODE, AksjonspunktType.MANUELL, "Registrer papirsøknad foreldrepenger",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_SØKNADSFRIST(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av søknadsfrist",
            BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_OM_VILKÅR_FOR_SYKDOM_OPPFYLT(
            AksjonspunktKodeDefinisjon.VURDER_OM_VILKÅR_FOR_SYKDOM_OPPFYLT_KODE, AksjonspunktType.MANUELL, "Vurder om vilkår for sykdom er oppfylt",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN, VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR,
            SkjermlenkeType.FAKTA_OM_FOEDSEL, TOTRINN, EnumSet.of(ES, FP)),
    FORDEL_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE,
            AksjonspunktType.MANUELL, "Fordel beregningsgrunnlag", BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_PERIODER_MED_OPPTJENING(
            AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Vurder perioder med opptjening",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.INN, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.FAKTA_FOR_OPPTJENING,
            ENTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_AKTIVITETER(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE,
            AksjonspunktType.MANUELL, "Avklar aktivitet for beregning", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_FORTSATT_MEDLEMSKAP(
            AksjonspunktKodeDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP_KODE, AksjonspunktType.MANUELL, "Avklar fortsatt medlemskap.",
            BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE,
            SkjermlenkeType.FAKTA_OM_MEDLEMSKAP, TOTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_VILKÅR_FOR_FORELDREANSVAR(
            AksjonspunktKodeDefinisjon.AVKLAR_VILKÅR_FOR_FORELDREANSVAR_KODE, AksjonspunktType.MANUELL, "Avklar fakta for foreldreansvarsvilkåret for FP",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN, VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD,
            SkjermlenkeType.PUNKT_FOR_FORELDREANSVAR, ENTRINN, EnumSet.of(ES, FP)),
    KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST(
            AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE, AksjonspunktType.MANUELL,
            "Vurder varsel ved vedtak til ugunst",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING(
            AksjonspunktKodeDefinisjon.KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE, AksjonspunktType.MANUELL,
            "Kontroll av manuelt opprettet revurderingsbehandling", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            EnumSet.of(ES, FP, SVP)),
    REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER_KODE, AksjonspunktType.MANUELL,
            "Registrer papir endringssøknad foreldrepenger",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_FAKTA_FOR_ATFL_SN(AksjonspunktKodeDefinisjon.VURDER_FAKTA_FOR_ATFL_SN_KODE,
            AksjonspunktType.MANUELL, "Vurder fakta for arbeidstaker, frilans og selvstendig næringsdrivende", BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
            VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_REFUSJON_BERGRUNN(AksjonspunktKodeDefinisjon.VURDER_REFUSJON_BERGRUNN,
        AksjonspunktType.MANUELL, "Vurder refusjonskrav for beregningen", BehandlingStegType.VURDER_REF_BERGRUNN,
        VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, EnumSet.of(FP, SVP)),
    MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG(
            AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_ALENEOMSORG_KODE, AksjonspunktType.MANUELL,
            "Manuell kontroll av om bruker har aleneomsorg", BehandlingStegType.KONTROLLER_OMSORG_RETT, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OMSORG_OG_RETT, ENTRINN, EnumSet.of(FP)),
    MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG(
            AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_OM_BRUKER_HAR_OMSORG_KODE, AksjonspunktType.MANUELL, "Manuell kontroll av om bruker har omsorg",
            BehandlingStegType.KONTROLLER_FAKTA_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_FOR_OMSORG, ENTRINN, EnumSet.of(FP)),
    AUTOMATISK_MARKERING_AV_UTENLANDSSAK(
            AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE, AksjonspunktType.MANUELL,
            "Innhent dokumentasjon fra utenlandsk trygdemyndighet",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_KONTROLLER_SØKNADSPERIODER_KODE,
            AksjonspunktType.MANUELL, "Kontrollerer søknadsperioder", BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.INN, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_UTTAK, ENTRINN, EnumSet.of(FP)),
    AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET_KODE,
        AksjonspunktType.MANUELL, "Gradering i søknadsperiode er lagt på ukjent aktivitet", BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.INN, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_OM_UTTAK, ENTRINN, EnumSet.of(FP)),
    AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG_KODE,
        AksjonspunktType.MANUELL, "Gradering i søknadsperiode er lagt på aktivitet uten beregningsgrunnlag", BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.INN, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_OM_UTTAK, ENTRINN, EnumSet.of(FP)),
    FASTSETT_UTTAKPERIODER(AksjonspunktKodeDefinisjon.FASTSETT_UTTAKPERIODER_KODE,
            AksjonspunktType.MANUELL, "Fastsett uttaksperioder manuelt", BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP)),
    KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE(
            AksjonspunktKodeDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE_KODE, AksjonspunktType.MANUELL, "Kontroller realitetsbehandling/klage",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_FORDELING_AV_STØNADSPERIODEN(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_FORDELING_AV_STØNADSPERIODEN_KODE, AksjonspunktType.MANUELL,
            "Kontroller opplysninger om fordeling av stønadsperioden", BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_DØD(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om død",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om søknadsfrist",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_ARBEIDSFORHOLD(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_KODE,
            AksjonspunktType.MANUELL, "Avklar arbeidsforhold", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD, ENTRINN, EnumSet.of(FP, SVP)),
    VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING_KODE,
        AksjonspunktType.MANUELL, "Avklar mangler rundt arbeidsforhold og inntektsmelding", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_INNTEKTSMELDING, ENTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_FØRSTE_UTTAKSDATO(AksjonspunktKodeDefinisjon.AVKLAR_FØRSTE_UTTAKSDATO_KODE,
            AksjonspunktType.MANUELL, "Avklar første uttaksdato", BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.INN, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_UTTAK, ENTRINN, EnumSet.of(FP, SVP)),
    VURDERING_AV_FORMKRAV_KLAGE_NFP(
            AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP_KODE, AksjonspunktType.MANUELL, "Vurder formkrav (NFP).",
            BehandlingStegType.KLAGE_VURDER_FORMKRAV_NFP, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FORMKRAV_KLAGE_NFP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDERING_AV_FORMKRAV_KLAGE_KA(
            AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_KA_KODE, AksjonspunktType.MANUELL, "Vurder formkrav (NK).",
            BehandlingStegType.KLAGE_VURDER_FORMKRAV_NK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FORMKRAV_KLAGE_KA, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_FEILUTBETALING(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE,
            AksjonspunktType.MANUELL, "Vurder feilutbetaling", BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT(
            AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT_KODE, AksjonspunktType.MANUELL, "Avklar annen forelder har rett",
            BehandlingStegType.KONTROLLER_OMSORG_RETT, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OMSORG_OG_RETT, ENTRINN, EnumSet.of(FP, SVP)),
    VURDER_DEKNINGSGRAD(AksjonspunktKodeDefinisjon.VURDER_DEKNINGSGRAD_KODE,
            AksjonspunktType.MANUELL, "Vurder Dekningsgrad", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_OPPTJENINGSVILKÅRET(
            AksjonspunktKodeDefinisjon.VURDER_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av opptjeningsvilkår",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_PERMISJON_UTEN_SLUTTDATO(
            AksjonspunktKodeDefinisjon.VURDER_PERMISJON_UTEN_SLUTTDATO_KODE, AksjonspunktType.MANUELL,"Vurder arbeidsforhold med permisjon uten sluttdato",
            BehandlingStegType.VURDER_ARB_FORHOLD_PERMISJON, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_PERMISJON,
            ENTRINN, EnumSet.of(FP, SVP)),
    MANUELL_KONTROLL_AV_BESTEBEREGNING(
        AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AV_BESTEBEREGNING_KODE, AksjonspunktType.MANUELL, "Kontroller den automatiske besteberegningen",
        BehandlingStegType.FORESLÅ_BESTEBEREGNING, VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BESTEBEREGNING,
        ENTRINN, EnumSet.of(FP)),
    VURDER_TILBAKETREKK(AksjonspunktKodeDefinisjon.VURDER_TILBAKETREKK_KODE,
            AksjonspunktType.MANUELL, "Vurder tilbaketrekk", BehandlingStegType.VURDER_TILBAKETREKK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.TILKJENT_YTELSE, TOTRINN, EnumSet.of(FP)),
    VURDER_SVP_TILRETTELEGGING(
            AksjonspunktKodeDefinisjon.VURDER_SVP_TILRETTELEGGING_KODE, AksjonspunktType.MANUELL, "Vurder tilrettelegging svangerskapspenger",
            BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.PUNKT_FOR_SVP_INNGANG, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET_KODE, AksjonspunktType.MANUELL, "Avklar svangerskapspengervilkåret",
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR, VurderingspunktType.UT, VilkårType.SVANGERSKAPSPENGERVILKÅR,
            SkjermlenkeType.PUNKT_FOR_SVANGERSKAPSPENGER, ENTRINN, EnumSet.of(SVP)),
    MANUELL_VURDERING_AV_ANKE(AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_KODE,
            AksjonspunktType.MANUELL, "Manuell vurdering av anke", BehandlingStegType.ANKE, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.ANKE_VURDERING,
            TOTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_ANKE_MERKNADER(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_ANKE_MERKNADER_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av anke merknader",
            BehandlingStegType.ANKE_MERKNADER, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.ANKE_MERKNADER, TOTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_FARESIGNALER(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE,
            AksjonspunktType.MANUELL, "Vurder Faresignaler", BehandlingStegType.VURDER_FARESIGNALER, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VURDER_FARESIGNALER, TOTRINN, EnumSet.of(ES, FP, SVP)),
    REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE, AksjonspunktType.MANUELL, "Registrer papirsøknad svangerskapspenger",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    KONTROLLER_AKTIVITETSKRAV(
        AksjonspunktKodeDefinisjon.KONTROLLER_AKTIVITETSKRAV, AksjonspunktType.MANUELL, "Kontroller aktivitetskrav",
        BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.UT, UTEN_VILKÅR, FAKTA_OM_AKTIVITETSKRAV, TOTRINN, EnumSet.of(FP)),

    // Gruppe : 600

    SØKERS_OPPLYSNINGSPLIKT_OVST(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING,
            "Saksbehandler initierer kontroll av søkers opplysningsplikt", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, VurderingspunktType.UT,
        VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_FØDSELSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av fødselsvilkåret", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL, TOTRINN, EnumSet.of(ES, FP)),
    OVERSTYRING_AV_ADOPSJONSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_ADOPSJONSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av adopsjonsvilkåret", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD, SkjermlenkeType.PUNKT_FOR_ADOPSJON, TOTRINN, EnumSet.of(ES, FP)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av medlemskapsvilkåret",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP,
            TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_SØKNADSFRISTVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av søknadsfristvilkåret",
            BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR, VurderingspunktType.UT, VilkårType.SØKNADSFRISTVILKÅRET, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, EnumSet.of(ES)),
    OVERSTYRING_AV_BEREGNING(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNING_KODE,
            AksjonspunktType.OVERSTYRING, "Overstyring av beregning", BehandlingStegType.BEREGN_YTELSE, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_UTTAKPERIODER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_UTTAKPERIODER_KODE, AksjonspunktType.OVERSTYRING, "Overstyr uttaksperioder",
            BehandlingStegType.BEREGN_YTELSE,
            VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av fødselsvilkåret for far/medmor", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL, TOTRINN, EnumSet.of(ES, FP)),
    OVERSTYRING_AV_ADOPSJONSVILKÅRET_FP(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_ADOPSJONSVILKÅRET_FP_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av adopsjonsvilkåret for foreldrepenger", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.ADOPSJONSVILKARET_FORELDREPENGER, SkjermlenkeType.PUNKT_FOR_ADOPSJON, TOTRINN, EnumSet.of(ES, FP)),
    OVERSTYRING_AV_OPPTJENINGSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av opptjeningsvilkåret",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av løpende medlemskapsvilkåret", BehandlingStegType.VULOMED, VurderingspunktType.UT,
            VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP_LØPENDE, TOTRINN, EnumSet.of(FP)),
    OVERSTYRING_AV_FAKTA_UTTAK(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_UTTAK_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyr søknadsperioder", BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.INN,
            UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_BEREGNINGSAKTIVITETER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av beregningsaktiviteter", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
            UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av beregningsgrunnlag",
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_AVKLART_STARTDATO(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO_KODE, AksjonspunktType.MANUELL, "Overstyr avklart startdato for foreldrepengeperioden",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP,
            TOTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_MARKERING_AV_UTLAND_SAKSTYPE(AksjonspunktKodeDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE_KODE, AksjonspunktType.MANUELL, "Manuell markering av utenlandssak",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.UTLAND, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING(AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_UTTAK_SAKSBEHANDLER_OVERSTYRING_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING,
            "Saksbehandler endret søknadsperioder uten aksjonspunkt", BehandlingStegType.KONTROLLER_AKTIVITETSKRAV, VurderingspunktType.INN, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_UTTAK, TOTRINN, EnumSet.of(FP, SVP)),

    // Gruppe : 700

    AUTO_MANUELT_SATT_PÅ_VENT(AksjonspunktKodeDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT_KODE, AksjonspunktType.AUTOPUNKT,
            "Manuelt satt på vent", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            FORBLI, "P4W", EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_FØDSELREGISTRERING(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på fødsel ved avklaring av søkers relasjon til barnet", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            TILBAKE, UTEN_FRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENTER_PÅ_KOMPLETT_SØKNAD(AksjonspunktKodeDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT,
            "Venter på komplett søknad", BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P4W", EnumSet.of(ES, FP, SVP)),
    VENT_PÅ_FØDSEL(AksjonspunktKodeDefinisjon.VENT_PÅ_FØDSEL_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på fødsel ved avklaring av medlemskap", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR,
            UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P3W", EnumSet.of(ES, FP, SVP)),
    AUTO_SATT_PÅ_VENT_REVURDERING(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING_KODE, AksjonspunktType.AUTOPUNKT,
            "Satt på vent etter varsel om revurdering", BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE,
            ENTRINN, FORBLI, "P4W", EnumSet.of(ES, FP, SVP)),
    VENT_PÅ_SCANNING(AksjonspunktKodeDefinisjon.VENT_PÅ_SCANNING_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på scanning", BehandlingStegType.VURDER_INNSYN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            "P3D", EnumSet.of(ES, FP, SVP)),
    VENT_PGA_FOR_TIDLIG_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT, "Satt på vent pga for tidlig søknad",
            BehandlingStegType.VURDER_KOMPLETTHET, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(ES, FP, SVP)),

    AUTO_KØET_BEHANDLING(AksjonspunktKodeDefinisjon.AUTO_KØET_BEHANDLING_KODE,
            AksjonspunktType.AUTOPUNKT, "Autokøet behandling", BehandlingStegType.KONTROLLER_LØPENDE_MEDLEMSKAP, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
        FORBLI, UTEN_FRIST, EnumSet.of(FP)),
    VENT_PÅ_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PÅ_SØKNAD_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på søknad", BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            "P3W", EnumSet.of(FP, SVP)),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, AksjonspunktType.AUTOPUNKT, "Vent på rapporteringsfrist for inntekt",
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på siste meldekort for AAP eller DP-mottaker", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_ETTERLYST_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding",
            BehandlingStegType.INREG_AVSL, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P3W", EnumSet.of(FP, SVP)),
    AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER(AksjonspunktKodeDefinisjon.AUTO_VENT_ANKE_MERKNADER_FRA_BRUKER_KODE, AksjonspunktType.AUTOPUNKT, "Autopunkt anke venter på merknader fra bruker",
            BehandlingStegType.ANKE_MERKNADER, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI, "P3W", EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN(AksjonspunktKodeDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN_KODE, AksjonspunktType.AUTOPUNKT, "Autopunkt anke oversendt til Trygderetten",
            BehandlingStegType.ANKE_MERKNADER, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, "P2Y", EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_SYKEMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SYKEMELDING_KODE, AksjonspunktType.AUTOPUNKT,
        "Vent på siste sykemelding fra søker som mottar sykepenger basert på dagpenger", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP)),
    AUTO_VENT_PÅ_KORRIGERT_BESTEBERERGNING(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_KORRIGERT_BESTEBEREGNING_KODE, AksjonspunktType.AUTOPUNKT,
        "Vent på korrigering / feilretting av besteberegningen", BehandlingStegType.FORESLÅ_BESTEBEREGNING, VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(FP)),
    AUTO_VENT_PÅ_KABAL_KLAGE(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE_KODE, AksjonspunktType.AUTOPUNKT, "Vent på klagebehandling hos NAV Klageinstans",
        BehandlingStegType.KLAGE_NK, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_KABAL_ANKE(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE_KODE, AksjonspunktType.AUTOPUNKT, "Vent på ankebehandling hos NAV Klageinstans",
        BehandlingStegType.ANKE, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, EnumSet.of(ES, FP, SVP)),
    UNDEFINED,

    // Utgåtte aksjonspunktkoder - kun her for bakoverkompatibilitet. Finnes historisk i databasen til fpsak i PROD !
    @Deprecated
    _5009("5009", AksjonspunktType.MANUELL, "Avklar tilleggsopplysninger"),
    @Deprecated
    _5022("5022", AksjonspunktType.MANUELL, "Avklar fakta for status på person."),
    @Deprecated
    _5024("5024", AksjonspunktType.MANUELL, "Saksbehandler må avklare hvilke verdier som er gjeldene, det er mismatch mellom register- og lokaldata (UTGÅTT)"),
    @Deprecated // Håndteres nå sammen med 5039
    _5042("5042", AksjonspunktType.MANUELL, "Fastsett beregningsgrunnlag for selvstendig næringsdrivende"),
    @Deprecated
    _5045("5045", AksjonspunktType.MANUELL, "Avklar startdato for foreldrepengeperioden"),
    @Deprecated // Erstattet av aksjonspunkt 5062
    _5048("5048", AksjonspunktType.MANUELL, "Kontroller den automatiske besteberegningen."),
    @Deprecated
    _5050("5050", AksjonspunktType.MANUELL, "Vurder gradering på andel uten beregningsgrunnlag"),
    @Deprecated
    _5072("5072", AksjonspunktType.MANUELL, "Søker er stortingsrepresentant/administrativt ansatt i Stortinget"),
    @Deprecated
    _5078("5078", AksjonspunktType.MANUELL, "Kontroller tilstøtende ytelser innvilget"),
    @Deprecated
    _5079("5079", AksjonspunktType.MANUELL, "Kontroller tilstøtende ytelser opphørt"),
    @Deprecated
    _5088("5088", AksjonspunktType.MANUELL, "Oppgitt at annen forelder ikke rett, men har løpende utbetaling"),
    @Deprecated
    _7006("7006", AksjonspunktType.AUTOPUNKT, "Venter på opptjeningsopplysninger"),
    @Deprecated
    _7009("7009", AksjonspunktType.AUTOPUNKT, "Vent på oppdatering som passerer kompletthetssjekk"),
    @Deprecated
    _7015("7015", AksjonspunktType.AUTOPUNKT, "Venter på regler for 80% dekningsgrad (UTGÅTT)"),
    @Deprecated
    _7016("7016", AksjonspunktType.AUTOPUNKT, "Opprettes når opptjeningsvilkåret blir automatisk avslått. NB! Autopunkt som er innført til prodfeil på opptjenig er fikset (UTGÅTT)"),
    @Deprecated
    _7017("7017", AksjonspunktType.AUTOPUNKT, "Sett på vent - ventelønn/vartpenger og militær med flere aktiviteter (UTGÅTT)"),
    @Deprecated
    _7018("7018", AksjonspunktType.AUTOPUNKT, "Autopunkt dødfødsel 80% dekningsgrad."),
    @Deprecated
    _7019("7019", AksjonspunktType.AUTOPUNKT, "Autopunkt gradering uten beregningsgrunnlag."),
    @Deprecated
    _7021("7021", AksjonspunktType.AUTOPUNKT, "Endring i fordeling av ytelse bakover i tid (UTGÅTT)"),
    @Deprecated
    _7022("7022", AksjonspunktType.AUTOPUNKT, "Autopunkt vent på ny inntektsmelding med gyldig arbeidsforholdId."),
    @Deprecated
    _7023("7023", AksjonspunktType.AUTOPUNKT, "Autopunkt militær i opptjeningsperioden og beregninggrunnlag under 3G."),
    @Deprecated
    _7024("7024", AksjonspunktType.AUTOPUNKT, "Sett på vent - Arbeidsgiver krever refusjon 3 måneder tilbake i tid (UTGÅTT)"),
    @Deprecated
    _7025("7025", AksjonspunktType.AUTOPUNKT, "Autopunkt gradering flere arbeidsforhold."),
    @Deprecated
    _7026("7026", AksjonspunktType.AUTOPUNKT, "Autopunkt vent på ulike startdatoer i SVP."),
    @Deprecated
    _7027("7027", AksjonspunktType.AUTOPUNKT, "Autopunkt vent delvis tilrettelegging og refusjon SVP."),
    @Deprecated
    _7028("7028", AksjonspunktType.AUTOPUNKT, "Sett på vent - Søker har søkt SVP og hatt AAP eller DP siste 10 mnd (UTGÅTT)"),
    @Deprecated
    _7029("7029", AksjonspunktType.AUTOPUNKT, "Sett på vent - Støtter ikke FL/SN i svangerskapspenger (UTGÅTT)"),
    @Deprecated
    _7034("7034", AksjonspunktType.AUTOPUNKT, "Autopunkt flere arbeidsforhold i samme virksomhet SVP"),
    @Deprecated
    _7035("7035", AksjonspunktType.AUTOPUNKT, "Autopunkt potensielt feil i endringssøknad, kontakt bruker"),
    @Deprecated
    _7036("7036", AksjonspunktType.AUTOPUNKT, "Autopunkt vent manglende arbeidsforhold ifm kommunereform 2020."),

    ;

    static final String KODEVERK = "AKSJONSPUNKT_DEF";

    private static final Map<String, AksjonspunktDefinisjon> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private static final Set<String> UTELUKKENDE_AKSJONSPUNKT = Set.of(
        AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE,
        AksjonspunktKodeDefinisjon.AVKLAR_TERMINBEKREFTELSE_KODE);

    private static final Map<String, Set<AksjonspunktDefinisjon>> UTELUKKENDE_AP_MAP = Map.ofEntries(
        Map.entry(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL.getKode(), Set.of(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE)),
        Map.entry(AksjonspunktDefinisjon.AVKLAR_TERMINBEKREFTELSE.getKode(), Set.of(AksjonspunktDefinisjon.SJEKK_MANGLENDE_FØDSEL)),
        Map.entry(AksjonspunktDefinisjon.FORESLÅ_VEDTAK.getKode(), Set.of(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT, AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL)),
        Map.entry(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL.getKode(), Set.of(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT, AksjonspunktDefinisjon.FORESLÅ_VEDTAK)),
        Map.entry(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT.getKode(), Set.of(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL, AksjonspunktDefinisjon.FORESLÅ_VEDTAK))
    );

    private AksjonspunktType aksjonspunktType = AksjonspunktType.UDEFINERT;

    /**
     * Definerer hvorvidt Aksjonspunktet default krever totrinnsbehandling. Dvs. Beslutter må godkjenne hva
     * Saksbehandler har utført.
     */
    private boolean defaultTotrinnBehandling = false;

    /**
     * Hvorvidt aksjonspunktet har en frist før det må være løst. Brukes i forbindelse med når Behandling er lagt til
     * Vent.
     */
    private String fristPeriode;

    private VilkårType vilkårType;

    private SkjermlenkeType skjermlenkeType;

    private boolean tilbakehoppVedGjenopptakelse;

    private BehandlingStegType behandlingStegType;

    private String navn;

    private Set<YtelseType> ytelseTyper;

    private VurderingspunktType vurderingspunktType;

    private boolean erUtgått = false;

    @JsonValue
    private String kode;

    AksjonspunktDefinisjon() {
        // for hibernate
    }

    /** Brukes for utgåtte aksjonspunkt. Disse skal ikke kunne gjenoppstå. */
    AksjonspunktDefinisjon(String kode, AksjonspunktType type, String navn) {
        this.kode = kode;
        this.aksjonspunktType = type;
        this.navn = navn;
        erUtgått = true;
    }

    // Bruk for ordinære aksjonspunkt og overstyring
    AksjonspunktDefinisjon(String kode,
                           AksjonspunktType aksjonspunktType,
                           String navn,
                           BehandlingStegType behandlingStegType,
                           VurderingspunktType vurderingspunktType,
                           VilkårType vilkårType,
                           SkjermlenkeType skjermlenkeType,
                           boolean defaultTotrinnBehandling,
                           Set<FagsakYtelseType.YtelseType> ytelseTyper) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.ytelseTyper = ytelseTyper;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = false;
        this.fristPeriode = null;
    }

    // Bruk for autopunkt i 7nnn serien
    AksjonspunktDefinisjon(String kode,
                           AksjonspunktType aksjonspunktType,
                           String navn,
                           BehandlingStegType behandlingStegType,
                           VurderingspunktType vurderingspunktType,
                           VilkårType vilkårType,
                           SkjermlenkeType skjermlenkeType,
                           boolean defaultTotrinnBehandling,
                           boolean tilbakehoppVedGjenopptakelse,
                           String fristPeriode,
                           Set<FagsakYtelseType.YtelseType> ytelseTyper) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = aksjonspunktType;
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.ytelseTyper = ytelseTyper;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.fristPeriode = fristPeriode;
    }


    /**
     * @deprecated Bruk heller
     *             {@link no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder#medSkjermlenke(SkjermlenkeType)}
     *             direkte og unngå å slå opp fra aksjonspunktdefinisjon
     */
    @Deprecated
    public SkjermlenkeType getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public AksjonspunktType getAksjonspunktType() {
        return Objects.equals(AksjonspunktType.UDEFINERT, aksjonspunktType) ? null : aksjonspunktType;
    }

    public boolean erAutopunkt() {
        return AksjonspunktType.AUTOPUNKT.equals(getAksjonspunktType());
    }

    public boolean getDefaultTotrinnBehandling() {
        return defaultTotrinnBehandling;
    }

    public String getFristPeriode() {
        return fristPeriode;
    }

    public Period getFristPeriod() {
        return (fristPeriode == null ? null : Period.parse(fristPeriode));
    }

    public VilkårType getVilkårType() {
        return (Objects.equals(VilkårType.UDEFINERT, vilkårType) ? null : vilkårType);
    }

    public Set<FagsakYtelseType> getYtelseTyper() {
        return ytelseTyper == null ? Collections.emptySet() : ytelseTyper.stream().map(y -> FagsakYtelseType.fraKode(y.name())).collect(Collectors.toSet());
    }

    public boolean tilbakehoppVedGjenopptakelse() {
        return tilbakehoppVedGjenopptakelse;
    }

    /** Returnerer kode verdi for aksjonspunkt utelukket av denne. */
    public Set<String> getUtelukkendeApdef() {
        // TODO: Til vurdering UTELUKKENDE_AP_MAP.getOrDefault(kode, Set.of()).stream().map(AksjonspunktDefinisjon::getKode).collect(Collectors.toSet());
        return UTELUKKENDE_AKSJONSPUNKT.stream().filter(ap -> !Objects.equals(kode, ap)).collect(Collectors.toSet());
    }

    @Override
    public String getNavn() {
        return navn;
    }

    @Override
    public String getKode() {
        return kode;
    }

    public BehandlingStegType getBehandlingSteg() {
        return behandlingStegType;
    }

    public VurderingspunktType getVurderingspunktType() {
        return vurderingspunktType;
    }

    @Override
    public String getKodeverk() {
        return KODEVERK;
    }

    /** Aksjonspunkt tidligere brukt, nå utgått (kan ikke gjenoppstå). */
    public boolean erUtgått() {
        return erUtgått;
    }

    @Override
    public String toString() {
        return super.toString() + "('" + getKode() + "')";
    }

    public static AksjonspunktDefinisjon fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent AksjonspunktDefinisjon: " + kode);
        }
        return ad;
    }

    public static Map<String, AksjonspunktDefinisjon> kodeMap() {
        return Collections.unmodifiableMap(KODER);
    }

    public static List<AksjonspunktDefinisjon> finnAksjonspunktDefinisjoner(BehandlingStegType behandlingStegType, VurderingspunktType vurderingspunktType) {
        return KODER.values().stream()
            .filter(ad -> Objects.equals(ad.getBehandlingSteg(), behandlingStegType) && Objects.equals(ad.getVurderingspunktType(), vurderingspunktType))
            .collect(Collectors.toList());
    }

    @Converter(autoApply = true)
    public static class KodeverdiConverter implements AttributeConverter<AksjonspunktDefinisjon, String> {
        @Override
        public String convertToDatabaseColumn(AksjonspunktDefinisjon attribute) {
            return attribute == null ? null : attribute.getKode();
        }

        @Override
        public AksjonspunktDefinisjon convertToEntityAttribute(String dbData) {
            return dbData == null ? null : AksjonspunktDefinisjon.fraKode(dbData);
        }
    }
}
