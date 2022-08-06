package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;

public class SøknadsperiodeFristTjenesteImplTest {

    @Test
    public void skal_utlede_skjæringstidspunktet() {
        var forventetResultat = LocalDate.of(2019, 7, 10);

        var jordmorsDato = LocalDate.of(2019, Month.APRIL, 1);

        var svp = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(forventetResultat)
            .medDelvisTilrettelegging(forventetResultat, BigDecimal.valueOf(50))
            .medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30))
            .medHelTilrettelegging(LocalDate.of(2019, 11, 1))
            .medIngenTilrettelegging(LocalDate.of(2019, 11, 25));

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(svp.build()))
            .medBehandlingId(1337L);

        var dag = SøknadsperiodeFristTjenesteImpl.utledNettoSøknadsperiodeFomFraGrunnlag(svpGrunnlag.build());

        assertThat(dag).hasValueSatisfying(ldi -> assertThat(ldi).isEqualTo(forventetResultat));
    }


}
