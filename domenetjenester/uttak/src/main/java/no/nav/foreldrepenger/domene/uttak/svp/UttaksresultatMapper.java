package no.nav.foreldrepenger.domene.uttak.svp;

import java.math.BigDecimal;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.ArbeidsforholdIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.PeriodeIkkeOppfyltÅrsak;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatArbeidsforholdEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatEntitet;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.SvangerskapspengerUttakResultatPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.svangerskapspenger.domene.felles.AktivitetType;
import no.nav.svangerskapspenger.domene.felles.Arbeidsforhold;
import no.nav.svangerskapspenger.domene.resultat.UtfallType;
import no.nav.svangerskapspenger.domene.resultat.Uttaksperiode;
import no.nav.svangerskapspenger.domene.resultat.Uttaksperioder;

@ApplicationScoped
class UttaksresultatMapper {

    @Inject
    UttaksresultatMapper() {
    }

    Uttaksperioder tilRegelmodell(SvangerskapspengerUttakResultatEntitet svangerskapspengerUttakResultatEntitet) {
        var uttaksperioder = new Uttaksperioder();

        svangerskapspengerUttakResultatEntitet.getUttaksResultatArbeidsforhold().forEach(urArbeidsforhold -> {

            urArbeidsforhold.getPerioder().forEach(urPeriode -> {
                var periode = new Uttaksperiode(urPeriode.getFom(), urPeriode.getTom(), urPeriode.getUtbetalingsgrad());
                Arbeidsforhold arbeidsforhold;
                if (urArbeidsforhold.getArbeidsgiver().erAktørId()) {
                    arbeidsforhold = Arbeidsforhold.aktør(mapTilAktivitetType(urArbeidsforhold.getUttakArbeidType()), urArbeidsforhold.getArbeidsgiver().getAktørId().getId(),
                        urArbeidsforhold.getArbeidsforholdRef() != null ? urArbeidsforhold.getArbeidsforholdRef().getReferanse() : null);
                } else {
                    arbeidsforhold = Arbeidsforhold.virksomhet(mapTilAktivitetType(urArbeidsforhold.getUttakArbeidType()), urArbeidsforhold.getArbeidsgiver().getOrgnr(), urArbeidsforhold.getArbeidsforholdRef().getReferanse());
                }
                uttaksperioder.leggTilPerioder(arbeidsforhold, periode);
            });

        });
        return uttaksperioder;
    }

    private AktivitetType mapTilAktivitetType(UttakArbeidType uttakArbeidType) {
        if (UttakArbeidType.ORDINÆRT_ARBEID.equals(uttakArbeidType)) {
            return AktivitetType.ARBEID;
        } else if(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE.equals(uttakArbeidType)) {
            return AktivitetType.SELVSTENDIG_NÆRINGSDRIVENDE;
        } else if(UttakArbeidType.FRILANS.equals(uttakArbeidType)) {
            return AktivitetType.FRILANS;
        }
        return AktivitetType.ANNET;
    }

    private UttakArbeidType mapTilUttakArbeidType(AktivitetType aktivitetType) {
        switch(aktivitetType) {
            case ARBEID:
                return UttakArbeidType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE:
                return UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS:
                return UttakArbeidType.FRILANS;
            default:
                return UttakArbeidType.ANNET;
        }
    }

    SvangerskapspengerUttakResultatEntitet tilEntiteter(Behandlingsresultat behandlingsresultat, Uttaksperioder uttaksperioder) {
        var resultatBuilder = new SvangerskapspengerUttakResultatEntitet.Builder(behandlingsresultat);
        uttaksperioder.alleArbeidsforhold()
            .forEach(arbeidsforhold -> resultatBuilder.medUttakResultatArbeidsforhold(konverterArbeidsforhold(uttaksperioder, arbeidsforhold).build()));
        return resultatBuilder.build();
    }

    private SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder konverterArbeidsforhold(Uttaksperioder uttaksperioder, Arbeidsforhold arbeidsforhold) {
        var arbeidsforholdBuilder = new SvangerskapspengerUttakResultatArbeidsforholdEntitet.Builder();
        arbeidsforholdBuilder.medUttakArbeidType(mapTilUttakArbeidType(arbeidsforhold.getAktivitetType()));
        var arbeidsforholdRef = InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId().orElse(null));
        String orgnr = arbeidsforhold.getArbeidsgiverVirksomhetId();
        if (orgnr != null) {
            arbeidsforholdBuilder.medArbeidsforhold(Arbeidsgiver.virksomhet(orgnr), arbeidsforholdRef);
        } else {
            if (arbeidsforhold.getArbeidsgiverAktørId() != null) {
                arbeidsforholdBuilder.medArbeidsforhold(Arbeidsgiver.person(new AktørId(arbeidsforhold.getArbeidsgiverAktørId())), arbeidsforholdRef);
            }
        }

        var uttaksperioderPerArbeidsforhold = uttaksperioder.perioder(arbeidsforhold);
        if (uttaksperioderPerArbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak() != null) {
            var årsak = ArbeidsforholdIkkeOppfyltÅrsak.fraKode(String.valueOf(uttaksperioderPerArbeidsforhold.getArbeidsforholdIkkeOppfyltÅrsak().getId()));
            arbeidsforholdBuilder.medArbeidsforholdIkkeOppfyltÅrsak(årsak);
        } else {
            uttaksperioderPerArbeidsforhold.getUttaksperioder().forEach(periode -> arbeidsforholdBuilder.medPeriode(konverterPeriode(periode).build()));
        }
        return arbeidsforholdBuilder;
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet.Builder konverterPeriode(Uttaksperiode periode) {
        var periodeBuilder = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(periode.getFom(), periode.getTom());
        periodeBuilder.medRegelInput(periode.getRegelInput());
        periodeBuilder.medRegelEvaluering(periode.getRegelSporing());
        if (periode.getUtfallType().equals(UtfallType.OPPFYLT)) {
            periodeBuilder.medPeriodeResultatType(PeriodeResultatType.INNVILGET);
            periodeBuilder.medUtbetalingsgrad(periode.getUtbetalingsgrad());
        } else {
            periodeBuilder.medPeriodeResultatType(PeriodeResultatType.AVSLÅTT);
            periodeBuilder.medUtbetalingsgrad(BigDecimal.ZERO);
            if (periode.getÅrsak() != null) {
                var periodeÅrsak = PeriodeIkkeOppfyltÅrsak.fraKode(String.valueOf(periode.getÅrsak().getId()));
                periodeBuilder.medPeriodeIkkeOppfyltÅrsak(periodeÅrsak);
            }
        }
        return periodeBuilder;
    }

}
