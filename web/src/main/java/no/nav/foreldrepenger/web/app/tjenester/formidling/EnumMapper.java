package no.nav.foreldrepenger.web.app.tjenester.formidling;

import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Behandlingsresultat;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.BehandlingÅrsakType;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Foreldrepenger;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.InnsynBehandling;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.KlageBehandling;
import static no.nav.foreldrepenger.web.app.tjenester.formidling.BrevGrunnlagDto.Rettigheter;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.innsyn.InnsynResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.klage.KlageAvvistÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.SaldoerDto;

final class EnumMapper {

    private EnumMapper() {
    }

    static Foreldrepenger.Stønadskonto.Type mapStønadskontoType(SaldoerDto.SaldoVisningStønadskontoType stønadskontotype) {
        return switch (stønadskontotype) {
            case MØDREKVOTE -> Foreldrepenger.Stønadskonto.Type.MØDREKVOTE;
            case FEDREKVOTE -> Foreldrepenger.Stønadskonto.Type.FEDREKVOTE;
            case FELLESPERIODE -> Foreldrepenger.Stønadskonto.Type.FELLESPERIODE;
            case FORELDREPENGER -> Foreldrepenger.Stønadskonto.Type.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> Foreldrepenger.Stønadskonto.Type.FORELDREPENGER_FØR_FØDSEL;
            case FLERBARNSDAGER -> Foreldrepenger.Stønadskonto.Type.FLERBARNSDAGER;
            case UTEN_AKTIVITETSKRAV -> Foreldrepenger.Stønadskonto.Type.UTEN_AKTIVITETSKRAV;
            case MINSTERETT_NESTE_STØNADSPERIODE -> Foreldrepenger.Stønadskonto.Type.MINSTERETT_NESTE_STØNADSPERIODE;
            case MINSTERETT -> Foreldrepenger.Stønadskonto.Type.MINSTERETT;
        };
    }

    static BrevGrunnlagDto.PeriodeResultatType mapPeriodeResultatType(PeriodeResultatType resultatType) {
        return switch (resultatType) {
            case INNVILGET -> BrevGrunnlagDto.PeriodeResultatType.INNVILGET;
            case AVSLÅTT -> BrevGrunnlagDto.PeriodeResultatType.AVSLÅTT;
            case MANUELL_BEHANDLING -> throw new IllegalStateException("Unexpected value: " + resultatType);
        };
    }

    static Foreldrepenger.TrekkontoType mapTrekkontoType(UttakPeriodeType trekkonto) {
        return switch (trekkonto) {
            case FELLESPERIODE -> Foreldrepenger.TrekkontoType.FELLESPERIODE;
            case MØDREKVOTE -> Foreldrepenger.TrekkontoType.MØDREKVOTE;
            case FEDREKVOTE -> Foreldrepenger.TrekkontoType.FEDREKVOTE;
            case FORELDREPENGER -> Foreldrepenger.TrekkontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> Foreldrepenger.TrekkontoType.FORELDREPENGER_FØR_FØDSEL;
            case UDEFINERT -> Foreldrepenger.TrekkontoType.UDEFINERT;
        };
    }

    static BrevGrunnlagDto.UttakArbeidType mapSvpUttakArbeidType(UttakArbeidType uttakArbeidType) {
        return switch (uttakArbeidType) {
            case ORDINÆRT_ARBEID -> BrevGrunnlagDto.UttakArbeidType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> BrevGrunnlagDto.UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> BrevGrunnlagDto.UttakArbeidType.FRILANS;
            case ANNET -> BrevGrunnlagDto.UttakArbeidType.ANNET;
        };
    }

    static InnsynBehandling.InnsynResultatType mapInnsynResultatType(InnsynResultatType innsynResultatType) {
        return switch (innsynResultatType) {
            case INNVILGET -> InnsynBehandling.InnsynResultatType.INNVILGET;
            case DELVIS_INNVILGET -> InnsynBehandling.InnsynResultatType.DELVIS_INNVILGET;
            case AVVIST -> InnsynBehandling.InnsynResultatType.AVVIST;
            case UDEFINERT -> InnsynBehandling.InnsynResultatType.UDEFINERT;
        };
    }

    static KlageBehandling.KlageAvvistÅrsak mapKlageAvvistÅrsak(KlageAvvistÅrsak klageAvvistÅrsak) {
        return switch (klageAvvistÅrsak) {
            case KLAGET_FOR_SENT -> KlageBehandling.KlageAvvistÅrsak.KLAGET_FOR_SENT;
            case KLAGE_UGYLDIG -> KlageBehandling.KlageAvvistÅrsak.KLAGE_UGYLDIG;
            case IKKE_PAKLAGD_VEDTAK -> KlageBehandling.KlageAvvistÅrsak.IKKE_PÅKLAGD_VEDTAK;
            case KLAGER_IKKE_PART -> KlageBehandling.KlageAvvistÅrsak.KLAGER_IKKE_PART;
            case IKKE_KONKRET -> KlageBehandling.KlageAvvistÅrsak.IKKE_KONKRET;
            case IKKE_SIGNERT -> KlageBehandling.KlageAvvistÅrsak.IKKE_SIGNERT;
            case UDEFINERT -> KlageBehandling.KlageAvvistÅrsak.UDEFINERT;
        };
    }

    static BrevGrunnlagDto.RelasjonsRolleType mapRelasjonsRolleType(RelasjonsRolleType relasjonsRolleType) {
        return switch (relasjonsRolleType) {
            case FARA -> BrevGrunnlagDto.RelasjonsRolleType.FARA;
            case MORA -> BrevGrunnlagDto.RelasjonsRolleType.MORA;
            case MEDMOR -> BrevGrunnlagDto.RelasjonsRolleType.MEDMOR;
            case UDEFINERT -> null;
            case BARN, EKTE, REGISTRERT_PARTNER, ANNEN_PART_FRA_SØKNAD ->
                throw new IllegalStateException("Unexpected value: " + relasjonsRolleType);
        };
    }

    static BrevGrunnlagDto.FagsakYtelseType mapFagsakYtelseType(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case ENGANGSTØNAD -> BrevGrunnlagDto.FagsakYtelseType.ENGANGSTØNAD;
            case FORELDREPENGER -> BrevGrunnlagDto.FagsakYtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> BrevGrunnlagDto.FagsakYtelseType.SVANGERSKAPSPENGER;
            case UDEFINERT -> throw new IllegalStateException("Udefinert ytelse på fagsak");
        };
    }

    static BehandlingÅrsakType mapBehandlingÅrsakType(BehandlingÅrsak behandlingÅrsak) {
        return switch (behandlingÅrsak.getBehandlingÅrsakType()) {
            case RE_FEIL_I_LOVANDVENDELSE -> BehandlingÅrsakType.RE_FEIL_I_LOVANDVENDELSE;
            case RE_FEIL_REGELVERKSFORSTÅELSE -> BehandlingÅrsakType.RE_FEIL_REGELVERKSFORSTÅELSE;
            case RE_FEIL_ELLER_ENDRET_FAKTA -> BehandlingÅrsakType.RE_FEIL_ELLER_ENDRET_FAKTA;
            case RE_FEIL_PROSESSUELL -> BehandlingÅrsakType.RE_FEIL_PROSESSUELL;
            case RE_ANNET -> BehandlingÅrsakType.RE_ANNET;
            case RE_OPPLYSNINGER_OM_MEDLEMSKAP -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_MEDLEMSKAP;
            case RE_OPPLYSNINGER_OM_OPPTJENING -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_OPPTJENING;
            case RE_OPPLYSNINGER_OM_FORDELING -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FORDELING;
            case RE_OPPLYSNINGER_OM_INNTEKT -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_INNTEKT;
            case RE_OPPLYSNINGER_OM_FØDSEL -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_FØDSEL;
            case RE_OPPLYSNINGER_OM_DØD -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_DØD;
            case RE_OPPLYSNINGER_OM_SØKERS_REL -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKERS_REL;
            case RE_OPPLYSNINGER_OM_SØKNAD_FRIST -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_SØKNAD_FRIST;
            case RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG;
            case RE_KLAGE_UTEN_END_INNTEKT -> BehandlingÅrsakType.RE_KLAGE_UTEN_END_INNTEKT;
            case RE_KLAGE_MED_END_INNTEKT -> BehandlingÅrsakType.RE_KLAGE_MED_END_INNTEKT;
            case ETTER_KLAGE -> BehandlingÅrsakType.ETTER_KLAGE;
            case RE_MANGLER_FØDSEL -> BehandlingÅrsakType.RE_MANGLER_FØDSEL;
            case RE_MANGLER_FØDSEL_I_PERIODE -> BehandlingÅrsakType.RE_MANGLER_FØDSEL_I_PERIODE;
            case RE_AVVIK_ANTALL_BARN -> BehandlingÅrsakType.RE_AVVIK_ANTALL_BARN;
            case RE_ENDRING_FRA_BRUKER -> BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER;
            case RE_ENDRET_INNTEKTSMELDING -> BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING;
            case BERØRT_BEHANDLING -> BehandlingÅrsakType.BERØRT_BEHANDLING;
            case REBEREGN_FERIEPENGER -> BehandlingÅrsakType.REBEREGN_FERIEPENGER;
            case RE_UTSATT_START -> BehandlingÅrsakType.RE_UTSATT_START;
            case RE_SATS_REGULERING -> BehandlingÅrsakType.RE_SATS_REGULERING;
            case ENDRE_DEKNINGSGRAD -> BehandlingÅrsakType.ENDRE_DEKNINGSGRAD;
            case INFOBREV_BEHANDLING -> BehandlingÅrsakType.INFOBREV_BEHANDLING;
            case INFOBREV_OPPHOLD -> BehandlingÅrsakType.INFOBREV_OPPHOLD;
            case INFOBREV_PÅMINNELSE -> BehandlingÅrsakType.INFOBREV_PÅMINNELSE;
            case OPPHØR_YTELSE_NYTT_BARN -> BehandlingÅrsakType.OPPHØR_YTELSE_NYTT_BARN;
            case RE_HENDELSE_FØDSEL -> BehandlingÅrsakType.RE_HENDELSE_FØDSEL;
            case RE_HENDELSE_DØD_FORELDER -> BehandlingÅrsakType.RE_HENDELSE_DØD_FORELDER;
            case RE_HENDELSE_DØD_BARN -> BehandlingÅrsakType.RE_HENDELSE_DØD_BARN;
            case RE_HENDELSE_DØDFØDSEL -> BehandlingÅrsakType.RE_HENDELSE_DØDFØDSEL;
            case RE_HENDELSE_UTFLYTTING -> BehandlingÅrsakType.RE_HENDELSE_UTFLYTTING;
            case RE_VEDTAK_PLEIEPENGER -> BehandlingÅrsakType.RE_VEDTAK_PLEIEPENGER;
            case FEIL_PRAKSIS_UTSETTELSE -> BehandlingÅrsakType.FEIL_PRAKSIS_UTSETTELSE;
            case FEIL_PRAKSIS_IVERKS_UTSET -> BehandlingÅrsakType.FEIL_PRAKSIS_IVERKS_UTSET;
            case FEIL_PRAKSIS_BG_AAP_KOMBI -> BehandlingÅrsakType.FEIL_PRAKSIS_BG_AAP_KOMBI;
            case KLAGE_TILBAKEBETALING -> BehandlingÅrsakType.KLAGE_TILBAKEBETALING;
            case RE_OPPLYSNINGER_OM_YTELSER -> BehandlingÅrsakType.RE_OPPLYSNINGER_OM_YTELSER;
            case RE_REGISTEROPPLYSNING -> BehandlingÅrsakType.RE_REGISTEROPPLYSNING;
            case KØET_BEHANDLING -> BehandlingÅrsakType.KØET_BEHANDLING;
            case RE_TILSTØTENDE_YTELSE_INNVILGET -> BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_INNVILGET;
            case RE_TILSTØTENDE_YTELSE_OPPHØRT -> BehandlingÅrsakType.RE_TILSTØTENDE_YTELSE_OPPHØRT;
            case UDEFINERT -> BehandlingÅrsakType.UDEFINERT;
        };
    }

    static Behandlingsresultat.VilkårType mapVilkårType(VilkårType vilkårType) {
        return switch (vilkårType) {
            case FØDSELSVILKÅRET_MOR -> Behandlingsresultat.VilkårType.FØDSELSVILKÅRET_MOR;
            case FØDSELSVILKÅRET_FAR_MEDMOR -> Behandlingsresultat.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
            case ADOPSJONSVILKARET_FORELDREPENGER -> Behandlingsresultat.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
            case MEDLEMSKAPSVILKÅRET -> Behandlingsresultat.VilkårType.MEDLEMSKAPSVILKÅRET;
            case MEDLEMSKAPSVILKÅRET_FORUTGÅENDE -> Behandlingsresultat.VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE;
            case MEDLEMSKAPSVILKÅRET_LØPENDE -> Behandlingsresultat.VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE;
            case SØKNADSFRISTVILKÅRET -> Behandlingsresultat.VilkårType.SØKNADSFRISTVILKÅRET;
            case ADOPSJONSVILKÅRET_ENGANGSSTØNAD -> Behandlingsresultat.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
            case OMSORGSVILKÅRET -> Behandlingsresultat.VilkårType.OMSORGSVILKÅRET;
            case FORELDREANSVARSVILKÅRET_2_LEDD -> Behandlingsresultat.VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
            case FORELDREANSVARSVILKÅRET_4_LEDD -> Behandlingsresultat.VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
            case SØKERSOPPLYSNINGSPLIKT -> Behandlingsresultat.VilkårType.SØKERSOPPLYSNINGSPLIKT;
            case OPPTJENINGSPERIODEVILKÅR -> Behandlingsresultat.VilkårType.OPPTJENINGSPERIODEVILKÅR;
            case OPPTJENINGSVILKÅRET -> Behandlingsresultat.VilkårType.OPPTJENINGSVILKÅRET;
            case BEREGNINGSGRUNNLAGVILKÅR -> Behandlingsresultat.VilkårType.BEREGNINGSGRUNNLAGVILKÅR;
            case SVANGERSKAPSPENGERVILKÅR -> Behandlingsresultat.VilkårType.SVANGERSKAPSPENGERVILKÅR;
            case UDEFINERT -> Behandlingsresultat.VilkårType.UDEFINERT;
        };
    }

    static Behandlingsresultat.BehandlingResultatType mapBehandlingResultatType(BehandlingResultatType behandlingResultatType) {
        return switch (behandlingResultatType) {
            case IKKE_FASTSATT -> Behandlingsresultat.BehandlingResultatType.IKKE_FASTSATT;
            case INNVILGET -> Behandlingsresultat.BehandlingResultatType.INNVILGET;
            case AVSLÅTT -> Behandlingsresultat.BehandlingResultatType.AVSLÅTT;
            case OPPHØR -> Behandlingsresultat.BehandlingResultatType.OPPHØR;
            case HENLAGT_SØKNAD_TRUKKET -> Behandlingsresultat.BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET;
            case HENLAGT_FEILOPPRETTET -> Behandlingsresultat.BehandlingResultatType.HENLAGT_FEILOPPRETTET;
            case HENLAGT_BRUKER_DØD -> Behandlingsresultat.BehandlingResultatType.HENLAGT_BRUKER_DØD;
            case MERGET_OG_HENLAGT -> Behandlingsresultat.BehandlingResultatType.MERGET_OG_HENLAGT;
            case HENLAGT_SØKNAD_MANGLER -> Behandlingsresultat.BehandlingResultatType.HENLAGT_SØKNAD_MANGLER;
            case FORELDREPENGER_ENDRET -> Behandlingsresultat.BehandlingResultatType.FORELDREPENGER_ENDRET;
            case FORELDREPENGER_SENERE -> Behandlingsresultat.BehandlingResultatType.FORELDREPENGER_SENERE;
            case INGEN_ENDRING -> Behandlingsresultat.BehandlingResultatType.INGEN_ENDRING;
            case MANGLER_BEREGNINGSREGLER -> Behandlingsresultat.BehandlingResultatType.MANGLER_BEREGNINGSREGLER;
            case KLAGE_AVVIST -> Behandlingsresultat.BehandlingResultatType.KLAGE_AVVIST;
            case KLAGE_MEDHOLD -> Behandlingsresultat.BehandlingResultatType.KLAGE_MEDHOLD;
            case KLAGE_DELVIS_MEDHOLD -> Behandlingsresultat.BehandlingResultatType.KLAGE_DELVIS_MEDHOLD;
            case KLAGE_OMGJORT_UGUNST -> Behandlingsresultat.BehandlingResultatType.KLAGE_OMGJORT_UGUNST;
            case KLAGE_YTELSESVEDTAK_OPPHEVET -> Behandlingsresultat.BehandlingResultatType.KLAGE_YTELSESVEDTAK_OPPHEVET;
            case KLAGE_YTELSESVEDTAK_STADFESTET -> Behandlingsresultat.BehandlingResultatType.KLAGE_YTELSESVEDTAK_STADFESTET;
            case KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET -> Behandlingsresultat.BehandlingResultatType.KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET;
            case HENLAGT_KLAGE_TRUKKET -> Behandlingsresultat.BehandlingResultatType.HENLAGT_KLAGE_TRUKKET;
            case HJEMSENDE_UTEN_OPPHEVE -> Behandlingsresultat.BehandlingResultatType.HJEMSENDE_UTEN_OPPHEVE;
            case ANKE_AVVIST -> Behandlingsresultat.BehandlingResultatType.ANKE_AVVIST;
            case ANKE_MEDHOLD -> Behandlingsresultat.BehandlingResultatType.ANKE_MEDHOLD;
            case ANKE_DELVIS_MEDHOLD -> Behandlingsresultat.BehandlingResultatType.ANKE_DELVIS_MEDHOLD;
            case ANKE_OMGJORT_UGUNST -> Behandlingsresultat.BehandlingResultatType.ANKE_OMGJORT_UGUNST;
            case ANKE_OPPHEVE_OG_HJEMSENDE -> Behandlingsresultat.BehandlingResultatType.ANKE_OPPHEVE_OG_HJEMSENDE;
            case ANKE_HJEMSENDE_UTEN_OPPHEV -> Behandlingsresultat.BehandlingResultatType.ANKE_HJEMSENDE_UTEN_OPPHEV;
            case ANKE_YTELSESVEDTAK_STADFESTET -> Behandlingsresultat.BehandlingResultatType.ANKE_YTELSESVEDTAK_STADFESTET;
            case HENLAGT_ANKE_TRUKKET -> Behandlingsresultat.BehandlingResultatType.HENLAGT_ANKE_TRUKKET;
            case INNSYN_INNVILGET -> Behandlingsresultat.BehandlingResultatType.INNSYN_INNVILGET;
            case INNSYN_DELVIS_INNVILGET -> Behandlingsresultat.BehandlingResultatType.INNSYN_DELVIS_INNVILGET;
            case INNSYN_AVVIST -> Behandlingsresultat.BehandlingResultatType.INNSYN_AVVIST;
            case HENLAGT_INNSYN_TRUKKET -> Behandlingsresultat.BehandlingResultatType.HENLAGT_INNSYN_TRUKKET;
        };
    }

    static Rettigheter.Rettighetstype mapRettighetstype(Rettighetstype rettighetstype) {
        return switch (rettighetstype) {
            case ALENEOMSORG -> Rettigheter.Rettighetstype.ALENEOMSORG;
            case BEGGE_RETT -> Rettigheter.Rettighetstype.BEGGE_RETT;
            case BEGGE_RETT_EØS -> Rettigheter.Rettighetstype.BEGGE_RETT_EØS;
            case BARE_MOR_RETT -> Rettigheter.Rettighetstype.BARE_MOR_RETT;
            case BARE_FAR_RETT -> Rettigheter.Rettighetstype.BARE_FAR_RETT;
            case BARE_FAR_RETT_MOR_UFØR -> Rettigheter.Rettighetstype.BARE_FAR_RETT_MOR_UFØR;
        };
    }

    static BrevGrunnlagDto.Språkkode mapSpråkkode(Språkkode språkkode) {
        return switch (språkkode) {
            case NB -> BrevGrunnlagDto.Språkkode.BOKMÅL;
            case NN -> BrevGrunnlagDto.Språkkode.NYNORSK;
            case EN -> BrevGrunnlagDto.Språkkode.ENGELSK;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + språkkode);
        };
    }

    static BrevGrunnlagDto.BehandlingType mapBehandlingType(BehandlingType type) {
        return switch (type) {
            case null -> null;
            case FØRSTEGANGSSØKNAD -> BrevGrunnlagDto.BehandlingType.FØRSTEGANGSSØKNAD;
            case KLAGE -> BrevGrunnlagDto.BehandlingType.KLAGE;
            case REVURDERING -> BrevGrunnlagDto.BehandlingType.REVURDERING;
            case ANKE -> BrevGrunnlagDto.BehandlingType.ANKE;
            case INNSYN -> BrevGrunnlagDto.BehandlingType.INNSYN;
            case TILBAKEKREVING_ORDINÆR -> BrevGrunnlagDto.BehandlingType.TILBAKEKREVING;
            case TILBAKEKREVING_REVURDERING -> BrevGrunnlagDto.BehandlingType.TILBAKEKREVING_REVURDERING;
            case UDEFINERT -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }
}
