package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;

class SøknadsperiodeFristTjenesteImplTest {

    @Test
    void skal_utlede_skjæringstidspunktet() {
        var forventetSkjæringstidspunkt = LocalDate.of(2019, 7, 10);

        var jordmorsDato = LocalDate.of(2019, Month.APRIL, 1);

        var svp = new SvpTilretteleggingEntitet.Builder()
            .medBehovForTilretteleggingFom(forventetSkjæringstidspunkt)
            .medDelvisTilrettelegging(forventetSkjæringstidspunkt, BigDecimal.valueOf(50), forventetSkjæringstidspunkt)
            .medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30), forventetSkjæringstidspunkt)
            .medHelTilrettelegging(LocalDate.of(2019, 11, 1), forventetSkjæringstidspunkt)
            .medIngenTilrettelegging(LocalDate.of(2019, 11, 25), forventetSkjæringstidspunkt);

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(svp.build()))
            .medBehandlingId(1337L);

        var dag = SøknadsperiodeFristTjenesteImpl.utledNettoSøknadsperiodeFomFraGrunnlag(svpGrunnlag.build());

        assertThat(dag).hasValueSatisfying(ldi -> assertThat(ldi).isEqualTo(forventetSkjæringstidspunkt));
    }


}
