package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsgiverDto;

@ApplicationScoped
public class FaktaUttakArbeidsforholdTjeneste {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    FaktaUttakArbeidsforholdTjeneste() {
        // CDI
    }

    @Inject
    public FaktaUttakArbeidsforholdTjeneste(ArbeidsgiverTjeneste arbeidsgiverTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
    }

    public List<ArbeidsforholdDto> hentArbeidsforhold(UttakInput input) {
        return input.getBeregningsgrunnlagStatuser().stream()
            .map(this::map)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .distinct()
            .collect(Collectors.toList());
    }

    private Optional<ArbeidsforholdDto> map(BeregningsgrunnlagStatus statusPeriode) {

        if (statusPeriode.erFrilanser()) {
            return Optional.of(ArbeidsforholdDto.frilans());
        }
        if (statusPeriode.erSelvstendigNæringsdrivende()) {
            return Optional.of(ArbeidsforholdDto.selvstendigNæringsdrivende());
        }
        if (statusPeriode.erArbeidstaker()) {
            return mapArbeidstaker(statusPeriode);
        }
        return Optional.empty();
    }

    private Optional<ArbeidsforholdDto> mapArbeidstaker(BeregningsgrunnlagStatus andel) {
        var arbeidsgiverOpt = andel.getArbeidsgiver();
        if (arbeidsgiverOpt.isEmpty()) {
            return Optional.empty();
        }
        var arbeidsgiver = arbeidsgiverOpt.get();
        ArbeidsforholdDto dto;
        if (arbeidsgiver.getErVirksomhet()) {
            dto = virksomhetArbeidsgiver(arbeidsgiver);
        } else {
            dto = mapPersonArbeidsgiver(arbeidsgiver);
        }
        return Optional.of(dto);
    }

    private ArbeidsforholdDto virksomhetArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        return ArbeidsforholdDto.ordinært(ArbeidsgiverDto.virksomhet(opplysninger.getIdentifikator(), opplysninger.getNavn()));
    }

    private ArbeidsforholdDto mapPersonArbeidsgiver(Arbeidsgiver arbeidsgiver) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arbeidsgiver);
        return ArbeidsforholdDto.ordinært(ArbeidsgiverDto.person(opplysninger.getNavn(), arbeidsgiver.getAktørId(), opplysninger.getFødselsdato()));
    }
}
