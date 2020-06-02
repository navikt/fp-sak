package no.nav.foreldrepenger.domene.uttak;

import static no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak.*;

import java.util.Objects;
import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.OppgittDekningsgradEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.FordelingPeriodeKilde;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.periode.UttakPeriodeVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OppholdÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.OverføringÅrsak;
import no.nav.foreldrepenger.behandlingslager.behandling.ytelsefordeling.årsak.UtsettelseÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.GraderingAvslagÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.IkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.InnvilgetÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.ManuellBehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.PeriodeResultatÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakAktivitetEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.UttakUtsettelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.regler.uttak.beregnkontoer.grunnlag.Dekningsgrad;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.Trekkdager;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetIdentifikator;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.AktivitetType;
import no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.PeriodeKilde;
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


    private UttakEnumMapper() {

    }

    public static Perioderesultattype map(PeriodeResultatType periodeResultatType) {
        if (PeriodeResultatType.INNVILGET.equals(periodeResultatType)) {
            return Perioderesultattype.INNVILGET;
        }
        if (PeriodeResultatType.AVSLÅTT.equals(periodeResultatType)) {
            return Perioderesultattype.AVSLÅTT;
        }
        if (PeriodeResultatType.MANUELL_BEHANDLING.equals(periodeResultatType)) {
            return Perioderesultattype.MANUELL_BEHANDLING;
        }
        throw new IllegalStateException("Ukjent type " + periodeResultatType);
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

    public static AktivitetIdentifikator map(UttakArbeidType uttakArbeidType, Optional<Arbeidsgiver> arbeidsgiver, InternArbeidsforholdRef ref) {
        if (uttakArbeidType.equals(UttakArbeidType.FRILANS)) {
            return AktivitetIdentifikator.forFrilans();
        }
        if (uttakArbeidType.equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE)) {
            return AktivitetIdentifikator.forSelvstendigNæringsdrivende();
        }
        if (uttakArbeidType.equals(UttakArbeidType.ANNET)) {
            return AktivitetIdentifikator.annenAktivitet();
        }
        if (uttakArbeidType.equals(UttakArbeidType.ORDINÆRT_ARBEID)) {
            AktivitetIdentifikator.ArbeidsgiverType arbeidsgiverType = arbeidsgiver.map(a -> {
                if (a.getErVirksomhet()) {
                    return AktivitetIdentifikator.ArbeidsgiverType.VIRKSOMHET;
                }
                return AktivitetIdentifikator.ArbeidsgiverType.PERSON;
            }).orElse(null);
            return AktivitetIdentifikator.forArbeid(arbeidsgiver.map(Arbeidsgiver::getIdentifikator).orElse(null), ref.getReferanse(), arbeidsgiverType);
        }
        throw new IllegalStateException("Ukjent uttakarbeidtype " + uttakArbeidType);
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
        if (OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER.equals(årsakType)) {
            return no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.FEDREKVOTE_ANNEN_FORELDER;
        }
        if (OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER.equals(årsakType)) {
            return no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.MØDREKVOTE_ANNEN_FORELDER;
        }
        if (OppholdÅrsak.KVOTE_FELLESPERIODE_ANNEN_FORELDER.equals(årsakType)) {
            return no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.FELLESPERIODE_ANNEN_FORELDER;
        }
        if (OppholdÅrsak.KVOTE_FORELDREPENGER_ANNEN_FORELDER.equals(årsakType)) {
            return no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OppholdÅrsak.FORELDREPENGER_ANNEN_FORELDER;
        }
        if (OppholdÅrsak.UDEFINERT.equals(årsakType)) {
            return null;
        }
        throw new IllegalStateException("Ikke støttet årsak " + årsakType);

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
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet årsak" + årsakType.getKode()));
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
            .build();
    }

    public static PeriodeVurderingType map(UttakPeriodeVurderingType uttakPeriodeVurderingType) {
        return VURDERING_TYPE_MAPPER
            .map(uttakPeriodeVurderingType)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet årsak " + uttakPeriodeVurderingType.getKode()));
    }

    private static KodeMapper<UttakPeriodeVurderingType, PeriodeVurderingType> initVurderingTypeMapper() {
        return KodeMapper
            .medMapping(UttakPeriodeVurderingType.PERIODE_OK, PeriodeVurderingType.PERIODE_OK)
            .medMapping(UttakPeriodeVurderingType.PERIODE_OK_ENDRET, PeriodeVurderingType.ENDRE_PERIODE)
            .medMapping(UttakPeriodeVurderingType.PERIODE_KAN_IKKE_AVKLARES, PeriodeVurderingType.UAVKLART_PERIODE)
            .medMapping(UttakPeriodeVurderingType.PERIODE_IKKE_VURDERT, PeriodeVurderingType.IKKE_VURDERT)
            .build();
    }

    public static PeriodeKilde map(FordelingPeriodeKilde fordelingPeriodeKilde) {
        if (FordelingPeriodeKilde.TIDLIGERE_VEDTAK.equals(fordelingPeriodeKilde)) {
            return PeriodeKilde.TIDLIGERE_VEDTAK;
        } else if (FordelingPeriodeKilde.SØKNAD.equals(fordelingPeriodeKilde)) {
            return PeriodeKilde.SØKNAD;
        }
        throw new UnsupportedOperationException("Har ikke støtte for periodekilde " + fordelingPeriodeKilde.getKode());
    }

    public static no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak map(OverføringÅrsak overføringÅrsak) {
        return OVERFØRING_ÅRSAK_MAPPER
            .map(overføringÅrsak)
            .orElseThrow(() -> new UnsupportedOperationException("Ikke støttet årsak " + overføringÅrsak.getKode()));
    }

    private static KodeMapper<OverføringÅrsak, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak> initOverføringÅrsakMapper() {
        return KodeMapper
            .medMapping(OverføringÅrsak.INSTITUSJONSOPPHOLD_ANNEN_FORELDER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.INNLEGGELSE)
            .medMapping(OverføringÅrsak.SYKDOM_ANNEN_FORELDER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.SYKDOM_ELLER_SKADE)
            .medMapping(OverføringÅrsak.IKKE_RETT_ANNEN_FORELDER, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.ANNEN_FORELDER_IKKE_RETT)
            .medMapping(OverføringÅrsak.ALENEOMSORG, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.OverføringÅrsak.ALENEOMSORG)
            .build();
    }

    public static UttakUtsettelseType map(no.nav.foreldrepenger.regler.uttak.fastsetteperiode.grunnlag.UtsettelseÅrsak utsettelseårsaktype) {
        switch (utsettelseårsaktype) {
            case ARBEID:
                return UttakUtsettelseType.ARBEID;
            case FERIE:
                return UttakUtsettelseType.FERIE;
            case INNLAGT_BARN:
                return UttakUtsettelseType.BARN_INNLAGT;
            case SYKDOM_SKADE:
                return UttakUtsettelseType.SYKDOM_SKADE;
            case INNLAGT_SØKER:
                return UttakUtsettelseType.SØKER_INNLAGT;
            case HV_OVELSE:
                return UttakUtsettelseType.HV_OVELSE;
            case NAV_TILTAK:
                return UttakUtsettelseType.NAV_TILTAK;
            default:
                throw new IllegalArgumentException("Utvikler-feil: Kom ut av regel med perioderesultattype " + utsettelseårsaktype);
        }
    }

    public static PeriodeResultatType map(Perioderesultattype perioderesultatType) {
        switch (perioderesultatType) {
            case INNVILGET:
                return PeriodeResultatType.INNVILGET;
            case AVSLÅTT:
                return PeriodeResultatType.AVSLÅTT;
            case MANUELL_BEHANDLING:
                return PeriodeResultatType.MANUELL_BEHANDLING;
            default:
                throw new IllegalArgumentException("Utvikler-feil: Kom ut av regel med perioderesultattype " + perioderesultatType);
        }
    }

    public static StønadskontoType map(Stønadskontotype stønadskontotype) {
        if (stønadskontotype == null) {
            return StønadskontoType.UDEFINERT;
        }
        switch (stønadskontotype) {
            case FEDREKVOTE:
                return StønadskontoType.FEDREKVOTE;
            case MØDREKVOTE:
                return StønadskontoType.MØDREKVOTE;
            case FELLESPERIODE:
                return StønadskontoType.FELLESPERIODE;
            case FORELDREPENGER:
                return StønadskontoType.FORELDREPENGER;
            case FORELDREPENGER_FØR_FØDSEL:
                return StønadskontoType.FORELDREPENGER_FØR_FØDSEL;
            case FLERBARNSDAGER:
                return StønadskontoType.FLERBARNSDAGER;
            default:
                throw new IllegalArgumentException("Støtter ikke Stønadskontotype: " + stønadskontotype);
        }
    }

    public static ManuellBehandlingÅrsak map(Manuellbehandlingårsak input) {
        if (input == null) {
            return ManuellBehandlingÅrsak.UKJENT;
        }
        return ManuellBehandlingÅrsak.fraKode(String.valueOf(input.getId()));
    }

    public static PeriodeResultatÅrsak map(PeriodeResultatType periodeResultatType, no.nav.foreldrepenger.regler.uttak.fastsetteperiode.utfall.PeriodeResultatÅrsak årsak) {
        if (årsak == null) {
            return PeriodeResultatÅrsak.UKJENT;
        }

        if (PeriodeResultatType.INNVILGET.equals(periodeResultatType)) {
            return InnvilgetÅrsak.fraKode(String.valueOf(årsak.getId()));
        }
        return IkkeOppfyltÅrsak.fraKode(String.valueOf(årsak.getId()));
    }

    public static GraderingAvslagÅrsak map(GraderingIkkeInnvilgetÅrsak graderingIkkeInnvilgetÅrsak) {
        if (graderingIkkeInnvilgetÅrsak == null) {
            return GraderingAvslagÅrsak.UKJENT;
        }
        return GraderingAvslagÅrsak.fraKode(String.valueOf(graderingIkkeInnvilgetÅrsak.getId()));
    }

    public static UttakArbeidType map(AktivitetType aktivitetType) {
        if (AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(aktivitetType)) {
            return UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
        }
        if (AktivitetType.FRILANS.equals(aktivitetType)) {
            return UttakArbeidType.FRILANS;
        }
        if (AktivitetType.ARBEID.equals(aktivitetType)) {
            return UttakArbeidType.ORDINÆRT_ARBEID;
        }
        if (AktivitetType.ANNET.equals(aktivitetType)) {
            return UttakArbeidType.ANNET;
        }
        throw new IllegalStateException("Ukjent aktivitetstype " + aktivitetType);
    }

    public static Arbeidsgiver mapArbeidsgiver(AktivitetIdentifikator aktivitetIdentifikator) {
        if (aktivitetIdentifikator == null || aktivitetIdentifikator.getArbeidsgiverIdentifikator() == null) {
            throw new IllegalArgumentException("Arbeidsgiver ident kan ikke være null");
        }
        return erVirksomhet(aktivitetIdentifikator) ? Arbeidsgiver.virksomhet(aktivitetIdentifikator.getArbeidsgiverIdentifikator())
            : Arbeidsgiver.person(new AktørId(aktivitetIdentifikator.getArbeidsgiverIdentifikator()));
    }

    private static boolean erVirksomhet(AktivitetIdentifikator aktivitetIdentifikator) {
        return aktivitetIdentifikator.getArbeidsgiverType().equals(AktivitetIdentifikator.ArbeidsgiverType.VIRKSOMHET);
    }
}
