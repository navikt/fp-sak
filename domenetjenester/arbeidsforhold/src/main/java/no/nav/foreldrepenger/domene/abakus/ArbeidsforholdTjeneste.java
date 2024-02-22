package no.nav.foreldrepenger.domene.abakus;

import static java.util.stream.Collectors.flatMapping;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.abakus.iaygrunnlag.AktørIdPersonident;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeid.v1.PermisjonDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsavtaleDto;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.abakus.iaygrunnlag.request.AktørDatoRequest;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.ArbeidsforholdMedPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.PermisjonsbeskrivelseType;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.EksternArbeidsforholdRef;

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

    public List<ArbeidsforholdMedPermisjon> hentArbeidsforholdInfoForEnPeriode(AktørId ident, LocalDate fradato, LocalDate tildato, FagsakYtelseType ytelseType) {
        var ytelse = FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType) ? YtelseType.SVANGERSKAPSPENGER : YtelseType.FORELDREPENGER;
        var request = new AktørDatoRequest(new AktørIdPersonident(ident.getId()), new Periode(tildato, fradato), ytelse);

        return abakusTjeneste.hentArbeidsforholdIPerioden(request).stream()
            .filter(af -> !ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(af.getType()))
            .map(this::mapArbeidsforholdMedPermisjon)
            .toList();
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

    private  ArbeidsforholdMedPermisjon mapArbeidsforholdMedPermisjon(ArbeidsforholdDto dto) {
        return new ArbeidsforholdMedPermisjon(mapTilArbeidsgiver(dto), no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.fraKode(dto.getType().getKode()), EksternArbeidsforholdRef.ref(dto.getArbeidsforholdId().getEksternReferanse()),
            dto.getArbeidsavtaler().stream().map(this::mapTilAktivitetsavtale).toList(), dto.getPermisjoner().stream().map(this::mapTilPermisjon).toList());
    }

    private Permisjon mapTilPermisjon(PermisjonDto permisjonDto) {
        return new Permisjon(DatoIntervallEntitet.fraOgMedTilOgMed(permisjonDto.getPeriode().getFom(), permisjonDto.getPeriode().getTom()), PermisjonsbeskrivelseType.fraKode(permisjonDto.getType().getKode()), permisjonDto.getProsentsats());
    }

    private AktivitetAvtale mapTilAktivitetsavtale(ArbeidsavtaleDto arbeidsavtaleDto) {
        return new AktivitetAvtale(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsavtaleDto.periode().getFom(), arbeidsavtaleDto.periode().getTom()), arbeidsavtaleDto.stillingsprosent());
    }

    public record AktivitetAvtale(DatoIntervallEntitet periode, BigDecimal stillingsprosent) {}
    public record Permisjon(DatoIntervallEntitet periode, PermisjonsbeskrivelseType type, BigDecimal prosent) {}

}
