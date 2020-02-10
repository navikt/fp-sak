package no.nav.foreldrepenger.domene.abakus;

import static java.util.stream.Collectors.flatMapping;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;

@ApplicationScoped
public class ArbeidsforholdTjeneste {

    private AbakusTjeneste abakusTjeneste;

    ArbeidsforholdTjeneste() {
        // CDI proxy
    }

    @Inject
    public ArbeidsforholdTjeneste(AbakusTjeneste abakusTjeneste) {
        this.abakusTjeneste = abakusTjeneste;
    }

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> finnArbeidsforholdForIdentPåDag(AktørId ident, LocalDate dato) {
        final var request = new AktørDatoRequest(new AktørIdPersonident(ident.getId()), new Periode(dato, dato));

        return abakusTjeneste.hentArbeidsforholdIPerioden(request)
            .stream()
            .collect(Collectors.groupingBy(this::mapTilArbeidsgiver,
                flatMapping(im -> Stream.of(EksternArbeidsforholdRef.ref(im.getArbeidsforholdId() != null ? im.getArbeidsforholdId().getEksternReferanse() : null)), Collectors.toSet())));
    }

    private Arbeidsgiver mapTilArbeidsgiver(ArbeidsforholdDto arbeidsforhold) {
        final var arbeidsgiver = arbeidsforhold.getArbeidsgiver();
        if (arbeidsgiver.getErOrganisasjon()) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getIdent());
        } else if (arbeidsgiver.getErPerson()) {
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }
        throw new IllegalArgumentException("Arbeidsgiver er verken person eller organisasjon");
    }
}
