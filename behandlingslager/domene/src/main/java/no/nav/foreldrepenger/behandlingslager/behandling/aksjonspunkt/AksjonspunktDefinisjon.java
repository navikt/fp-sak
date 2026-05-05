package no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.ENTRINN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.FORBLI;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.SAMME_BEHFRIST;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TILBAKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.TOTRINN;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_FRIST;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_SKJERMLENKE;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTEN_VILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon.UTVID_BEHFRIST;
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

import jakarta.persistence.EnumeratedValue;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType.YtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.DatabaseKode;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;

/**
 * Definerer mulige Aksjonspunkter inkludert hvilket Vurderingspunkt de må løses i.
 * Inkluderer også konstanter for å enklere kunne referere til dem i eksisterende logikk.
 */
public enum AksjonspunktDefinisjon implements Kodeverdi, DatabaseKode {

    // Gruppe : 500

    SJEKK_TERMINBEKREFTELSE(AksjonspunktKodeDefinisjon.SJEKK_TERMINBEKREFTELSE_KODE,
            AksjonspunktType.MANUELL, "Avklar terminbekreftelse", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN,
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.FAKTA_OM_FOEDSEL, ENTRINN, EnumSet.of(ES, FP)),
    SJEKK_MANGLENDE_FØDSEL(AksjonspunktKodeDefinisjon.SJEKK_MANGLENDE_FØDSEL_KODE,
            AksjonspunktType.MANUELL, "Sjekk manglende fødsel", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN,
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.FAKTA_OM_FOEDSEL, ENTRINN, EnumSet.of(ES, FP)),
    VURDER_OMSORGSOVERTAKELSEVILKÅRET(AksjonspunktKodeDefinisjon.VURDER_OMSORGSOVERTAKELSEVILKÅRET,
            AksjonspunktType.MANUELL, "Vurder vilkår for adopsjon eller foreldreansvar", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.OMSORGSOVERTAKELSEVILKÅR, SkjermlenkeType.FAKTA_OM_OMSORGSOVERTAKELSE, TOTRINN, EnumSet.of(ES, FP)),
    MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRISTVILKÅRET_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av søknadsfristvilkåret",
            BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR, VurderingspunktType.UT, VilkårType.SØKNADSFRISTVILKÅRET, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, EnumSet.of(ES)),
    REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD_KODE, AksjonspunktType.MANUELL, "Registrer papirsøknad engangsstønad",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FORESLÅ_VEDTAK(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Foreslå vedtak totrinn", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VEDTAK, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FATTER_VEDTAK(AksjonspunktKodeDefinisjon.FATTER_VEDTAK_KODE,
            AksjonspunktType.MANUELL, "Fatter vedtak", BehandlingStegType.FATTE_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.VEDTAK, ENTRINN,
            EnumSet.of(ES, FP, SVP)),
    AVKLAR_DEKNINGSGRAD(AksjonspunktKodeDefinisjon.AVKLAR_DEKNINGSGRAD_KODE,
        AksjonspunktType.MANUELL, "Avklar dekningsgrad", BehandlingStegType.DEKNINGSGRAD, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER, TOTRINN, EnumSet.of(FP)),
    VARSEL_REVURDERING_MANUELL( // Kun ES
            AksjonspunktKodeDefinisjon.VARSEL_REVURDERING_MANUELL_KODE, AksjonspunktType.MANUELL, "Varsel om revurdering opprettet manuelt",
            BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES)),
    FORESLÅ_VEDTAK_MANUELT(AksjonspunktKodeDefinisjon.FORESLÅ_VEDTAK_MANUELT_KODE,
            AksjonspunktType.MANUELL, "Foreslå vedtak manuelt", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.VEDTAK,
            ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_VERGE(AksjonspunktKodeDefinisjon.AVKLAR_VERGE_KODE, AksjonspunktType.MANUELL,
            "Avklar verge", BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_VERGE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE(
            AksjonspunktKodeDefinisjon.AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE_KODE, AksjonspunktType.MANUELL, "Vurdere om søkers ytelse gjelder samme barn",
            BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, TOTRINN, EnumSet.of(ES, FP)),
    VURDERE_ANNEN_YTELSE_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere annen ytelse før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDERE_DOKUMENT_FØR_VEDTAK(
            AksjonspunktKodeDefinisjon.VURDERE_DOKUMENT_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere dokument før vedtak",
            BehandlingStegType.FORESLÅ_VEDTAK,
            VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDERE_INNTEKTSMELDING_FØR_VEDTAK(
        AksjonspunktKodeDefinisjon.VURDERE_INNTEKTSMELDING_FØR_VEDTAK_KODE, AksjonspunktType.MANUELL, "Vurdere inntektsmelding før vedtak",
        BehandlingStegType.FORESLÅ_VEDTAK,
        VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(FP, SVP)),
    MANUELL_VURDERING_AV_KLAGE_NFP(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_KLAGE_NFP_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av klage (NFP)",
            BehandlingStegType.KLAGE_NFP, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.KLAGE_BEH_NFP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_INNSYN(AksjonspunktKodeDefinisjon.VURDER_INNSYN_KODE,
            AksjonspunktType.MANUELL, "Vurder innsyn", BehandlingStegType.VURDER_INNSYN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS_KODE, AksjonspunktType.MANUELL,
            "Fastsette beregningsgrunnlag for arbeidstaker/frilanser skjønnsmessig", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE(
            AksjonspunktKodeDefinisjon.VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE_KODE, AksjonspunktType.MANUELL,
            "Vurder varig endret/nyoppstartet næring selvstendig næringsdrivende", BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    REGISTRER_PAPIRSØKNAD_FORELDREPENGER(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_FORELDREPENGER_KODE, AksjonspunktType.MANUELL, "Registrer papirsøknad foreldrepenger",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_SØKNADSFRIST(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SØKNADSFRIST_KODE, AksjonspunktType.MANUELL, "Manuell vurdering av søknadsfrist",
            BehandlingStegType.SØKNADSFRIST_FORELDREPENGER, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, EnumSet.of(FP, SVP)),
    FORDEL_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.FORDEL_BEREGNINGSGRUNNLAG_KODE,
            AksjonspunktType.MANUELL, "Fordel beregningsgrunnlag", BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.FAKTA_OM_FORDELING, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for tidsbegrenset arbeidsforhold", BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT,
            VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET(
            AksjonspunktKodeDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET_KODE, AksjonspunktType.MANUELL,
            "Fastsett beregningsgrunnlag for SN som er ny i arbeidslivet", BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.BEREGNING_FORELDREPENGER, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_PERIODER_MED_OPPTJENING(
            AksjonspunktKodeDefinisjon.VURDER_PERIODER_MED_OPPTJENING_KODE, AksjonspunktType.MANUELL, "Vurder perioder med opptjening",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.INN, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.FAKTA_FOR_OPPTJENING,
            ENTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_AKTIVITETER(AksjonspunktKodeDefinisjon.AVKLAR_AKTIVITETER_KODE,
            AksjonspunktType.MANUELL, "Avklar aktivitet for beregning", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST(
            AksjonspunktKodeDefinisjon.KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE, AksjonspunktType.MANUELL,
            "Vurder varsel ved vedtak til ugunst",
            BehandlingStegType.FORESLÅ_VEDTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
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
    AVKLAR_LØPENDE_OMSORG(
            AksjonspunktKodeDefinisjon.AVKLAR_LØPENDE_OMSORG, AksjonspunktType.MANUELL, "Manuell kontroll av om bruker har omsorg",
            BehandlingStegType.FAKTA_LØPENDE_OMSORG, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_FOR_OMSORG, ENTRINN, EnumSet.of(FP)),
    AUTOMATISK_MARKERING_AV_UTENLANDSSAK(
            AksjonspunktKodeDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK_KODE, AksjonspunktType.MANUELL,
            "Innhent dokumentasjon fra utenlandsk trygdemyndighet",
            BehandlingStegType.VURDER_KOMPLETT_TIDLIG, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    FAKTA_UTTAK_INGEN_PERIODER(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_INGEN_PERIODER_KODE,
        AksjonspunktType.MANUELL, "Ingen perioder å vurdere. Vurder om behandlingen er feilopprettet og kan henlegges", BehandlingStegType.FAKTA_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_UTTAK, TOTRINN, EnumSet.of(FP)),
    FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_MANUELT_SATT_STARTDATO_ULIK_SØKNAD_STARTDATO_KODE,
        AksjonspunktType.MANUELL, "Første periode starter ikke på avklart startdato. Legg inn periode fra startdato", BehandlingStegType.FAKTA_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_UTTAK, TOTRINN, EnumSet.of(FP)),
    FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_GRADERING_UKJENT_AKTIVITET_KODE,
        AksjonspunktType.MANUELL, "Gradering av ukjent arbeidsforhold. Vurder gradering", BehandlingStegType.FAKTA_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_UTTAK, TOTRINN, EnumSet.of(FP)),
    FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.FAKTA_UTTAK_GRADERING_AKTIVITET_UTEN_BEREGNINGSGRUNNLAG_KODE,
        AksjonspunktType.MANUELL, "Gradering av aktivitet uten beregningsgrunnlag. Vurder gradering", BehandlingStegType.FAKTA_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_UTTAK, TOTRINN, EnumSet.of(FP)),
    FASTSETT_UTTAKPERIODER(AksjonspunktKodeDefinisjon.FASTSETT_UTTAKPERIODER_KODE,
            AksjonspunktType.MANUELL, "Fastsett uttaksperioder manuelt", BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP)),
    FASTSETT_UTTAK_STORTINGSREPRESENTANT(AksjonspunktKodeDefinisjon.VURDER_UTTAK_STORTINGSREPRESENTANT_KODE, AksjonspunktType.MANUELL, "Søker er stortingsrepresentant",
        BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP)),

    KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE(
            AksjonspunktKodeDefinisjon.KONTROLLER_REALITETSBEHANDLING_ELLER_KLAGE_KODE, AksjonspunktType.MANUELL, "Kontroller realitetsbehandling/klage",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_DØD(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_DØD_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om død",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST(
            AksjonspunktKodeDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE, AksjonspunktType.MANUELL, "Kontroller opplysninger om søknadsfrist",
            BehandlingStegType.VURDER_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(FP, SVP)),
    VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.VURDER_ARBEIDSFORHOLD_INNTEKTSMELDING_KODE,
        AksjonspunktType.MANUELL, "Avklar mangler rundt arbeidsforhold og inntektsmelding", BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD_INNTEKTSMELDING, VurderingspunktType.UT, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_OM_ARBEIDSFORHOLD_INNTEKTSMELDING, ENTRINN, EnumSet.of(FP, SVP)),
    VURDERING_AV_FORMKRAV_KLAGE_NFP(
            AksjonspunktKodeDefinisjon.VURDERING_AV_FORMKRAV_KLAGE_NFP_KODE, AksjonspunktType.MANUELL, "Vurder formkrav (NFP).",
            BehandlingStegType.KLAGE_VURDER_FORMKRAV_NFP, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FORMKRAV_KLAGE_NFP, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_FEILUTBETALING(AksjonspunktKodeDefinisjon.VURDER_FEILUTBETALING_KODE, AksjonspunktType.MANUELL, "Vurder feilutbetaling",
        BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    KONTROLLER_STOR_ETTERBETALING_SØKER(AksjonspunktKodeDefinisjon.KONTROLLER_STOR_ETTERBETALING_SØKER_KODE,  AksjonspunktType.MANUELL, "Kontroller stor etterbetaling til søker",
        BehandlingStegType.SIMULER_OPPDRAG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(FP, SVP)),
    AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT(
            AksjonspunktKodeDefinisjon.AVKLAR_FAKTA_ANNEN_FORELDER_HAR_RETT_KODE, AksjonspunktType.MANUELL, "Avklar annen forelder har rett",
            BehandlingStegType.KONTROLLER_OMSORG_RETT, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OMSORG_OG_RETT, ENTRINN, EnumSet.of(FP)),
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
    MANUELL_KONTROLL_AAP_KOMBINERT_ATFL(
        AksjonspunktKodeDefinisjon.MANUELL_KONTROLL_AAP_KOMBINERT_ATFL_KODE, AksjonspunktType.MANUELL, "Manuell kontroll av beregning for AAP kombinert med AT/FL",
        BehandlingStegType.VURDER_VILKAR_BERGRUNN, VurderingspunktType.UT, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, SkjermlenkeType.BEREGNING_FORELDREPENGER,
        ENTRINN, EnumSet.of(FP)),
    VURDER_SVP_TILRETTELEGGING(
            AksjonspunktKodeDefinisjon.VURDER_SVP_TILRETTELEGGING_KODE, AksjonspunktType.MANUELL, "Vurder tilrettelegging svangerskapspenger",
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.PUNKT_FOR_SVP_INNGANG, ENTRINN, EnumSet.of(ES, FP, SVP)),
    MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET(
            AksjonspunktKodeDefinisjon.MANUELL_VURDERING_AV_SVANGERSKAPSPENGERVILKÅRET_KODE, AksjonspunktType.MANUELL, "Avklar svangerskapspengervilkåret",
            BehandlingStegType.VURDER_SVANGERSKAPSPENGERVILKÅR, VurderingspunktType.UT, VilkårType.SVANGERSKAPSPENGERVILKÅR,
            SkjermlenkeType.PUNKT_FOR_SVANGERSKAPSPENGER, ENTRINN, EnumSet.of(SVP)),
    VURDER_FARESIGNALER(AksjonspunktKodeDefinisjon.VURDER_FARESIGNALER_KODE,
            AksjonspunktType.MANUELL, "Vurder Faresignaler", BehandlingStegType.VURDER_FARESIGNALER, VurderingspunktType.UT, UTEN_VILKÅR,
            SkjermlenkeType.VURDER_FARESIGNALER, TOTRINN, EnumSet.of(ES, FP, SVP)),
    REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER(
            AksjonspunktKodeDefinisjon.REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER_KODE, AksjonspunktType.MANUELL, "Registrer papirsøknad svangerskapspenger",
            BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, EnumSet.of(ES, FP, SVP)),
    VURDER_UTTAK_DOKUMENTASJON(
        AksjonspunktKodeDefinisjon.VURDER_UTTAK_DOKUMENTASJON_KODE, AksjonspunktType.MANUELL, "Vurder uttaksdokumentasjon",
        BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_UTTAK_DOKUMENTASJON, TOTRINN, EnumSet.of(FP)),
    VURDER_MEDLEMSKAPSVILKÅRET(
        AksjonspunktKodeDefinisjon.VURDER_MEDLEMSKAPSVILKÅRET, AksjonspunktType.MANUELL, "Manuell vurdering av medlemskapsvilkåret",
        BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP,
        TOTRINN, EnumSet.of(FP, SVP, ES)),
    VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR(
        AksjonspunktKodeDefinisjon.VURDER_FORUTGÅENDE_MEDLEMSKAPSVILKÅR, AksjonspunktType.MANUELL, "Manuell vurdering av forutgående medlemskapsvilkår",
        BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SkjermlenkeType.FAKTA_OM_MEDLEMSKAP,
        TOTRINN, EnumSet.of(ES)),
    AVKLAR_UTTAK_I_EØS_FOR_ANNENPART(
        AksjonspunktKodeDefinisjon.AVKLAR_UTTAK_I_EØS_FOR_ANNENPART_KODE, AksjonspunktType.MANUELL, "Avklar uttak i EØS for annen forelder",
        BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_UTTAK_EØS, TOTRINN, EnumSet.of(FP)),

    // Gruppe : 600

    SØKERS_OPPLYSNINGSPLIKT_OVST(AksjonspunktKodeDefinisjon.SØKERS_OPPLYSNINGSPLIKT_OVST_KODE, AksjonspunktType.SAKSBEHANDLEROVERSTYRING,
            "Saksbehandler initierer kontroll av søkers opplysningsplikt", BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT, VurderingspunktType.UT,
        VilkårType.SØKERSOPPLYSNINGSPLIKT, SkjermlenkeType.OPPLYSNINGSPLIKT, ENTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_FØDSELSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av fødselsvilkåret", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.FØDSELSVILKÅRET_MOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL, TOTRINN, EnumSet.of(ES, FP)),
    OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av medlemskapsvilkåret",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP,
            TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_SØKNADSFRISTVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av søknadsfristvilkåret",
            BehandlingStegType.VURDER_SØKNADSFRISTVILKÅR, VurderingspunktType.UT, VilkårType.SØKNADSFRISTVILKÅRET, SkjermlenkeType.SOEKNADSFRIST, TOTRINN, EnumSet.of(ES)),
    OVERSTYRING_AV_UTTAKPERIODER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_UTTAKPERIODER_KODE, AksjonspunktType.OVERSTYRING, "Overstyr uttaksperioder",
            BehandlingStegType.BEREGN_YTELSE,
            VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.UTTAK, TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FØDSELSVILKÅRET_FAR_MEDMOR_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av fødselsvilkåret for far/medmor", BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.UT,
            VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR, SkjermlenkeType.PUNKT_FOR_FOEDSEL, TOTRINN, EnumSet.of(ES, FP)),
    OVERSTYRING_AV_OPPTJENINGSVILKÅRET(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_OPPTJENINGSVILKÅRET_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av opptjeningsvilkåret",
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR, VurderingspunktType.UT, VilkårType.OPPTJENINGSVILKÅRET, SkjermlenkeType.PUNKT_FOR_OPPTJENING,
            TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_FAKTA_UTTAK(AksjonspunktKodeDefinisjon.OVERSTYRING_FAKTA_UTTAK_KODE, AksjonspunktType.OVERSTYRING,
        "Overstyr fakta om uttak", BehandlingStegType.FAKTA_UTTAK, VurderingspunktType.UT,
        UTEN_VILKÅR, SkjermlenkeType.FAKTA_UTTAK, TOTRINN, EnumSet.of(FP)),
    OVERSTYRING_AV_BEREGNINGSAKTIVITETER(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSAKTIVITETER_KODE, AksjonspunktType.OVERSTYRING,
            "Overstyring av beregningsaktiviteter", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT,
            UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_BEREGNINGSGRUNNLAG(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG_KODE, AksjonspunktType.OVERSTYRING, "Overstyring av beregningsgrunnlag",
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_BEREGNING, TOTRINN, EnumSet.of(FP, SVP)),
    OVERSTYRING_AV_AVKLART_STARTDATO(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_AVKLART_STARTDATO_KODE, AksjonspunktType.MANUELL, "Overstyr avklart startdato for foreldrepengeperioden",
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR, VurderingspunktType.INN, VilkårType.MEDLEMSKAPSVILKÅRET, SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER,
            TOTRINN, EnumSet.of(ES, FP, SVP)),
    OVERSTYRING_AV_DEKNINGSGRAD(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_DEKNINGSGRAD_KODE, AksjonspunktType.OVERSTYRING, "Overstyr dekningsgrad",
        BehandlingStegType.DEKNINGSGRAD, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.KONTROLL_AV_SAKSOPPLYSNINGER,
        TOTRINN, EnumSet.of(FP)),
    OVERSTYRING_AV_RETT_OG_OMSORG(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_RETT_OG_OMSORG_KODE, AksjonspunktType.OVERSTYRING, "Overstyr rett og omsorg",
        BehandlingStegType.KONTROLLER_OMSORG_RETT, VurderingspunktType.UT, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OMSORG_OG_RETT,
        TOTRINN, EnumSet.of(FP)),
    OVERSTYRING_AV_FAKTA_OM_FØDSEL(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FAKTA_OM_FØDSEL_KODE, AksjonspunktType.OVERSTYRING, "Overstyr fakta om fødsel",
        BehandlingStegType.SØKERS_RELASJON_TIL_BARN, VurderingspunktType.INN, UTEN_VILKÅR, SkjermlenkeType.FAKTA_OM_FOEDSEL,
        TOTRINN, EnumSet.of(ES, FP)),

    OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_FORUTGÅENDE_MEDLEMSKAPSVILKÅR_KODE,
        AksjonspunktType.OVERSTYRING, "Overstyring av vilkår forutgående medlemskap", BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
        VurderingspunktType.UT, VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE, SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP, TOTRINN, EnumSet.of(ES)),
    OVERSTYRING_AV_UTTAK_I_EØS_FOR_ANNENPART(AksjonspunktKodeDefinisjon.OVERSTYRING_UTTAK_I_EØS_FOR_ANNENPART_KODE, AksjonspunktType.OVERSTYRING,
        "Overstyr uttak i EØS for annen forelder", BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON, VurderingspunktType.INN, UTEN_VILKÅR,
        SkjermlenkeType.FAKTA_UTTAK_EØS, TOTRINN, EnumSet.of(FP)),

    // Gruppe : 700

    AUTO_MANUELT_SATT_PÅ_VENT(AksjonspunktKodeDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT_KODE, AksjonspunktType.AUTOPUNKT,
            "Manuelt satt på vent", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            FORBLI, Period.ofWeeks(4), SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_FØDSELREGISTRERING(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_FØDSELREGISTRERING_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på fødsel ved avklaring av søkers relasjon til barnet", BehandlingStegType.KONTROLLER_FAKTA, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
            TILBAKE, UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENTER_PÅ_KOMPLETT_SØKNAD(AksjonspunktKodeDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT,
            "Venter på komplett søknad", BehandlingStegType.VURDER_KOMPLETT_BEH, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, FORBLI,
            Period.ofWeeks(4), SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_SATT_PÅ_VENT_REVURDERING(AksjonspunktKodeDefinisjon.AUTO_SATT_PÅ_VENT_REVURDERING_KODE, AksjonspunktType.AUTOPUNKT,
            "Satt på vent etter varsel om revurdering", BehandlingStegType.VARSEL_REVURDERING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE,
            ENTRINN, FORBLI, Period.ofWeeks(4), SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    VENT_PÅ_SCANNING(AksjonspunktKodeDefinisjon.VENT_PÅ_SCANNING_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på scanning", BehandlingStegType.VURDER_INNSYN, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
        Period.ofDays(3), SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    VENT_PGA_FOR_TIDLIG_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD_KODE, AksjonspunktType.AUTOPUNKT, "Satt på vent pga for tidlig søknad",
            BehandlingStegType.VURDER_KOMPLETT_TIDLIG, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            UTEN_FRIST, UTVID_BEHFRIST, EnumSet.of(ES, FP, SVP)),

    AUTO_KØET_BEHANDLING(AksjonspunktKodeDefinisjon.AUTO_KØET_BEHANDLING_KODE,
            AksjonspunktType.AUTOPUNKT, "Autokøet behandling", BehandlingStegType.INNGANG_UTTAK, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN,
        FORBLI, UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(FP)),
    VENT_PÅ_SØKNAD(AksjonspunktKodeDefinisjon.VENT_PÅ_SØKNAD_KODE,
            AksjonspunktType.AUTOPUNKT, "Venter på søknad", BehandlingStegType.REGISTRER_SØKNAD, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            Period.ofWeeks(3), UTVID_BEHFRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE, AksjonspunktType.AUTOPUNKT, "Vent på rapporteringsfrist for inntekt",
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE,
            UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SISTE_AAP_ELLER_DP_MELDEKORT_KODE, AksjonspunktType.AUTOPUNKT,
            "Vent på siste meldekort for AAP eller DP-mottaker", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
            UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_ETTERLYST_INNTEKTSMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE, AksjonspunktType.AUTOPUNKT, "Vent på etterlyst inntektsmelding",
            BehandlingStegType.INREG_AVSL, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, Period.ofWeeks(3), SAMME_BEHFRIST, EnumSet.of(FP, SVP)),
    AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN(AksjonspunktKodeDefinisjon.AUTO_VENT_ANKE_OVERSENDT_TIL_TRYGDERETTEN_KODE, AksjonspunktType.AUTOPUNKT, "Autopunkt anke oversendt til Trygderetten",
            BehandlingStegType.ANKE_MERKNADER, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, Period.ofYears(2),
            UTVID_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_SYKEMELDING(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_SYKEMELDING_KODE, AksjonspunktType.AUTOPUNKT,
        "Vent på siste sykemelding fra søker som mottar sykepenger basert på dagpenger", BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, VurderingspunktType.UT, UTEN_VILKÅR,
        UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(FP)),
    AUTO_VENT_PÅ_KABAL_KLAGE(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_KABAL_KLAGE_KODE, AksjonspunktType.AUTOPUNKT, "Vent på klagebehandling hos Nav klageinstans",
        BehandlingStegType.KLAGE_NK, VurderingspunktType.INN, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),
    AUTO_VENT_PÅ_KABAL_ANKE(AksjonspunktKodeDefinisjon.AUTO_VENT_PÅ_KABAL_ANKE_KODE, AksjonspunktType.AUTOPUNKT, "Vent på ankebehandling hos Nav klageinstans",
        BehandlingStegType.ANKE, VurderingspunktType.UT, UTEN_VILKÅR, UTEN_SKJERMLENKE, ENTRINN, TILBAKE, UTEN_FRIST, SAMME_BEHFRIST, EnumSet.of(ES, FP, SVP)),

    // Utgåtte aksjonspunktkoder - kun her for bakoverkompatibilitet. Finnes historisk i databasen til fpsak i PROD !
    @Deprecated
    UTGÅTT_5004("5004", AksjonspunktType.MANUELL, "Avklar adopsjonsdokumentasjon"),
    @Deprecated
    UTGÅTT_5005("5005", AksjonspunktType.MANUELL, "Avklar om adopsjon gjelder ektefelles barn"),
    @Deprecated
    UTGÅTT_5006("5006", AksjonspunktType.MANUELL, "Avklar om søker er mann adopterer alene"),
    @Deprecated
    UTGÅTT_5008("5008", AksjonspunktType.MANUELL, "Avklar fakta for omsorgs/foreldreansvarsvilkåret"),
    @Deprecated
    UTGÅTT_5009("5009", AksjonspunktType.MANUELL, "Avklar tilleggsopplysninger"),
    @Deprecated
    UTGÅTT_5011("5011", AksjonspunktType.MANUELL, "Manuell vurdering av omsorgsvilkåret"),
    @Deprecated
    UTGÅTT_5013("5013", AksjonspunktType.MANUELL, "Manuell vurdering av foreldreansvarsvilkåret andre ledd"),
    @Deprecated
    UTGÅTT_5014("5014", AksjonspunktType.MANUELL, "Manuell vurdering av foreldreansvarsvilkåret fjerde ledd"),
    @Deprecated
    UTGÅTT_5017("5017", AksjonspunktType.MANUELL, "Vurder søkers opplysningsplikt ved ufullstendig/ikke-komplett søknad"),
    @Deprecated
    UTGÅTT_5019("5019", AksjonspunktType.MANUELL, "Avklar lovlig opphold."),
    @Deprecated
    UTGÅTT_5020("5020", AksjonspunktType.MANUELL, "Avklar om bruker er bosatt."),
    @Deprecated
    UTGÅTT_5021("5021", AksjonspunktType.MANUELL, "Avklar om bruker har gyldig periode."),
    @Deprecated
    UTGÅTT_5022("5022", AksjonspunktType.MANUELL, "Avklar fakta for status på person."),
    @Deprecated
    UTGÅTT_5023("5023", AksjonspunktType.MANUELL, "Avklar oppholdsrett."),
    @Deprecated
    UTGÅTT_5024("5024", AksjonspunktType.MANUELL, "Saksbehandler må avklare hvilke verdier som er gjeldene, det er mismatch mellom register- og lokaldata (UTGÅTT)"),
    @Deprecated
    UTGÅTT_5025("5025", AksjonspunktType.MANUELL, "Varsel om revurdering ved automatisk etterkontroll"),
    @Deprecated
    UTGÅTT_5032("5032", AksjonspunktType.MANUELL, "Vurdere om annen forelder sin ytelse gjelder samme barn"),
    @Deprecated
    UTGÅTT_5036("5036", AksjonspunktType.MANUELL, "Manuell vurdering av klage (NK)"),
    @Deprecated // Håndteres nå sammen med 5039
    UTGÅTT_5042("5042", AksjonspunktType.MANUELL, "Fastsett beregningsgrunnlag for selvstendig næringsdrivende"),
    @Deprecated
    UTGÅTT_5044("5044", AksjonspunktType.MANUELL, "Vurder om vilkår for sykdom er oppfylt"),
    @Deprecated
    UTGÅTT_5045("5045", AksjonspunktType.MANUELL, "Avklar startdato for foreldrepengeperioden"),
    @Deprecated // Erstattet av aksjonspunkt 5062
    UTGÅTT_5048("5048", AksjonspunktType.MANUELL, "Kontroller den automatiske besteberegningen."),
    @Deprecated
    UTGÅTT_5050("5050", AksjonspunktType.MANUELL, "Vurder gradering på andel uten beregningsgrunnlag"),
    @Deprecated
    UTGÅTT_5053("5053", AksjonspunktType.MANUELL, "Avklar fortsatt medlemskap."),
    @Deprecated
    UTGÅTT_5054("5054", AksjonspunktType.MANUELL, "Avklar fakta for foreldreansvarsvilkåret for FP"),
    @Deprecated
    UTGÅTT_5056("5056", AksjonspunktType.MANUELL, "Kontroll av manuelt opprettet revurderingsbehandling"),
    @Deprecated
    UTGÅTT_5067("5067", AksjonspunktType.MANUELL, "Bruker har minsterett ifm tette saker og uttak etter start av ny sak"),
    @Deprecated
    UTGÅTT_5069("5069", AksjonspunktType.MANUELL, "Kontroller annen forelders uttak i EØS"),
    @Deprecated
    UTGÅTT_5070("5070", AksjonspunktType.MANUELL, "Kontrollerer søknadsperioder"),
    @Deprecated
    UTGÅTT_5075("5075", AksjonspunktType.MANUELL, "Kontroller opplysninger om fordeling av stønadsperioden"),
    @Deprecated
    UTGÅTT_5078("5078", AksjonspunktType.MANUELL, "Kontroller tilstøtende ytelser innvilget"),
    @Deprecated
    UTGÅTT_5079("5079", AksjonspunktType.MANUELL, "Kontroller tilstøtende ytelser opphørt"),
    @Deprecated // Erstattet av 5085
    UTGÅTT_5080("5080", AksjonspunktType.MANUELL, "Avklar arbeidsforhold"),
    @Deprecated
    UTGÅTT_5081("5081", AksjonspunktType.MANUELL, "Avklar første uttaksdato"),
    @Deprecated
    UTGÅTT_5083("5083", AksjonspunktType.MANUELL, "Vurder formkrav (NK)."),
    @Deprecated
    UTGÅTT_5087("5087", AksjonspunktType.MANUELL, "Vurder Dekningsgrad"),
    @Deprecated
    UTGÅTT_5088("5088", AksjonspunktType.MANUELL, "Oppgitt at annen forelder ikke rett, men har løpende utbetaling"),
    @Deprecated
    UTGÅTT_5090("5090", AksjonspunktType.MANUELL, "Vurder tilbaketrekk"),
    @Deprecated
    UTGÅTT_5093("5093", AksjonspunktType.MANUELL, "Manuell vurdering av anke"),
    @Deprecated
    UTGÅTT_5094("5094", AksjonspunktType.MANUELL, "Manuell vurdering av anke merknader"),
    @Deprecated
    UTGÅTT_5097("5097", AksjonspunktType.MANUELL, "Gradering i søknadsperiode er lagt på ukjent aktivitet"),
    @Deprecated
    UTGÅTT_5098("5098", AksjonspunktType.MANUELL, "Gradering i søknadsperiode er lagt på aktivitet uten beregningsgrunnlag"),
    @Deprecated
    UTGÅTT_5099("5099", AksjonspunktType.MANUELL, "Kontroller aktivitetskrav"),
    @Deprecated
    UTGÅTT_6004("6004", AksjonspunktType.OVERSTYRING, "Overstyring av adopsjonsvilkåret"),
    @Deprecated
    UTGÅTT_6007("6007", AksjonspunktType.OVERSTYRING, "Overstyring av beregning"),
    @Deprecated
    UTGÅTT_6010("6010", AksjonspunktType.OVERSTYRING, "Overstyring av adopsjonsvilkåret for foreldrepenger"),
    @Deprecated
    UTGÅTT_6012("6012", AksjonspunktType.OVERSTYRING, "Overstyring av løpende medlemskapsvilkåret"),
    @Deprecated
    UTGÅTT_6013("6013", AksjonspunktType.OVERSTYRING, "Overstyr søknadsperioder"),
    @Deprecated
    UTGÅTT_6068("6068", AksjonspunktType.MANUELL, "Manuell markering av utenlandssak"),
    @Deprecated
    UTGÅTT_6070("6070", AksjonspunktType.OVERSTYRING, "Saksbehandler endret søknadsperioder uten aksjonspunkt"),
    @Deprecated
    UTGÅTT_7004("7004", AksjonspunktType.AUTOPUNKT, "Vent på fødsel ved avklaring av medlemskap"),
    @Deprecated
    UTGÅTT_7006("7006", AksjonspunktType.AUTOPUNKT, "Venter på opptjeningsopplysninger"),
    @Deprecated
    UTGÅTT_7009("7009", AksjonspunktType.AUTOPUNKT, "Vent på oppdatering som passerer kompletthetssjekk"),
    @Deprecated
    UTGÅTT_7015("7015", AksjonspunktType.AUTOPUNKT, "Venter på regler for 80% dekningsgrad (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7016("7016", AksjonspunktType.AUTOPUNKT, "Opprettes når opptjeningsvilkåret blir automatisk avslått. NB! Autopunkt som er innført til prodfeil på opptjenig er fikset (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7017("7017", AksjonspunktType.AUTOPUNKT, "Sett på vent - ventelønn/vartpenger og militær med flere aktiviteter (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7018("7018", AksjonspunktType.AUTOPUNKT, "Autopunkt dødfødsel 80% dekningsgrad."),
    @Deprecated
    UTGÅTT_7019("7019", AksjonspunktType.AUTOPUNKT, "Autopunkt gradering uten beregningsgrunnlag."),
    @Deprecated
    UTGÅTT_7021("7021", AksjonspunktType.AUTOPUNKT, "Endring i fordeling av ytelse bakover i tid (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7022("7022", AksjonspunktType.AUTOPUNKT, "Autopunkt vent på ny inntektsmelding med gyldig arbeidsforholdId."),
    @Deprecated
    UTGÅTT_7023("7023", AksjonspunktType.AUTOPUNKT, "Autopunkt militær i opptjeningsperioden og beregninggrunnlag under 3G."),
    @Deprecated
    UTGÅTT_7024("7024", AksjonspunktType.AUTOPUNKT, "Sett på vent - Arbeidsgiver krever refusjon 3 måneder tilbake i tid (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7025("7025", AksjonspunktType.AUTOPUNKT, "Autopunkt gradering flere arbeidsforhold."),
    @Deprecated
    UTGÅTT_7026("7026", AksjonspunktType.AUTOPUNKT, "Autopunkt vent på ulike startdatoer i SVP."),
    @Deprecated
    UTGÅTT_7027("7027", AksjonspunktType.AUTOPUNKT, "Autopunkt vent delvis tilrettelegging og refusjon SVP."),
    @Deprecated
    UTGÅTT_7028("7028", AksjonspunktType.AUTOPUNKT, "Sett på vent - Søker har søkt SVP og hatt AAP eller DP siste 10 mnd (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7029("7029", AksjonspunktType.AUTOPUNKT, "Sett på vent - Støtter ikke FL/SN i svangerskapspenger (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7032("7032", AksjonspunktType.AUTOPUNKT, "Autopunkt anke venter på merknader fra bruker (UTGÅTT)"),
    @Deprecated
    UTGÅTT_7034("7034", AksjonspunktType.AUTOPUNKT, "Autopunkt flere arbeidsforhold i samme virksomhet SVP"),
    @Deprecated
    UTGÅTT_7035("7035", AksjonspunktType.AUTOPUNKT, "Autopunkt potensielt feil i endringssøknad, kontakt bruker"),
    @Deprecated
    UTGÅTT_7036("7036", AksjonspunktType.AUTOPUNKT, "Autopunkt vent manglende arbeidsforhold ifm kommunereform 2020."),
    @Deprecated
    UTGÅTT_7038("7038", AksjonspunktType.AUTOPUNKT, "Vent på korrigering / feilretting av besteberegningen."),
    @Deprecated
    UTGÅTT_7041("7041", AksjonspunktType.AUTOPUNKT, "Vent på vedtak om lovendring vedrørende beregning av næring i kombinasjon med arbeid eller frilans"),
    ;

    private static final Map<String, AksjonspunktDefinisjon> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    private static final Set<AksjonspunktDefinisjon> DYNAMISK_SKJERMLENKE = Set.of(AVKLAR_OM_SØKER_HAR_MOTTATT_STØTTE);

    private static final Set<AksjonspunktDefinisjon> FORESLÅ_VEDTAK_AP = Set.of(FORESLÅ_VEDTAK, FORESLÅ_VEDTAK_MANUELT);

    private static final Set<AksjonspunktDefinisjon> PAPIRSØKNAD_AP = Set.of(REGISTRER_PAPIRSØKNAD_ENGANGSSTØNAD,
        REGISTRER_PAPIRSØKNAD_FORELDREPENGER, REGISTRER_PAPIRSØKNAD_SVANGERSKAPSPENGER, REGISTRER_PAPIR_ENDRINGSØKNAD_FORELDREPENGER);

    private static final Set<AksjonspunktDefinisjon> AVVIK_I_BEREGNING = Set.of(FASTSETT_BEREGNINGSGRUNNLAG_ARBEIDSTAKER_FRILANS,
        FASTSETT_BEREGNINGSGRUNNLAG_FOR_SN_NY_I_ARBEIDSLIVET, FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD,
        VURDER_VARIG_ENDRET_ELLER_NYOPPSTARTET_NÆRING_SELVSTENDIG_NÆRINGSDRIVENDE);

    private static final Set<AksjonspunktDefinisjon> IKKE_KLAR_FOR_INNTEKTSMELDING = Set.of(VENT_PGA_FOR_TIDLIG_SØKNAD,
        VENT_PÅ_SØKNAD, REGISTRER_PAPIRSØKNAD_FORELDREPENGER);

    private static final Map<AksjonspunktDefinisjon, Set<AksjonspunktDefinisjon>> UTELUKKENDE_AP_MAP = Map.ofEntries(
        Map.entry(SJEKK_MANGLENDE_FØDSEL, Set.of(SJEKK_TERMINBEKREFTELSE)),
        Map.entry(SJEKK_TERMINBEKREFTELSE, Set.of(SJEKK_MANGLENDE_FØDSEL))
        /* TODO: Vurder om disse skal tas med
        , Map.entry(FORESLÅ_VEDTAK, Set.of(FORESLÅ_VEDTAK_MANUELT))
        , Map.entry(FORESLÅ_VEDTAK_MANUELT, Set.of(FORESLÅ_VEDTAK))
         */
    );

    private AksjonspunktType aksjonspunktType;

    /**
     * Definerer hvorvidt Aksjonspunktet default krever totrinnsbehandling. Dvs. Beslutter må godkjenne hva Saksbehandler har utført.
     */
    private boolean defaultTotrinnBehandling = false;

    /**
     * Hvorvidt aksjonspunktet har en frist før det må være løst. Brukes i forbindelse med når Behandling er lagt til Vent.
     */
    private Period fristPeriode;

    private boolean utviderBehandlingsfrist;

    private VilkårType vilkårType;

    private SkjermlenkeType skjermlenkeType;

    private boolean tilbakehoppVedGjenopptakelse;

    private BehandlingStegType behandlingStegType;

    private String navn;

    private Set<YtelseType> ytelseTyper;

    private VurderingspunktType vurderingspunktType;

    private boolean erUtgått = false;

    @JsonValue
    @EnumeratedValue
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
    AksjonspunktDefinisjon(String kode,                        // NOSONAR
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
        this.aksjonspunktType = Objects.requireNonNull(aksjonspunktType);
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
    AksjonspunktDefinisjon(String kode,                        // NOSONAR
                           AksjonspunktType aksjonspunktType,
                           String navn,
                           BehandlingStegType behandlingStegType,
                           VurderingspunktType vurderingspunktType,
                           VilkårType vilkårType,
                           SkjermlenkeType skjermlenkeType,
                           boolean defaultTotrinnBehandling,
                           boolean tilbakehoppVedGjenopptakelse,
                           Period fristPeriode,
                           boolean utviderBehandlingsfrist,
                           Set<FagsakYtelseType.YtelseType> ytelseTyper) {
        this.kode = Objects.requireNonNull(kode);
        this.navn = navn;
        this.aksjonspunktType = Objects.requireNonNull(aksjonspunktType);
        this.behandlingStegType = behandlingStegType;
        this.vurderingspunktType = vurderingspunktType;
        this.ytelseTyper = ytelseTyper;
        this.vilkårType = vilkårType;
        this.defaultTotrinnBehandling = defaultTotrinnBehandling;
        this.skjermlenkeType = skjermlenkeType;
        this.tilbakehoppVedGjenopptakelse = tilbakehoppVedGjenopptakelse;
        this.fristPeriode = fristPeriode;
        this.utviderBehandlingsfrist = utviderBehandlingsfrist;
    }

    /**
     * @deprecated Bruk heller
     *             {@link Historikkinnslag.Builder#medTittel(SkjermlenkeType)}
     *             direkte og unngå å slå opp fra aksjonspunktdefinisjon
     */
    @Deprecated
    public SkjermlenkeType getSkjermlenkeType() {
        return skjermlenkeType;
    }

    public AksjonspunktType getAksjonspunktType() {
        return aksjonspunktType;
    }

    public boolean erAutopunkt() {
        return AksjonspunktType.AUTOPUNKT.equals(getAksjonspunktType());
    }

    public boolean getDefaultTotrinnBehandling() {
        return defaultTotrinnBehandling;
    }

    public boolean kanSetteTotrinnBehandling() {
        return SkjermlenkeType.totrinnsSkjermlenke(skjermlenkeType) || DYNAMISK_SKJERMLENKE.contains(this);
    }

    public Period getFristPeriod() {
        return fristPeriode;
    }

    public boolean utviderBehandlingsfrist() {
        return utviderBehandlingsfrist;
    }

    public VilkårType getVilkårType() {
        return vilkårType;
    }

    public Set<FagsakYtelseType> getYtelseTyper() {
        return ytelseTyper == null ? Collections.emptySet() : ytelseTyper.stream().map(y -> FagsakYtelseType.fraKode(y.name())).collect(Collectors.toSet());
    }

    public boolean tilbakehoppVedGjenopptakelse() {
        return tilbakehoppVedGjenopptakelse;
    }

    /** Returnerer kode verdi for aksjonspunkt utelukket av denne. */
    public Set<AksjonspunktDefinisjon> getUtelukkendeApdef() {
        return UTELUKKENDE_AP_MAP.getOrDefault(this, Set.of());
    }

    public static Set<AksjonspunktDefinisjon> getForeslåVedtakAksjonspunkter() {
        return FORESLÅ_VEDTAK_AP;
    }

    public static Set<AksjonspunktDefinisjon> getAvvikIBeregning() {
        return AVVIK_I_BEREGNING;
    }

    public static Set<AksjonspunktDefinisjon> getIkkeKlarForInntektsmelding() {
        return IKKE_KLAR_FOR_INNTEKTSMELDING;
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

    /** Aksjonspunkt tidligere brukt, nå utgått (kan ikke gjenoppstå). */
    public boolean erUtgått() {
        return erUtgått;
    }

    public boolean erPapirsøknadAksjonspunkt() {
        return PAPIRSØKNAD_AP.contains(this);
    }

    public boolean avbrytVedTilbakeføring() {
        return FATTER_VEDTAK.equals(this);
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

    public static List<AksjonspunktDefinisjon> finnAksjonspunktDefinisjoner(BehandlingStegType behandlingStegType, VurderingspunktType vurderingspunktType) {
        return KODER.values().stream()
            .filter(ad -> Objects.equals(ad.getBehandlingSteg(), behandlingStegType) && Objects.equals(ad.getVurderingspunktType(), vurderingspunktType))
            .toList();
    }
}
