package no.nav.foreldrepenger.domene.abakus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    public List<ArbeidsforholdMedPermisjon> hentArbeidsforholdInfoForEnPeriode(AktørId ident, LocalDate fradato, LocalDate tildato, FagsakYtelseType ytelseType) {
        var ytelse = FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType) ? YtelseType.SVANGERSKAPSPENGER : YtelseType.FORELDREPENGER;
        var request = new AktørDatoRequest(new AktørIdPersonident(ident.getId()), new Periode(fradato, tildato), ytelse);
        return abakusTjeneste.hentArbeidsforholdIPeriodenMedAvtalerOgPermisjoner(request).stream()
            .filter(af -> !ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER.equals(af.getType()))
            .map(ArbeidsforholdTjeneste::mapArbeidsforholdMedPermisjon)
            .toList();
    }

    private static Arbeidsgiver mapTilArbeidsgiver(ArbeidsforholdDto arbeidsforhold) {
        var arbeidsgiver = arbeidsforhold.getArbeidsgiver();
        if (arbeidsgiver.getErOrganisasjon()) {
            return Arbeidsgiver.virksomhet(arbeidsgiver.getIdent());
        }
        if (arbeidsgiver.getErPerson()) {
            return Arbeidsgiver.person(new AktørId(arbeidsgiver.getIdent()));
        }
        throw new IllegalArgumentException("Arbeidsgiver er verken person eller organisasjon");
    }

    private static ArbeidsforholdMedPermisjon mapArbeidsforholdMedPermisjon(ArbeidsforholdDto dto) {
        return new ArbeidsforholdMedPermisjon(
            mapTilArbeidsgiver(dto),
            no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType.fraKode(dto.getType().getKode()),
            dto.getArbeidsforholdId() != null ? EksternArbeidsforholdRef.ref(dto.getArbeidsforholdId().getEksternReferanse()) : EksternArbeidsforholdRef.nullRef(),
            tilAktivitetsavtale(dto.getArbeidsavtaler()),
            tilPermisjoner(dto.getPermisjoner()));
    }

    private static List<Permisjon> tilPermisjoner(List<PermisjonDto> permisjoner) {
        if (permisjoner == null) {
            return List.of();
        }
        return permisjoner.stream().map(ArbeidsforholdTjeneste::mapTilPermisjon).toList();
    }

    private static List<AktivitetAvtale> tilAktivitetsavtale(List<ArbeidsavtaleDto> arbeidsavtaler) {
        if (arbeidsavtaler == null) {
            return List.of();
        }
        return arbeidsavtaler.stream().map(ArbeidsforholdTjeneste::mapTilAktivitetsavtale).toList();
    }

    private static Permisjon mapTilPermisjon(PermisjonDto permisjonDto) {
        return new Permisjon(DatoIntervallEntitet.fraOgMedTilOgMed(permisjonDto.getPeriode().getFom(), permisjonDto.getPeriode().getTom()), PermisjonsbeskrivelseType.fraKode(permisjonDto.getType().getKode()), permisjonDto.getProsentsats());
    }

    private static AktivitetAvtale mapTilAktivitetsavtale(ArbeidsavtaleDto arbeidsavtaleDto) {
        return new AktivitetAvtale(DatoIntervallEntitet.fraOgMedTilOgMed(arbeidsavtaleDto.periode().getFom(), arbeidsavtaleDto.periode().getTom()), arbeidsavtaleDto.stillingsprosent());
    }

    public record AktivitetAvtale(DatoIntervallEntitet periode, BigDecimal stillingsprosent) {}
    public record Permisjon(DatoIntervallEntitet periode, PermisjonsbeskrivelseType type, BigDecimal prosent) {}

}
