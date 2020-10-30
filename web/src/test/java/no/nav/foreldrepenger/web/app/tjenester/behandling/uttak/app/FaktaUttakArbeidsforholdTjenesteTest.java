package no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.app;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.ScenarioMorSøkerForeldrepenger;
import no.nav.foreldrepenger.behandlingslager.uttak.UttakArbeidType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Virksomhet;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.foreldrepenger.domene.uttak.input.BeregningsgrunnlagStatus;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.web.app.tjenester.behandling.uttak.dto.ArbeidsforholdDto;

@ExtendWith(MockitoExtension.class)
public class FaktaUttakArbeidsforholdTjenesteTest extends EntityManagerAwareTest {

    private static final String NAVN = "Person Navn";
    private static final LocalDate FØDSEL = LocalDate.of(2000, 1, 1);

    @Mock
    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;

    @Test
    public void skalReturnereArbeidsforhold() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));

        String virksomhetOrgnr1 = "123";
        String virksomhetOrgnr2 = "456";
        Virksomhet virksomhet1 = lagVirksomhet(virksomhetOrgnr1, "navn");
        Virksomhet virksomhet2 = lagVirksomhet(virksomhetOrgnr2, "navn2");

        AktørId aktørId = AktørId.dummy();
        Arbeidsgiver virksomhet123 = Arbeidsgiver.virksomhet(virksomhetOrgnr1);
        Arbeidsgiver virksomhet456 = Arbeidsgiver.virksomhet(virksomhetOrgnr2);
        Arbeidsgiver person = Arbeidsgiver.person(aktørId);

        Mockito.when(arbeidsgiverTjeneste.hent(person)).thenReturn(new ArbeidsgiverOpplysninger(aktørId.getId(), NAVN, FØDSEL));
        Mockito.when(arbeidsgiverTjeneste.hent(virksomhet123)).thenReturn(new ArbeidsgiverOpplysninger(virksomhet123.getOrgnr(), virksomhet1.getNavn()));
        Mockito.when(arbeidsgiverTjeneste.hent(virksomhet456)).thenReturn(new ArbeidsgiverOpplysninger(virksomhet456.getOrgnr(), virksomhet2.getNavn()));

        var input = new UttakInput(lagReferanse(behandling), null, null)
                .medBeregningsgrunnlagStatuser(Set.of(
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, virksomhet123, InternArbeidsforholdRef.nyRef()),
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, virksomhet123, InternArbeidsforholdRef.nyRef()),
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, virksomhet456, null),
                    new BeregningsgrunnlagStatus(AktivitetStatus.ARBEIDSTAKER, person, null),
                    new BeregningsgrunnlagStatus(AktivitetStatus.FRILANSER),
                    new BeregningsgrunnlagStatus(AktivitetStatus.SELVSTENDIG_NÆRINGSDRIVENDE)
                ));

        FaktaUttakArbeidsforholdTjeneste tjeneste = tjeneste();

        List<ArbeidsforholdDto> arbeidsforhold = tjeneste.hentArbeidsforhold(input);

        assertThat(arbeidsforhold).hasSize(5);
        var dtoForVirksomhet123 = finnDtoFor(arbeidsforhold, UttakArbeidType.ORDINÆRT_ARBEID, virksomhet123);
        assertThat(dtoForVirksomhet123.getArbeidsgiver().getNavn()).isEqualTo(virksomhet1.getNavn());

        var dtoForVirksomhet456 = finnDtoFor(arbeidsforhold, UttakArbeidType.ORDINÆRT_ARBEID, virksomhet456);
        assertThat(dtoForVirksomhet456.getArbeidsgiver().getNavn()).isEqualTo(virksomhet2.getNavn());

        var dtoForPerson = finnDtoFor(arbeidsforhold, UttakArbeidType.ORDINÆRT_ARBEID, person);
        assertThat(dtoForPerson.getArbeidsgiver().getIdentifikator()).isNull();
        assertThat(dtoForPerson.getArbeidsgiver().getNavn()).isEqualTo(NAVN);
        assertThat(dtoForPerson.getArbeidsgiver().getAktørId()).isEqualTo(aktørId);

        var dtoForFrilans = finnDtoFor(arbeidsforhold, UttakArbeidType.FRILANS, null);
        assertThat(dtoForFrilans).isNotNull();

        var dtoForSN = finnDtoFor(arbeidsforhold, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null);
        assertThat(dtoForSN).isNotNull();
    }

    private ArbeidsforholdDto finnDtoFor(List<ArbeidsforholdDto> arbeidsforhold, UttakArbeidType arbeidType, Arbeidsgiver arbeidsgiver) {
        var dtoSet = arbeidsforhold.stream().filter(a -> a.getArbeidType().equals(arbeidType)).collect(Collectors.toSet());
        if (arbeidsgiver != null) {
            if (arbeidsgiver.getErVirksomhet()) {
                dtoSet = dtoSet.stream()
                    .filter(a -> a.getArbeidsgiver().isVirksomhet())
                    .filter(a -> a.getArbeidsgiver().getIdentifikator().equals(arbeidsgiver.getIdentifikator()))
                    .collect(Collectors.toSet());
            } else {
                dtoSet = dtoSet.stream()
                    .filter(a -> !a.getArbeidsgiver().isVirksomhet())
                    .filter(a -> a.getArbeidsgiver().getAktørId().equals(arbeidsgiver.getAktørId()))
                    .collect(Collectors.toSet());
            }
        }
        if (dtoSet.size() != 1) {
            throw new IllegalStateException("Fint ikke akkurat 1");
        }
        return dtoSet.stream().findFirst().orElseThrow();
    }

    @Test
    public void skalHåndtereAnnenStatusEnnArbeidstakerFrilansOgSelvstendigNæringsdrivende() {
        ScenarioMorSøkerForeldrepenger scenario = ScenarioMorSøkerForeldrepenger.forFødsel();
        Behandling behandling = scenario.lagre(new BehandlingRepositoryProvider(getEntityManager()));

        FaktaUttakArbeidsforholdTjeneste tjeneste = tjeneste();

        var input = new UttakInput(lagReferanse(behandling), null, null)
                .medBeregningsgrunnlagStatuser(Set.of(new BeregningsgrunnlagStatus(AktivitetStatus.DAGPENGER)));
        List<ArbeidsforholdDto> arbeidsforhold = tjeneste.hentArbeidsforhold(input);

        assertThat(arbeidsforhold).isEmpty();
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling, LocalDate.now());
    }

    private Virksomhet lagVirksomhet(String orgnr, String navn) {
        return new Virksomhet.Builder()
            .medOrgnr(orgnr)
            .medNavn(navn)
            .build();
    }

    private FaktaUttakArbeidsforholdTjeneste tjeneste() {
        return new FaktaUttakArbeidsforholdTjeneste(arbeidsgiverTjeneste);
    }

}
