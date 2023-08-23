package no.nav.foreldrepenger.domene.uttak.svp;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.uttak.PeriodeResultatType;
import no.nav.foreldrepenger.behandlingslager.uttak.Utbetalingsgrad;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.uttak.svp.*;
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
        var orgnr = arbeidsforhold.getArbeidsgiverVirksomhetId();
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

    private UttakArbeidType mapTilUttakArbeidType(AktivitetType aktivitetType) {
        return switch (aktivitetType) {
            case ARBEID -> UttakArbeidType.ORDINÆRT_ARBEID;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
            case FRILANS -> UttakArbeidType.FRILANS;
            default -> UttakArbeidType.ANNET;
        };
    }

    private SvangerskapspengerUttakResultatPeriodeEntitet.Builder konverterPeriode(Uttaksperiode periode) {
        var periodeBuilder = new SvangerskapspengerUttakResultatPeriodeEntitet.Builder(periode.getFom(), periode.getTom());
        periodeBuilder.medRegelInput(periode.getRegelInput());
        periodeBuilder.medRegelEvaluering(periode.getRegelSporing());
        if (periode.getUtfallType().equals(UtfallType.OPPFYLT)) {
            periodeBuilder.medPeriodeResultatType(PeriodeResultatType.INNVILGET);
            periodeBuilder.medUtbetalingsgrad(new Utbetalingsgrad(periode.getUtbetalingsgrad()));
        } else {
            periodeBuilder.medPeriodeResultatType(PeriodeResultatType.AVSLÅTT);
            periodeBuilder.medUtbetalingsgrad(Utbetalingsgrad.ZERO);
            if (periode.getÅrsak() != null) {
                var periodeÅrsak = PeriodeIkkeOppfyltÅrsak.fraKode(String.valueOf(periode.getÅrsak().getId()));
                periodeBuilder.medPeriodeIkkeOppfyltÅrsak(periodeÅrsak);
            }
        }
        return periodeBuilder;
    }

}
