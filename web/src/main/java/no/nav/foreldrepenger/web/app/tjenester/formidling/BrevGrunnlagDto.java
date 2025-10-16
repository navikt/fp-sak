package no.nav.foreldrepenger.web.app.tjenester.formidling;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import no.nav.foreldrepenger.kontrakter.fpsak.beregningsgrunnlag.v2.BeregningsgrunnlagDto;
import no.nav.foreldrepenger.kontrakter.fpsak.inntektsmeldinger.ArbeidsforholdInntektsmeldingerDto;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseDagytelseDto;
import no.nav.foreldrepenger.kontrakter.fpsak.tilkjentytelse.TilkjentYtelseEngangsstønadDto;

record BrevGrunnlagDto(UUID uuid, String saksnummer, FagsakYtelseType fagsakYtelseType, RelasjonsRolleType relasjonsRolleType, String aktørId,
                       BehandlingType behandlingType, LocalDateTime opprettet, LocalDateTime avsluttet, String behandlendeEnhet, Språkkode språkkode,
                       boolean automatiskBehandlet, FamilieHendelse familieHendelse, OriginalBehandling originalBehandling,
                       Behandlingsresultat behandlingsresultat, List<BehandlingÅrsakType> behandlingÅrsakTyper, TilkjentYtelse tilkjentYtelse,
                       BeregningsgrunnlagDto beregningsgrunnlag, ArbeidsforholdInntektsmeldingerDto inntektsmeldingerStatus,
                       LocalDate førsteSøknadMottattDato,
                       //Ser på mottatt dato på dokument
                       LocalDate sisteSøknadMottattDato, //Ser på mottatt dato på dokument
                       LocalDate søknadMottattDato, //Uttaksperiodegrense (når den anses å være mottatt)
                       List<Inntektsmelding> inntektsmeldinger, Verge verge, KlageBehandling klageBehandling, InnsynBehandling innsynBehandling,
                       Svangerskapspenger svangerskapspenger, Foreldrepenger foreldrepenger) {
    enum FagsakYtelseType {
        ENGANGSTØNAD,
        FORELDREPENGER,
        SVANGERSKAPSPENGER,
    }

    enum RelasjonsRolleType {
        FARA,
        MORA,
        MEDMOR,
    }

    enum BehandlingType {
        FØRSTEGANGSSØKNAD,
        REVURDERING,
        KLAGE,
        ANKE,
        INNSYN,
        TILBAKEKREVING,
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

    enum Dekningsgrad {
        HUNDRE,
        ÅTTI
    }

    record OriginalBehandling(FamilieHendelse familieHendelse, Behandlingsresultat.BehandlingResultatType behandlingResultatType,
                              LocalDate førsteDagMedUtbetaltForeldrepenger) {
    }

    record TilkjentYtelse(TilkjentYtelseEngangsstønadDto engangsstønad, TilkjentYtelseEngangsstønadDto originalBehandlingEngangsstønad,
                          TilkjentYtelseDagytelseDto dagytelse) {
    }

    record Foreldrepenger(Dekningsgrad dekningsgrad, Rettigheter rettigheter, List<Stønadskonto> stønadskontoer, int tapteDagerFpff,
                          List<Uttaksperiode> perioderSøker, List<Uttaksperiode> perioderAnnenpart, boolean ønskerJustertUttakVedFødsel,
                          LocalDate nyStartDatoVedUtsattOppstart) {
        record Uttaksperiode(LocalDate fom, LocalDate tom, List<Aktivitet> aktiviteter, PeriodeResultatType periodeResultatType,
                             String periodeResultatÅrsak, String graderingAvslagÅrsak, String periodeResultatÅrsakLovhjemmel,
                             String graderingsAvslagÅrsakLovhjemmel, LocalDate tidligstMottattDato,
                             boolean erUtbetalingRedusertTilMorsStillingsprosent) {
        }

        record Aktivitet(TrekkontoType trekkontoType, BigDecimal trekkdager, BigDecimal prosentArbeid, String arbeidsgiverReferanse,
                         String arbeidsforholdId, BigDecimal utbetalingsgrad, UttakArbeidType uttakArbeidType, boolean gradering) {
        }

        record Stønadskonto(Type stønadskontotype, int maxDager, int saldo, KontoUtvidelser kontoUtvidelser) {

            public enum Type {
                MØDREKVOTE,
                FEDREKVOTE,
                FELLESPERIODE,
                FORELDREPENGER,
                FORELDREPENGER_FØR_FØDSEL,
                FLERBARNSDAGER,
                UTEN_AKTIVITETSKRAV,
                MINSTERETT_NESTE_STØNADSPERIODE,
                MINSTERETT
            }
        }

        record KontoUtvidelser(int prematurdager, int flerbarnsdager) {

        }

        enum TrekkontoType {
            FELLESPERIODE,
            MØDREKVOTE,
            FEDREKVOTE,
            FORELDREPENGER,
            FORELDREPENGER_FØR_FØDSEL,
            UDEFINERT,
        }
    }

    enum UttakArbeidType {
        ORDINÆRT_ARBEID,
        SELVSTENDIG_NÆRINGSDRIVENDE,
        FRILANS,
        ANNET,
    }

    enum PeriodeResultatType {
        INNVILGET,
        AVSLÅTT,
        MANUELL_BEHANDLING
    }

    record Svangerskapspenger(List<UttakArbeidsforhold> uttakArbeidsforhold) {

        record UttakArbeidsforhold(String arbeidsforholdIkkeOppfyltÅrsak, String arbeidsgiverReferanse, UttakArbeidType arbeidType,
                                   List<Uttaksperiode> perioder) {

        }

        record Uttaksperiode(LocalDate fom, LocalDate tom, PeriodeResultatType periodeResultatType, String periodeIkkeOppfyltÅrsak) {
        }
    }

    record Inntektsmelding(String arbeidsgiverReferanse, LocalDateTime innsendingstidspunkt) {
    }

    record Rettigheter(Rettighetstype opprinnelig,  //søknad eller forrige vedtak
                       Rettighetstype gjeldende, EøsUttak eøsUttak) {
        record EøsUttak(LocalDate fom, LocalDate tom, int forbruktFellesperiode, int fellesperiodeINorge) {
        }

        enum Rettighetstype {
            ALENEOMSORG,
            BEGGE_RETT,
            BEGGE_RETT_EØS,
            BARE_MOR_RETT,
            BARE_FAR_RETT,
            BARE_FAR_RETT_MOR_UFØR,
        }
    }

    record FamilieHendelse(List<Barn> barn, LocalDate termindato, int antallBarn, LocalDate omsorgsovertakelse) {
    }

    record Barn(LocalDate fødselsdato, LocalDate dødsdato) {
    }

    record Verge(String aktørId, String navn, String organisasjonsnummer, LocalDate gyldigFom, LocalDate gyldigTom) {

    }

    record InnsynBehandling(InnsynResultatType innsynResultatType, List<InnsynDokument> dokumenter) {

        enum InnsynResultatType {
            INNVILGET,
            DELVIS_INNVILGET,
            AVVIST,
            UDEFINERT,
        }

        record InnsynDokument(String journalpostId, String dokumentId) {
        }
    }

    record KlageBehandling(KlageFormkravResultat klageFormkravResultatNFP, KlageVurderingResultat klageVurderingResultatNFP,
                           KlageFormkravResultat klageFormkravResultatKA, KlageVurderingResultat klageVurderingResultatNK, LocalDate mottattDato) {

        record KlageFormkravResultat(BehandlingType påklagdBehandlingType, List<KlageAvvistÅrsak> avvistÅrsaker) {
        }

        record KlageVurderingResultat(String fritekstTilBrev) {
        }

        enum KlageAvvistÅrsak {
            KLAGET_FOR_SENT,
            KLAGE_UGYLDIG,
            IKKE_PÅKLAGD_VEDTAK,
            KLAGER_IKKE_PART,
            IKKE_KONKRET,
            IKKE_SIGNERT,
            UDEFINERT,
        }
    }

    record Behandlingsresultat(String medlemskapOpphørsårsak, LocalDate medlemskapFom, BehandlingResultatType behandlingResultatType,
                               String avslagsårsak, Fritekst fritekst, Skjæringstidspunkt skjæringstidspunkt, boolean endretDekningsgrad,
                               LocalDate opphørsdato, List<KonsekvensForYtelsen> konsekvenserForYtelsen, List<VilkårType> vilkårTyper) {

        record Fritekst(String overskrift, String brødtekst, String avslagsarsakFritekst) {
        }

        record Skjæringstidspunkt(LocalDate dato, boolean utenMinsterett) {
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
    }
}
