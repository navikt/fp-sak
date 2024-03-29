package no.nav.foreldrepenger.skjæringstidspunkt.svp;


import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde;

class SkjæringstidspunktTjenesteImplTest {

    @Test
    void skal_utlede_skjæringstidspunktet() {
        var forventetSkjæringstidspunkt = LocalDate.of(2019, 7, 10);

        var svpGrunnlagEntitet = new SvpGrunnlagEntitet.Builder();
        var svp = new SvpTilretteleggingEntitet.Builder();
        svp.medBehovForTilretteleggingFom(forventetSkjæringstidspunkt);
        svp.medDelvisTilrettelegging(forventetSkjæringstidspunkt, BigDecimal.valueOf(50), forventetSkjæringstidspunkt, no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde.SØKNAD);
        svp.medDelvisTilrettelegging(LocalDate.of(2019, 9, 17), BigDecimal.valueOf(30), forventetSkjæringstidspunkt, no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpTilretteleggingFomKilde.SØKNAD);
        svp.medHelTilrettelegging(LocalDate.of(2019, 11, 1), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD);
        svp.medIngenTilrettelegging(LocalDate.of(2019, 11, 25), forventetSkjæringstidspunkt, SvpTilretteleggingFomKilde.SØKNAD);

        var tilretteleggingEntitet = svp.build();
        svpGrunnlagEntitet.medOpprinneligeTilrettelegginger(List.of(tilretteleggingEntitet));
        svpGrunnlagEntitet.medBehandlingId(1337L);

        var dag = SkjæringstidspunktTjenesteImpl.utledBasertPåGrunnlag(svpGrunnlagEntitet.build());

        assertThat(dag).isEqualTo(forventetSkjæringstidspunkt);
    }
}
