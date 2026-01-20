package no.nav.foreldrepenger.domene.mappers.til_kalkulus;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.opptjening.OpptjeningAktiviteterDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.opptjening.OpptjeningPeriodeDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Aktør;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.AktørIdPersonident;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.InternArbeidsforholdRefDto;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Organisasjon;
import no.nav.foreldrepenger.kalkulus.kontrakt.typer.Periode;

public class MapOpptjeningTilKalkulusInput {

    private MapOpptjeningTilKalkulusInput() {
        // Hindrer default konstruktør
    }

    public static OpptjeningAktiviteterDto mapOpptjening(OpptjeningAktiviteter opptjeningAktiviteter,
                                                           InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                           BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var relevanteAktiviteter = RelevantOpptjeningMapper.map(opptjeningAktiviteter, iayGrunnlag, ref, stp);
        return new OpptjeningAktiviteterDto(relevanteAktiviteter.stream()
            .map(opptjeningPeriode -> new OpptjeningPeriodeDto(
                KodeverkTilKalkulusMapper.mapOpptjeningAktivitetType(opptjeningPeriode.opptjeningAktivitetType()),
                new Periode(opptjeningPeriode.periode().getFom(), opptjeningPeriode.periode().getTom()),
                mapTilAktør(opptjeningPeriode),
                mapReferanse(opptjeningPeriode)))
            .toList());
    }

    private static InternArbeidsforholdRefDto mapReferanse(OpptjeningAktiviteter.OpptjeningPeriode opptjeningPeriode) {
        return opptjeningPeriode.arbeidsforholdId() != null
            && opptjeningPeriode.arbeidsforholdId().getReferanse() != null ? new InternArbeidsforholdRefDto(
            opptjeningPeriode.arbeidsforholdId().getReferanse()) : null;
    }

    private static Aktør mapTilAktør(OpptjeningAktiviteter.OpptjeningPeriode periode) {
        var orgNummer = periode.arbeidsgiverOrgNummer() != null ? new Organisasjon(periode.arbeidsgiverOrgNummer()) : null;
        if (orgNummer != null) {
            return orgNummer;
        }
        return periode.arbeidsgiverAktørId() != null ? new AktørIdPersonident(periode.arbeidsgiverAktørId()) : null;
    }
}
