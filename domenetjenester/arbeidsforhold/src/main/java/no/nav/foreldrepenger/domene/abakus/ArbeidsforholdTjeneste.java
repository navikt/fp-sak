package no.nav.foreldrepenger.domene.abakus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.flatMapping;

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

    public Map<Arbeidsgiver, Set<EksternArbeidsforholdRef>> finnArbeidsforholdForIdentPåDag(AktørId ident, LocalDate dato,
            FagsakYtelseType ytelseType) {
        var ytelse = FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType) ? YtelseType.SVANGERSKAPSPENGER : YtelseType.FORELDREPENGER;
        var request = new AktørDatoRequest(new AktørIdPersonident(ident.getId()), new Periode(dato, dato), ytelse);

        return abakusTjeneste.hentArbeidsforholdIPerioden(request).stream()
                .filter(af -> !ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(af.getType()))
                .collect(Collectors.groupingBy(this::mapTilArbeidsgiver,
                        flatMapping(
                                im -> Stream.of(EksternArbeidsforholdRef
                                        .ref(im.getArbeidsforholdId() != null ? im.getArbeidsforholdId().getEksternReferanse() : null)),
                                Collectors.toSet())));
    }

    private Arbeidsgiver mapTilArbeidsgiver(ArbeidsforholdDto arbeidsforhold) {
        var arbeidsgiver = arbeidsforhold.getArbeidsgiver();
        if (arbeidsgiver.getErOrganisasjon()) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getIdent());
        }
        if (arbeidsgiver.getErPerson()) {
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }
        throw new IllegalArgumentException("Arbeidsgiver er verken person eller organisasjon");
    }
}
