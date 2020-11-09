package no.nav.foreldrepenger.domene.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.foreldrepenger.dbstoette.FPsakEntityManagerAwareExtension;
import no.nav.foreldrepenger.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;

@ExtendWith(FPsakEntityManagerAwareExtension.class)
public class AvklarteDatoerTjenesteTest {

    private GrunnlagOppretter grunnlagOppretter;
    private AvklarteDatoerTjeneste avklarteDatoerTjeneste;

    @BeforeEach
    void setUp(EntityManager entityManager) {
        UttakRepositoryProvider repositoryProvider = new UttakRepositoryProvider(entityManager);
        BasisPersonopplysningTjeneste basisPersonopplysningTjeneste = new BasisPersonopplysningTjeneste(new PersonopplysningRepository(entityManager));
        InntektsmeldingTjeneste inntektsmeldingTjeneste = new InntektsmeldingTjeneste(new AbakusInMemoryInntektArbeidYtelseTjeneste());
        avklarteDatoerTjeneste = new AvklarteDatoerTjeneste(repositoryProvider.getUttaksperiodegrenseRepository(),
            basisPersonopplysningTjeneste, inntektsmeldingTjeneste);
        grunnlagOppretter = new GrunnlagOppretter(repositoryProvider);
    }

    @Test
    public void opprett_avklarte_datoer_for_søknad_med_termindato() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var termindato = LocalDate.of(2019, Month.SEPTEMBER, 1);
        grunnlagOppretter.lagreUttaksgrenser(behandling.getId(), LocalDate.of(2019, Month.MAY, 1), LocalDate.of(2019, Month.AUGUST, 1));

        UttakInput input = input(behandling, termindato, null);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        assertThat(avklarteDatoer).isNotNull();
        assertThat(avklarteDatoer.getTerminsdato()).isEqualTo(termindato);
        assertThat(avklarteDatoer.getFødselsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBarnetsDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBrukersDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getOpphørsdatoForMedlemskap()).isNotPresent();
        assertThat(avklarteDatoer.getFørsteLovligeUttaksdato().orElseThrow()).isEqualTo(LocalDate.of(2019, Month.MAY, 1));
        assertThat(avklarteDatoer.getFerier()).hasSize(0);
    }

    private UttakInput input(Behandling behandling, LocalDate termindato, LocalDate fødselsdato) {
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new SvangerskapspengerGrunnlag().medFamilieHendelse(FamilieHendelse.forFødsel(termindato, fødselsdato, List.of(new Barn()), 1));
        return new UttakInput(lagReferanse(behandling), null, ytelsespesifiktGrunnlag);
    }

    private BehandlingReferanse lagReferanse(Behandling behandling) {
        return BehandlingReferanse.fra(behandling);
    }

    @Test
    public void opprett_avklarte_datoer_for_søknad_med_fødselsdato() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var termindato = LocalDate.of(2019, Month.SEPTEMBER, 1);
        var fødselsdato = termindato.plusDays(2);
        grunnlagOppretter.lagreUttaksgrenser(behandling.getId(), LocalDate.of(2019, Month.MAY, 1), LocalDate.of(2019, Month.AUGUST, 1));

        var input = input(behandling, termindato, fødselsdato);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        assertThat(avklarteDatoer).isNotNull();
        assertThat(avklarteDatoer.getTerminsdato()).isEqualTo(termindato);
        assertThat(avklarteDatoer.getFødselsdato()).isEqualTo(Optional.of(fødselsdato));
        assertThat(avklarteDatoer.getBarnetsDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBrukersDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getOpphørsdatoForMedlemskap()).isNotPresent();
        assertThat(avklarteDatoer.getFørsteLovligeUttaksdato().orElseThrow()).isEqualTo(LocalDate.of(2019, Month.MAY, 1));
        assertThat(avklarteDatoer.getFerier()).hasSize(0);
    }

}
