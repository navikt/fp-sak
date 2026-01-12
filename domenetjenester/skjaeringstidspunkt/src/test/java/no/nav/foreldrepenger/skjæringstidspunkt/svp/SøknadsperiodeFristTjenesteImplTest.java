package no.nav.foreldrepenger.skjæringstidspunkt.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.virksomhet.ArbeidType;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;

class SøknadsperiodeFristTjenesteImplTest {

    @Test
    void skal_utlede_skjæringstidspunktet() {
        var forventetSkjæringstidspunkt = LocalDate.of(2019, 7, 10);

        var svp = new SvpTilretteleggingEntitet.Builder()
            .medArbeidType(ArbeidType.FRILANSER)
            .medBehovForTilretteleggingFom(forventetSkjæringstidspunkt)
            .medDelvisTilrettelegging(forventetSkjæringstidspunkt, BigDecimal.valueOf(50), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD)
            .medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD)
            .medHelTilrettelegging(LocalDate.of(2019, 11, 1), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD)
            .medIngenTilrettelegging(LocalDate.of(2019, 11, 25), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD);

        var svpGrunnlag = new SvpGrunnlagEntitet.Builder()
            .medOpprinneligeTilrettelegginger(List.of(svp.build()))
            .medBehandlingId(1337L);

        var dag = SøknadsperiodeFristTjenesteImpl.utledNettoSøknadsperiodeFomFraGrunnlag(svpGrunnlag.build());

        assertThat(dag).hasValueSatisfying(ldi -> assertThat(ldi).isEqualTo(forventetSkjæringstidspunkt));
    }


}
