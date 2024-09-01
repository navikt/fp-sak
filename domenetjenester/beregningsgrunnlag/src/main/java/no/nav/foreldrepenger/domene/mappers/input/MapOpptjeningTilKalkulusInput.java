package no.nav.foreldrepenger.domene.mappers.input;

import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.mappers.RelevantOpptjeningMapper;
import no.nav.foreldrepenger.domene.mappers.til_kalkulator.KodeverkTilKalkulusMapper;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;

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
