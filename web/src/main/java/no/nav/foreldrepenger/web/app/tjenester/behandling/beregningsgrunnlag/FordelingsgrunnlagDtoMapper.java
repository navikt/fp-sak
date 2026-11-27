package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.kalkulus.response.fordelingsgrunnlag.gui.FordelingGrunnlagDto;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagPrStatusOgAndel;

final class FordelingsgrunnlagDtoMapper {

    private FordelingsgrunnlagDtoMapper() {
    }

    static FordelingsgrunnlagDto map(FordelingGrunnlagDto fordelingsgrunnlag, Optional<Beregningsgrunnlag> beregningsgrunnlag) {
        var tilkomneArbeidsforhold = beregningsgrunnlag.map(Beregningsgrunnlag::getBeregningsgrunnlagPerioder).orElse(List.of()).stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .filter(andel -> AktivitetStatus.ARBEIDSTAKER.equals(andel.getAktivitetStatus()))
            .filter(BeregningsgrunnlagPrStatusOgAndel::erTilkommetAndel)
            .flatMap(andel -> andel.getBgAndelArbeidsforhold().stream())
            .map(arbeidsforhold -> new ArbeidsforholdNøkkel(arbeidsforhold.getArbeidsgiver().getIdentifikator(),
                arbeidsforhold.getArbeidsforholdRef().getReferanse()))
            .collect(Collectors.toSet());

        var refusjonsandeler = fordelingsgrunnlag.refusjonsandeler().stream()
            .map(andel -> new RefusjonsandelDto(andel.arbeidsgiverIdent(), andel.arbeidsforholdId(), andel.kilde(),
                andel.journalpostId(), andel.refusjonsperioder(),
                tilkomneArbeidsforhold.contains(new ArbeidsforholdNøkkel(andel.arbeidsgiverIdent(), andel.arbeidsforholdId()))))
            .toList();
        return new FordelingsgrunnlagDto(refusjonsandeler);
    }

    private record ArbeidsforholdNøkkel(String arbeidsgiverIdent, String arbeidsforholdId) {
    }
}
