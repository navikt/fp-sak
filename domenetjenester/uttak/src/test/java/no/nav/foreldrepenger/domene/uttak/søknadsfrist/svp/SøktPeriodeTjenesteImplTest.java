package no.nav.foreldrepenger.domene.uttak.søknadsfrist.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.svp.RegelmodellSøknaderMapper;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.ScenarioMorSøkerSvangerskapspenger;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;

public class SøktPeriodeTjenesteImplTest {

    private final UttakRepositoryProvider repositoryProvider = new UttakRepositoryStubProvider();

    @Test
    public void skal_utlede_skjæringstidspunktet() {
        var forventetResultat = LocalDate.of(2019, 7, 10);

        var scenario = ScenarioMorSøkerSvangerskapspenger.forSvangerskapspenger();

        var behandling = scenario.lagre(repositoryProvider);

        var jordmorsDato = LocalDate.of(2019, Month.APRIL, 1);

        var tjeneste = new SøktPeriodeTjenesteImpl(new RegelmodellSøknaderMapper());
        var svp = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(forventetResultat)
            .medDelvisTilrettelegging(forventetResultat, BigDecimal.valueOf(50))
            .medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30))
            .medHelTilrettelegging(LocalDate.of(2019, 11, 1))
            .medIngenTilrettelegging(LocalDate.of(2019, 11, 25));

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(svp.build()))
            .medBehandlingId(behandling.getId());

        var familieHendelse = FamilieHendelse.forFødsel(LocalDate.of(2019, Month.DECEMBER, 1), null, List.of(new Barn()), 1);
        var ytelsespesifiktGrunnlag = new SvangerskapspengerGrunnlag()
            .medFamilieHendelse(familieHendelse)
            .medSvpGrunnlagEntitet(svpGrunnlag.build());


        var input = new UttakInput(BehandlingReferanse.fra(behandling), null, ytelsespesifiktGrunnlag);
        var dag = tjeneste.finnSøktPeriode(input);

        assertThat(dag).hasValueSatisfying(ldi -> assertThat(ldi.getFomDato()).isEqualTo(forventetResultat));
    }


}
