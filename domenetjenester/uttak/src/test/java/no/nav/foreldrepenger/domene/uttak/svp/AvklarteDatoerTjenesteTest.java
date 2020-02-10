package no.nav.foreldrepenger.domene.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.BasisPersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class AvklarteDatoerTjenesteTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private UttakRepositoryProvider repositoryProvider;

    @Inject
    private BasisPersonopplysningTjeneste basisPersonopplysningTjeneste;

    @Inject
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    private GrunnlagOppretter grunnlagOppretter;
    private AvklarteDatoerTjeneste avklarteDatoerTjeneste;

    @Before
    public void setup() {
        avklarteDatoerTjeneste = new AvklarteDatoerTjeneste(repositoryProvider, basisPersonopplysningTjeneste, inntektsmeldingTjeneste);
        grunnlagOppretter = new GrunnlagOppretter(repositoryProvider);
    }

    @Test
    public void opprett_avklarte_datoer_for_søknad_med_termindato() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var termindato = LocalDate.of(2019, Month.SEPTEMBER, 1);
        grunnlagOppretter.lagreUttaksgrenser(behandling, LocalDate.of(2019, Month.MAY, 1), LocalDate.of(2019, Month.AUGUST, 1));

        UttakInput input = input(behandling, termindato, null);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        assertThat(avklarteDatoer).isNotNull();
        assertThat(avklarteDatoer.getTerminsdato()).isEqualTo(termindato);
        assertThat(avklarteDatoer.getFødselsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBarnetsDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBrukersDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getOpphørsdatoForMedlemskap()).isNotPresent();
        assertThat(avklarteDatoer.getFørsteLovligeUttaksdato().get()).isEqualTo(LocalDate.of(2019, Month.MAY, 1));
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
        grunnlagOppretter.lagreUttaksgrenser(behandling, LocalDate.of(2019, Month.MAY, 1), LocalDate.of(2019, Month.AUGUST, 1));

        var input = input(behandling, termindato, fødselsdato);
        var avklarteDatoer = avklarteDatoerTjeneste.finn(input);

        assertThat(avklarteDatoer).isNotNull();
        assertThat(avklarteDatoer.getTerminsdato()).isEqualTo(termindato);
        assertThat(avklarteDatoer.getFødselsdato()).isEqualTo(Optional.of(fødselsdato));
        assertThat(avklarteDatoer.getBarnetsDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getBrukersDødsdato()).isNotPresent();
        assertThat(avklarteDatoer.getOpphørsdatoForMedlemskap()).isNotPresent();
        assertThat(avklarteDatoer.getFørsteLovligeUttaksdato().get()).isEqualTo(LocalDate.of(2019, Month.MAY, 1));
        assertThat(avklarteDatoer.getFerier()).hasSize(0);
    }

}
