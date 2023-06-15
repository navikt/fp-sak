package no.nav.foreldrepenger.domene.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.PersonopplysningerForUttakStub;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

class AvklarteDatoerTjenesteTest {
    private final LocalDate fraDato = LocalDate.now();
    private final LocalDate fraDato2 = LocalDate.now().plusMonths(1);

    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(
        new AbakusInMemoryInntektArbeidYtelseTjeneste());

    private final AvklarteDatoerTjeneste avklarteDatoerTjeneste = new AvklarteDatoerTjeneste(
        repositoryProvider.getUttaksperiodegrenseRepository(), new PersonopplysningerForUttakStub(),
        inntektsmeldingTjeneste);
    private final GrunnlagOppretter grunnlagOppretter = new GrunnlagOppretter(repositoryProvider);

    @Test
    void opprett_avklarte_datoer_for_søknad_med_termindato() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var termindato = LocalDate.of(2019, Month.SEPTEMBER, 1);

        grunnlagOppretter.lagreUttaksgrenser(behandling.getId(), LocalDate.of(2019, Month.AUGUST, 1));

        var input = input(behandling, termindato, null);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        assertThat(avklarteDatoer).isNotNull();
        assertThat(avklarteDatoer.getTerminsdato()).isEqualTo(termindato);
        assertThat(avklarteDatoer.getFødselsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBarnetsDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBrukersDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getOpphørsdatoForMedlemskap()).isNotPresent();
        assertThat(avklarteDatoer.getFørsteLovligeUttaksdato().orElseThrow()).isEqualTo(
            LocalDate.of(2019, Month.MAY, 1));
        assertThat(avklarteDatoer.getOppholdListe()).isEmpty();
    }

    @Test
    void opprett_avklarte_datoer_for_søknad_med_fødselsdato() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var termindato = LocalDate.of(2019, Month.SEPTEMBER, 1);
        var fødselsdato = termindato.plusDays(2);
        grunnlagOppretter.lagreUttaksgrenser(behandling.getId(), LocalDate.of(2019, Month.AUGUST, 1));

        var input = input(behandling, termindato, fødselsdato);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        assertThat(avklarteDatoer).isNotNull();
        assertThat(avklarteDatoer.getTerminsdato()).isEqualTo(termindato);
        assertThat(avklarteDatoer.getFødselsdato()).isEqualTo(Optional.of(fødselsdato));
        assertThat(avklarteDatoer.getBarnetsDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBrukersDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getOpphørsdatoForMedlemskap()).isNotPresent();
        assertThat(avklarteDatoer.getFørsteLovligeUttaksdato().orElseThrow()).isEqualTo(
            LocalDate.of(2019, Month.MAY, 1));
        assertThat(avklarteDatoer.getOppholdListe()).isEmpty();
    }

    @Test
    void opprett_avklarte_datoer_med_opphold_fra_saksbehandler() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var termindato = LocalDate.of(2019, Month.SEPTEMBER, 1);
        grunnlagOppretter.lagreUttaksgrenser(behandling.getId(), LocalDate.of(2019, Month.AUGUST, 1));

        var svpGrunnlagEntitet = grunnlagOppretter.lagTilretteleggingMedOpphold(behandling.getId());
        var input = inputMedSvpGrunnlag(behandling, termindato, svpGrunnlagEntitet);

        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        var oppholdsliste = avklarteDatoer.getOppholdListe();

        assertThat(avklarteDatoer).isNotNull();
        assertThat(oppholdsliste).hasSize(2);
        assertThat(oppholdsliste.get(0).getFom()).isEqualTo(fraDato.plusDays(2));
        assertThat(oppholdsliste.get(0).getTom()).isEqualTo(fraDato.plusDays(4));
        assertThat(oppholdsliste.get(0).getÅrsak()).hasToString("SYKEPENGER");
        assertThat(oppholdsliste.get(1).getFom()).isEqualTo(fraDato2);
        assertThat(oppholdsliste.get(1).getTom()).isEqualTo(fraDato2.plusWeeks(4));
        assertThat(oppholdsliste.get(1).getÅrsak()).hasToString("FERIE");
    }

    private UttakInput input(Behandling behandling, LocalDate termindato, LocalDate fødselsdato) {
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new SvangerskapspengerGrunnlag().medFamilieHendelse(
            FamilieHendelse.forFødsel(termindato, fødselsdato, List.of(new Barn()), 1));
        return new UttakInput(lagReferanse(behandling), null, ytelsespesifiktGrunnlag);
    }

    private UttakInput inputMedSvpGrunnlag(Behandling behandling, LocalDate termindato, SvpGrunnlagEntitet svpGrunnlagEntitet) {
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new SvangerskapspengerGrunnlag().medFamilieHendelse(
                FamilieHendelse.forFødsel(termindato, null, List.of(new Barn()), 1))
            .medSvpGrunnlagEntitet(svpGrunnlagEntitet);
        return new UttakInput(lagReferanse(behandling), null, ytelsespesifiktGrunnlag);
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }
}
