package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@ExtendWith(MockitoExtension.class)
class FaktaUttakArbeidsforholdTjenesteTest extends EntityManagerAwareTest {

    private static final String NAVN = "Person Navn";
    private static final LocalDate FØDSEL = LocalDate.of(2000, 1, 1);

    @Test
    void skalReturnereArbeidsforhold() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));

        var virksomhetOrgnr1 = "123";
        var virksomhetOrgnr2 = "456";
        var virksomhet1 = lagVirksomhet(virksomhetOrgnr1, "navn");
        var virksomhet2 = lagVirksomhet(virksomhetOrgnr2, "navn2");

        var aktørId = AktørId.dummy();
        var virksomhet123 = Arbeidsgiver.virksomhet(virksomhetOrgnr1);
        var virksomhet456 = Arbeidsgiver.virksomhet(virksomhetOrgnr2);
        var person = Arbeidsgiver.person(aktørId);

        var input = new UttakInput(BehandlingReferanse.fra(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build(), null, null)
                .medBeregningsgrunnlagStatuser(Set.of(
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, virksomhet123, InternArbeidsforholdRef.nyRef()),
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, virksomhet123, InternArbeidsforholdRef.nyRef()),
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, virksomhet456, null),
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, person, null),
                    new BeregningsgrunnlagStatus(AktivitetStatus.FRILANSER),
                    new BeregningsgrunnlagStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                ));

        var arbeidsforhold = FaktaUttakArbeidsforholdTjeneste.hentArbeidsforhold(input);

        assertThat(arbeidsforhold).hasSize(5);
        var dtoForVirksomhet123 = finnDtoFor(arbeidsforhold, UttakArbeidType.ORDINÆRT_ARBEID, virksomhet123);
        assertThat(dtoForVirksomhet123.arbeidsgiverReferanse()).isEqualTo(virksomhetOrgnr1);

        var dtoForVirksomhet456 = finnDtoFor(arbeidsforhold, UttakArbeidType.ORDINÆRT_ARBEID, virksomhet456);
        assertThat(dtoForVirksomhet456.arbeidsgiverReferanse()).isEqualTo(virksomhetOrgnr2);

        var dtoForPerson = finnDtoFor(arbeidsforhold, UttakArbeidType.ORDINÆRT_ARBEID, person);
        assertThat(dtoForPerson.arbeidsgiverReferanse()).isEqualTo(aktørId.getId());

        var dtoForFrilans = finnDtoFor(arbeidsforhold, UttakArbeidType.FRILANS, null);
        assertThat(dtoForFrilans).isNotNull();

        var dtoForSN = finnDtoFor(arbeidsforhold, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null);
        assertThat(dtoForSN).isNotNull();
    }

    private ArbeidsforholdDto finnDtoFor(List<ArbeidsforholdDto> arbeidsforhold, UttakArbeidType arbeidType, Arbeidsgiver arbeidsgiver) {
        var dtoSet = arbeidsforhold.stream().filter(a -> a.arbeidType().equals(arbeidType)).collect(Collectors.toSet());
        if (arbeidsgiver != null) {
            dtoSet = dtoSet.stream()
                .filter(a -> a.arbeidsgiverReferanse().equals(arbeidsgiver.getIdentifikator()))
                .collect(Collectors.toSet());
        }
        if (dtoSet.size() != 1) {
            throw new IllegalStateException("Fint ikke akkurat 1");
        }
        return dtoSet.stream().findFirst().orElseThrow();
    }

    @Test
    void skalHåndtereAnnenStatusEnnArbeidstakerFrilansOgSelvstendigNæringsdrivende() {
        var scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        var behandling = scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));


        var input = new UttakInput(BehandlingReferanse.fra(behandling), Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(LocalDate.now()).build(), null, null)
                .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.DAGPENGER)));
        var arbeidsforhold = FaktaUttakArbeidsforholdTjeneste.hentArbeidsforhold(input);

        assertThat(arbeidsforhold).isEmpty();
    }

    private Virksomhet lagVirksomhet(String orgnr, String navn) {
        return new Virksomhet.Builder()
            .medOrgnr(orgnr)
            .medNavn(navn)
            .build();
    }

}
