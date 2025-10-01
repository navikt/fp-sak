package no.nav.foreldrepenger.web.app.tjenester.brev;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.Rettighetstype;

record BrevGrunnlagDto(UUID uuid, BehandlingType type, LocalDateTime opprettet, LocalDateTime avsluttet, String behandlendeEnhet, Språkkode språkkode,
                       boolean automatiskBehandlet, FamilieHendelseDto familieHendelse, FamilieHendelseDto originalBehandlingFamilieHendelse,
                       RettigheterDto rettigheter, BehandlingsresultatDto behandlingsresultat, List<BehandlingÅrsakType> behandlingÅrsakTyper) {
    enum BehandlingType {
        FØRSTEGANGSSØKNAD,
        REVURDERING,
        KLAGE,
        ANKE,
        INNSYN,
        TILBAKEKREVING_ORDINÆR,
        TILBAKEKREVING_REVURDERING,
    }

    enum Språkkode {
        BOKMÅL,
        NYNORSK,
        ENGELSK
    }

    enum BehandlingÅrsakType {
        RE_FEIL_I_LOVANDVENDELSE,
        RE_FEIL_REGELVERKSFORSTÅELSE,
        RE_FEIL_ELLER_ENDRET_FAKTA,
        RE_FEIL_PROSESSUELL,
        RE_ANNET,
        RE_OPPLYSNINGER_OM_MEDLEMSKAP,
        RE_OPPLYSNINGER_OM_OPPTJENING,
        RE_OPPLYSNINGER_OM_FORDELING,
        RE_OPPLYSNINGER_OM_INNTEKT,
        RE_OPPLYSNINGER_OM_FØDSEL,
        RE_OPPLYSNINGER_OM_DØD,
        RE_OPPLYSNINGER_OM_SØKERS_REL,
        RE_OPPLYSNINGER_OM_SØKNAD_FRIST,
        RE_OPPLYSNINGER_OM_BEREGNINGSGRUNNLAG,
        RE_KLAGE_UTEN_END_INNTEKT,
        RE_KLAGE_MED_END_INNTEKT,
        ETTER_KLAGE,
        RE_MANGLER_FØDSEL,
        RE_MANGLER_FØDSEL_I_PERIODE,
        RE_AVVIK_ANTALL_BARN,
        RE_ENDRING_FRA_BRUKER,
        RE_ENDRET_INNTEKTSMELDING,
        BERØRT_BEHANDLING,
        REBEREGN_FERIEPENGER,
        RE_UTSATT_START,
        RE_SATS_REGULERING,
        ENDRE_DEKNINGSGRAD,
        INFOBREV_BEHANDLING,
        INFOBREV_OPPHOLD,
        INFOBREV_PÅMINNELSE,
        OPPHØR_YTELSE_NYTT_BARN,
        RE_HENDELSE_FØDSEL,
        RE_HENDELSE_DØD_FORELDER,
        RE_HENDELSE_DØD_BARN,
        RE_HENDELSE_DØDFØDSEL,
        RE_HENDELSE_UTFLYTTING,
        RE_VEDTAK_PLEIEPENGER,
        FEIL_PRAKSIS_UTSETTELSE,
        FEIL_PRAKSIS_IVERKS_UTSET,
        FEIL_PRAKSIS_BG_AAP_KOMBI,
        KLAGE_TILBAKEBETALING,
        RE_OPPLYSNINGER_OM_YTELSER,
        RE_REGISTEROPPLYSNING,
        KØET_BEHANDLING,
        RE_TILSTØTENDE_YTELSE_INNVILGET,
        RE_TILSTØTENDE_YTELSE_OPPHØRT,
        UDEFINERT,
    }

    record RettigheterDto(Rettighetstype opprinnelig,  //søknad eller forrige vedtak
                          Rettighetstype gjeldende, EøsUttakDto eøsUttak) {
        record EøsUttakDto(LocalDate fom, LocalDate tom, int forbruktFellesperiode, int fellesperiodeINorge) {
        }
    }

    record FamilieHendelseDto(List<BarnDto> barn, LocalDate termindato, int antallBarn, LocalDate omsorgsovertakelse) {
    }

    record BarnDto(LocalDate fødselsdato, LocalDate dødsdato) {

    }

    enum MedlemskapOpphørsÅrsak {
        SØKER_ER_IKKE_MEDLEM,
        SØKER_ER_UTVANDRET,
        SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
        SØKER_HAR_IKKE_OPPHOLDSRETT,
        SØKER_ER_IKKE_BOSATT,
        SØKER_INNFLYTTET_FOR_SENT,
    }

    record BehandlingsresultatDto(MedlemskapOpphørsÅrsak medlemskapOpphørsårsak, LocalDate medlemskapFom,
                                  BehandlingResultatType behandlingResultatType, Avslagsårsak avslagsårsak, Fritekst avslagsarsakFritekst,
                                  SkjæringstidspunktDto skjæringstidspunkt, boolean endretDekningsgrad, LocalDate opphørsdato,
                                  List<KonsekvensForYtelsen> konsekvenserForYtelsen, List<VilkårType> vilkårTyper) {

        record Fritekst(String overskrift, String fritekst, String avslagsarsakFritekst) {
        }

        record SkjæringstidspunktDto(LocalDate dato, boolean utenMinsterett) {
        }

        enum VilkårType {
            FØDSELSVILKÅRET_MOR,
            FØDSELSVILKÅRET_FAR_MEDMOR,
            ADOPSJONSVILKARET_FORELDREPENGER,
            MEDLEMSKAPSVILKÅRET,
            MEDLEMSKAPSVILKÅRET_FORUTGÅENDE,
            MEDLEMSKAPSVILKÅRET_LØPENDE,
            SØKNADSFRISTVILKÅRET,
            ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
            OMSORGSVILKÅRET,
            FORELDREANSVARSVILKÅRET_2_LEDD,
            FORELDREANSVARSVILKÅRET_4_LEDD,
            SØKERSOPPLYSNINGSPLIKT,
            OPPTJENINGSPERIODEVILKÅR,
            OPPTJENINGSVILKÅRET,
            BEREGNINGSGRUNNLAGVILKÅR,
            SVANGERSKAPSPENGERVILKÅR,
            UDEFINERT,
        }

        enum KonsekvensForYtelsen {
            FORELDREPENGER_OPPHØRER,
            ENDRING_I_BEREGNING,
            ENDRING_I_UTTAK,
            ENDRING_I_FORDELING_AV_YTELSEN,
            INGEN_ENDRING,
            UDEFINERT,
        }

        enum BehandlingResultatType {
            IKKE_FASTSATT,
            INNVILGET,
            AVSLÅTT,
            OPPHØR,
            HENLAGT_SØKNAD_TRUKKET,
            HENLAGT_FEILOPPRETTET,
            HENLAGT_BRUKER_DØD,
            MERGET_OG_HENLAGT,
            HENLAGT_SØKNAD_MANGLER,
            FORELDREPENGER_ENDRET,
            FORELDREPENGER_SENERE,
            INGEN_ENDRING,
            MANGLER_BEREGNINGSREGLER,
            KLAGE_AVVIST,
            KLAGE_MEDHOLD,
            KLAGE_DELVIS_MEDHOLD,
            KLAGE_OMGJORT_UGUNST,
            KLAGE_YTELSESVEDTAK_OPPHEVET,
            KLAGE_YTELSESVEDTAK_STADFESTET,
            KLAGE_TILBAKEKREVING_VEDTAK_STADFESTET,
            HENLAGT_KLAGE_TRUKKET,
            HJEMSENDE_UTEN_OPPHEVE,
            ANKE_AVVIST,
            ANKE_MEDHOLD,
            ANKE_DELVIS_MEDHOLD,
            ANKE_OMGJORT_UGUNST,
            ANKE_OPPHEVE_OG_HJEMSENDE,
            ANKE_HJEMSENDE_UTEN_OPPHEV,
            ANKE_YTELSESVEDTAK_STADFESTET,
            HENLAGT_ANKE_TRUKKET,
            INNSYN_INNVILGET,
            INNSYN_DELVIS_INNVILGET,
            INNSYN_AVVIST,
            HENLAGT_INNSYN_TRUKKET,
        }

        enum Avslagsårsak {
            SØKT_FOR_TIDLIG,
            SØKER_ER_MEDMOR,
            SØKER_ER_FAR,
            BARN_OVER_15_ÅR,
            EKTEFELLES_SAMBOERS_BARN,
            MANN_ADOPTERER_IKKE_ALENE,
            SØKT_FOR_SENT,
            SØKER_ER_IKKE_BARNETS_FAR_O,
            MOR_IKKE_DØD,
            MOR_IKKE_DØD_VED_FØDSEL_OMSORG,
            ENGANGSSTØNAD_ALLEREDE_UTBETALT_TIL_MOR,
            FAR_HAR_IKKE_OMSORG_FOR_BARNET,
            BARN_IKKE_UNDER_15_ÅR,
            SØKER_HAR_IKKE_FORELDREANSVAR,
            SØKER_HAR_HATT_VANLIG_SAMVÆR_MED_BARNET,
            SØKER_ER_IKKE_BARNETS_FAR_F,
            OMSORGSOVERTAKELSE_ETTER_56_UKER,
            IKKE_FORELDREANSVAR_ALENE_ETTER_BARNELOVA,
            MANGLENDE_DOKUMENTASJON,
            SØKER_ER_IKKE_MEDLEM,
            SØKER_ER_UTVANDRET,
            SØKER_HAR_IKKE_LOVLIG_OPPHOLD,
            SØKER_HAR_IKKE_OPPHOLDSRETT,
            SØKER_ER_IKKE_BOSATT,
            FØDSELSDATO_IKKE_OPPGITT_ELLER_REGISTRERT,
            INGEN_BARN_DOKUMENTERT_PÅ_FAR_MEDMOR,
            MOR_FYLLER_IKKE_VILKÅRET_FOR_SYKDOM,
            BRUKER_ER_IKKE_REGISTRERT_SOM_FAR_MEDMOR_TIL_BARNET,
            ENGANGSTØNAD_ER_ALLEREDE_UTBETAL_TIL_MOR,
            FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_MOR,
            ENGANGSSTØNAD_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
            FORELDREPENGER_ER_ALLEREDE_UTBETALT_TIL_FAR_MEDMOR,
            IKKE_TILSTREKKELIG_OPPTJENING,
            FOR_LAVT_BEREGNINGSGRUNNLAG,
            STEBARNSADOPSJON_IKKE_FLERE_DAGER_IGJEN,
            SØKER_INNFLYTTET_FOR_SENT,
            SØKER_IKKE_GRAVID_KVINNE,
            SØKER_ER_IKKE_I_ARBEID,
            SØKER_HAR_MOTTATT_SYKEPENGER,
            ARBEIDSTAKER_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER,
            ARBEIDSTAKER_KAN_OMPLASSERES,
            SN_FL_HAR_IKKE_DOKUMENTERT_RISIKOFAKTORER,
            SN_FL_HAR_MULIGHET_TIL_Å_TILRETTELEGGE_SITT_VIRKE,
            INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN,
            UDEFINERT,
        }
    }
}
