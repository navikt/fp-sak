package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.ARBEID;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.FERIE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.FRI;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.HV_OVELSE;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.INSTITUSJON_BARN;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.INSTITUSJON_SØKER;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.NAV_TILTAK;
import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.SYKDOM;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.KontrollerAktivitetskravAvklaring;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.MorsAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.grunnlag.Dekningsgrad;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.ArbeidsgiverIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Orgnummer;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.PeriodeMedAvklartMorsAktivitet;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.PeriodeVurderingType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.Perioderesultattype;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.GraderingIkkeInnvilgetÅrsak;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.Manuellbehandlingårsak;
import no.nav.foreldrepenger.regler.uttak.felles.grunnlag.Stønadskontotype;

public final class UttakEnumMapper {

    private static final KodeMapper<StønadskontoType, Stønadskontotype> STØNADSKONTOTYPE_KODE_MAPPER = initStønadskontotypeMapper();
    private static final KodeMapper<UttakPeriodeType, Stønadskontotype> UTTAK_PERIODE_TYPE_MAPPER = initUttakPeriodeTypeMapper();
    private static final KodeMapper<UtsettelseÅrsak, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak> UTSETTELSE_ÅRSAK_MAPPER = initUtsettelseÅrsakMapper();
    private static final KodeMapper<UttakPeriodeVurderingType, PeriodeVurderingType> VURDERING_TYPE_MAPPER = initVurderingTypeMapper();
    private static final KodeMapper<OverføringÅrsak, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak> OVERFØRING_ÅRSAK_MAPPER = initOverføringÅrsakMapper();
    private static final KodeMapper<MorsAktivitet, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet> MORS_AKTIVITET_MAPPER = initMorsAktivitetMapper();
    private static final KodeMapper<UttakUtsettelseType, UtsettelseÅrsak> UTTAK_TIL_OPPGITT_UTSETTELSE_MAPPER = initUttakTilOppgittUtsettelseMapper();
    private static final KodeMapper<StønadskontoType, UttakPeriodeType> PERIODE_TYPE_MAPPER = initPeriodeTypeMapper();


    private UttakEnumMapper() {

    }

    public static Perioderesultattype map(PeriodeResultatType periodeResultatType) {
        return switch (periodeResultatType) {
            case INNVILGET -> Perioderesultattype.INNVILGET;
            case AVSLÅTT -> Perioderesultattype.AVSLÅTT;
            case MANUELL_BEHANDLING -> Perioderesultattype.MANUELL_BEHANDLING;
        };
    }

    public static Trekkdager map(no.nav.foreldrepenger.behandlingslager.uttak.fp.Trekkdager trekkdager) {
        return new Trekkdager(trekkdager.decimalValue());
    }

    public static AktivitetIdentifikator map(UttakAktivitetEntitet aktivitet) {
        var ref = aktivitet.getArbeidsforholdRef();
        var arbeidsgiver = aktivitet.getArbeidsgiver();
        var uttakArbeidType = aktivitet.getUttakArbeidType();
        return map(uttakArbeidType, arbeidsgiver, ref);
    }

    public static AktivitetIdentifikator map(UttakArbeidType uttakArbeidType,
                                             Optional<Arbeidsgiver> arbeidsgiver,
                                             InternArbeidsforholdRef ref) {
        return switch (uttakArbeidType) {
            case FRILANS -> AktivitetIdentifikator.forFrilans();
            case SELVSTENDIG_NÆRINGSDRIVENDE ->  AktivitetIdentifikator.forSelvstendigNæringsdrivende();
            case ANNET -> AktivitetIdentifikator.annenAktivitet();
            case ORDINÆRT_ARBEID -> AktivitetIdentifikator.forArbeid(mapArbeidTypeArbeid(arbeidsgiver), ref.getReferanse());
        };
    }

    private static ArbeidsgiverIdentifikator mapArbeidTypeArbeid(Optional<Arbeidsgiver> arbeidsgiver) {
        return arbeidsgiver.map(a -> {
            var identifikator = a.getIdentifikator();
            if (a.getErVirksomhet()) {
                return new Orgnummer(identifikator);
            }
            return new no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktørId(identifikator);
        }).orElse(null);
    }

    public static Dekningsgrad map(int dekningsgrad) {
        if (Objects.equals(dekningsgrad, OppgittDekningsgradEntitet.HUNDRE_PROSENT)) {
            return Dekningsgrad.DEKNINGSGRAD_100;
        }
        return Dekningsgrad.DEKNINGSGRAD_80;
    }

    public static Stønadskontotype map(StønadskontoType stønadskontoType) {
        if (stønadskontoType == StønadskontoType.UDEFINERT) {
            return null;
        }
        return STØNADSKONTOTYPE_KODE_MAPPER
            .map(stønadskontoType)
            .orElseThrow(() -> new UnsupportedOperationException(String.format("Har ikke støtte for søknadstype %s", stønadskontoType.getKode())));
    }

    private static KodeMapper<StønadskontoType, Stønadskontotype> initStønadskontotypeMapper() {
        return KodeMapper
            .medMapping(StønadskontoType.FORELDREPENGER, Stønadskontotype.FORELDREPENGER)
            .medMapping(StønadskontoType.FELLESPERIODE, Stønadskontotype.FELLESPERIODE)
            .medMapping(StønadskontoType.MØDREKVOTE, Stønadskontotype.MØDREKVOTE)
            .medMapping(StønadskontoType.FEDREKVOTE, Stønadskontotype.FEDREKVOTE)
            .medMapping(StønadskontoType.FLERBARNSDAGER, Stønadskontotype.FLERBARNSDAGER)
            .medMapping(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, Stønadskontotype.FORELDREPENGER_FØR_FØDSEL)
            .build();
    }

    public static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak map(OppholdÅrsak årsakType) {
        return switch (årsakType) {
            case FEDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
            case MØDREKVOTE_ANNEN_FORELDER -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
            case KVOTE_FELLESPERIODE_ANNEN_FORELDER -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
            case KVOTE_FORELDREPENGER_ANNEN_FORELDER -> no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
            case UDEFINERT -> null;
            default -> throw new IllegalStateException("Ikke støttet oppholdårsak " + årsakType);
        };
    }

    public static Stønadskontotype map(UttakPeriodeType uttakPeriodeType) {
        return UTTAK_PERIODE_TYPE_MAPPER
            .map(uttakPeriodeType)
            .orElse(null);
    }

    private static KodeMapper<UttakPeriodeType, Stønadskontotype> initUttakPeriodeTypeMapper() {
        return KodeMapper
            .medMapping(UttakPeriodeType.FORELDREPENGER, Stønadskontotype.FORELDREPENGER)
            .medMapping(UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL, Stønadskontotype.FORELDREPENGER_FØR_FØDSEL)
            .medMapping(UttakPeriodeType.FELLESPERIODE, Stønadskontotype.FELLESPERIODE)
            .medMapping(UttakPeriodeType.MØDREKVOTE, Stønadskontotype.MØDREKVOTE)
            .medMapping(UttakPeriodeType.FEDREKVOTE, Stønadskontotype.FEDREKVOTE)
            .build();
    }

    public static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak map(UtsettelseÅrsak årsakType) {
        return UTSETTELSE_ÅRSAK_MAPPER
            .map(årsakType)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet utsettelseårsak" + årsakType.getKode()));
    }

    private static KodeMapper<UtsettelseÅrsak, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak> initUtsettelseÅrsakMapper() {
        return KodeMapper
            .medMapping(FERIE, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.FERIE)
            .medMapping(ARBEID, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.ARBEID)
            .medMapping(SYKDOM, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.SYKDOM_SKADE)
            .medMapping(INSTITUSJON_SØKER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.INNLAGT_SØKER)
            .medMapping(INSTITUSJON_BARN, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.INNLAGT_BARN)
            .medMapping(HV_OVELSE, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.HV_OVELSE)
            .medMapping(NAV_TILTAK, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.NAV_TILTAK)
            .medMapping(FRI, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak.FRI)
            .build();
    }

    private static KodeMapper<UttakUtsettelseType, UtsettelseÅrsak> initUttakTilOppgittUtsettelseMapper() {
        return KodeMapper
            .medMapping(UttakUtsettelseType.FERIE, UtsettelseÅrsak.FERIE)
            .medMapping(UttakUtsettelseType.ARBEID, UtsettelseÅrsak.ARBEID)
            .medMapping(UttakUtsettelseType.SYKDOM_SKADE, UtsettelseÅrsak.SYKDOM)
            .medMapping(UttakUtsettelseType.SØKER_INNLAGT, UtsettelseÅrsak.INSTITUSJON_SØKER)
            .medMapping(UttakUtsettelseType.BARN_INNLAGT, UtsettelseÅrsak.INSTITUSJON_BARN)
            .medMapping(UttakUtsettelseType.HV_OVELSE, UtsettelseÅrsak.HV_OVELSE)
            .medMapping(UttakUtsettelseType.NAV_TILTAK, UtsettelseÅrsak.NAV_TILTAK)
            .medMapping(UttakUtsettelseType.FRI, UtsettelseÅrsak.FRI)
            .build();
    }

    private static KodeMapper<StønadskontoType, UttakPeriodeType> initPeriodeTypeMapper() {
        return KodeMapper
            .medMapping(StønadskontoType.FEDREKVOTE, UttakPeriodeType.FEDREKVOTE)
            .medMapping(StønadskontoType.FELLESPERIODE, UttakPeriodeType.FELLESPERIODE)
            .medMapping(StønadskontoType.FORELDREPENGER_FØR_FØDSEL, UttakPeriodeType.FORELDREPENGER_FØR_FØDSEL)
            .medMapping(StønadskontoType.MØDREKVOTE, UttakPeriodeType.MØDREKVOTE)
            .medMapping(StønadskontoType.FORELDREPENGER, UttakPeriodeType.FORELDREPENGER)
            .medMapping(StønadskontoType.UDEFINERT, UttakPeriodeType.UDEFINERT)
            .build();
    }

    public static PeriodeVurderingType map(UttakPeriodeVurderingType uttakPeriodeVurderingType) {
        return VURDERING_TYPE_MAPPER
            .map(uttakPeriodeVurderingType)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet periodevurdering " + uttakPeriodeVurderingType.getKode()));
    }

    private static KodeMapper<UttakPeriodeVurderingType, PeriodeVurderingType> initVurderingTypeMapper() {
        return KodeMapper
            .medMapping(UttakPeriodeVurderingType.PERIODE_OK, PeriodeVurderingType.PERIODE_OK)
            .medMapping(UttakPeriodeVurderingType.PERIODE_OK_ENDRET, PeriodeVurderingType.ENDRE_PERIODE)
            .medMapping(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES, PeriodeVurderingType.UAVKLART_PERIODE)
            .medMapping(UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT, PeriodeVurderingType.IKKE_VURDERT)
            .build();
    }

    public static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak map(OverføringÅrsak overføringÅrsak) {
        return OVERFØRING_ÅRSAK_MAPPER
            .map(overføringÅrsak)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet overføringårsak " + overføringÅrsak.getKode()));
    }

    public static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet map(MorsAktivitet morsAktivitet) {
        //Ikke relevant for regler
        if (MorsAktivitet.UDEFINERT.equals(morsAktivitet) || MorsAktivitet.SAMTIDIGUTTAK.equals(morsAktivitet)) {
            return null;
        }
        return MORS_AKTIVITET_MAPPER
            .map(morsAktivitet)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet mors aktivitet " + morsAktivitet.getKode()));
    }

    private static KodeMapper<OverføringÅrsak, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak> initOverføringÅrsakMapper() {
        return KodeMapper
            .medMapping(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.INNLEGGELSE)
            .medMapping(OverføringÅrsak.SYKDOM_ANNEN_FORELDER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.SYKDOM_ELLER_SKADE)
            .medMapping(OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.ANNEN_FORELDER_IKKE_RETT)
            .medMapping(OverføringÅrsak.ALENEOMSORG, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.ALENEOMSORG)
            .build();
    }

    private static KodeMapper<MorsAktivitet, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet> initMorsAktivitetMapper() {
        return KodeMapper
            .medMapping(MorsAktivitet.ARBEID, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.ARBEID)
            .medMapping(MorsAktivitet.ARBEID_OG_UTDANNING, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.ARBEID_OG_UTDANNING)
            .medMapping(MorsAktivitet.UTDANNING, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.UTDANNING)
            .medMapping(MorsAktivitet.UFØRE, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.UFØRE)
            .medMapping(MorsAktivitet.INNLAGT, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.INNLAGT)
            .medMapping(MorsAktivitet.TRENGER_HJELP, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.SYK)
            .medMapping(MorsAktivitet.INTROPROG, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.INTROPROG)
            .medMapping(MorsAktivitet.KVALPROG, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.MorsAktivitet.KVALPROG)
            .build();
    }

    public static UttakUtsettelseType map(no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak utsettelseårsaktype) {
        return switch (utsettelseårsaktype) {
            case ARBEID -> UttakUtsettelseType.ARBEID;
            case FERIE -> UttakUtsettelseType.FERIE;
            case INNLAGT_BARN -> UttakUtsettelseType.BARN_INNLAGT;
            case SYKDOM_SKADE -> UttakUtsettelseType.SYKDOM_SKADE;
            case INNLAGT_SØKER -> UttakUtsettelseType.SØKER_INNLAGT;
            case HV_OVELSE -> UttakUtsettelseType.HV_OVELSE;
            case NAV_TILTAK -> UttakUtsettelseType.NAV_TILTAK;
            case FRI -> UttakUtsettelseType.FRI;
        };
    }

    public static PeriodeResultatType map(Perioderesultattype perioderesultatType) {
        return switch (perioderesultatType) {
            case INNVILGET -> PeriodeResultatType.INNVILGET;
            case AVSLÅTT -> PeriodeResultatType.AVSLÅTT;
            case MANUELL_BEHANDLING -> PeriodeResultatType.MANUELL_BEHANDLING;
        };
    }

    public static StønadskontoType map(Stønadskontotype stønadskontotype) {
        if (stønadskontotype == null) {
            return StønadskontoType.UDEFINERT;
        }
        return switch (stønadskontotype) {
            case FEDREKVOTE -> StønadskontoType.FEDREKVOTE;
            case MØDREKVOTE -> StønadskontoType.MØDREKVOTE;
            case FELLESPERIODE -> StønadskontoType.FELLESPERIODE;
            case FORELDREPENGER -> StønadskontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL -> StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case FLERBARNSDAGER -> StønadskontoType.FLERBARNSDAGER;
        };
    }

    public static ManuellBehandlingÅrsak map(Manuellbehandlingårsak input) {
        if (input == null) {
            return ManuellBehandlingÅrsak.UKJENT;
        }
        return ManuellBehandlingÅrsak.fraKode(String.valueOf(input.getId()));
    }

    public static PeriodeResultatÅrsak map(no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.PeriodeResultatÅrsak årsak) {
        if (årsak == null) {
            return PeriodeResultatÅrsak.UKJENT;
        }
        return PeriodeResultatÅrsak.fraKode(String.valueOf(årsak.getId()));
    }

    public static GraderingAvslagÅrsak map(GraderingIkkeInnvilgetÅrsak graderingIkkeInnvilgetÅrsak) {
        if (graderingIkkeInnvilgetÅrsak == null) {
            return GraderingAvslagÅrsak.UKJENT;
        }
        return switch (graderingIkkeInnvilgetÅrsak) {
            case AVSLAG_PGA_SEN_SØKNAD -> GraderingAvslagÅrsak.FOR_SEN_SØKNAD;
            case AVSLAG_PGA_FOR_TIDLIG_GRADERING -> GraderingAvslagÅrsak.GRADERING_FØR_UKE_7;
        };
    }

    public static UttakArbeidType map(AktivitetType aktivitetType) {
        return switch (aktivitetType) {
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakArbeidType.FRILANS;
            case ARBEID -> UttakArbeidType.ORDINÆRT_ARBEID;
            case ANNET -> UttakArbeidType.ANNET;
        };
    }


    public static Arbeidsgiver mapArbeidsgiver(AktivitetIdentifikator aktivitetIdentifikator) {
        if (aktivitetIdentifikator == null || aktivitetIdentifikator.getArbeidsgiverIdentifikator() == null) {
            throw new IllegalArgumentException("Arbeidsgiver ident kan ikke være null");
        }
        var arbeidsgiverIdentifikator = aktivitetIdentifikator.getArbeidsgiverIdentifikator();
        if (arbeidsgiverIdentifikator instanceof Orgnummer) {
            return Arbeidsgiver.virksomhet(arbeidsgiverIdentifikator.value());
        }
        return Arbeidsgiver.person(new AktørId(arbeidsgiverIdentifikator.value()));
    }

    public static UttakPeriodeType mapTilYf(StønadskontoType stønadskontoType) {
        return PERIODE_TYPE_MAPPER.map(stønadskontoType).orElseThrow(() -> new IllegalArgumentException("Ukjent stønadskontoType " + stønadskontoType));
    }

    public static Optional<UtsettelseÅrsak> mapTilYf(UttakUtsettelseType utsettelseType) {
        return UTTAK_TIL_OPPGITT_UTSETTELSE_MAPPER.map(utsettelseType);
    }

    public static PeriodeMedAvklartMorsAktivitet.Resultat map(KontrollerAktivitetskravAvklaring avklaring) {
        if (avklaring == null) {
            return null;
        }
        return switch (avklaring) {
            case I_AKTIVITET -> PeriodeMedAvklartMorsAktivitet.Resultat.I_AKTIVITET;
            case IKKE_I_AKTIVITET_DOKUMENTERT -> PeriodeMedAvklartMorsAktivitet.Resultat.IKKE_I_AKTIVITET_DOKUMENTERT;
            case IKKE_I_AKTIVITET_IKKE_DOKUMENTERT -> PeriodeMedAvklartMorsAktivitet.Resultat.IKKE_I_AKTIVITET_IKKE_DOKUMENTERT;
        };
    }
}
