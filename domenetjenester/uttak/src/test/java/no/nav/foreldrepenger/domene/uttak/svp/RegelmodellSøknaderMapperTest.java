package no.nav.foreldrepenger.domene.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryStubProvider;
import no.nav.svangerskapspenger.domene.søknad.IngenTilrettelegging;

class RegelmodellSøknaderMapperTest {
    private final RegelmodellSøknaderMapper regelmodellSøknaderMapper = new RegelmodellSøknaderMapper();
    private final UttakRepositoryStubProvider repositoryProvider = new UttakRepositoryStubProvider();
    private final GrunnlagOppretter grunnlagOppretter = new GrunnlagOppretter(repositoryProvider);
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    @Test
    void skal_opprette_regelmodell_søknader() {
        var behandling = grunnlagOppretter.lagreBehandling();
        var svpGrunnlagEntitet = grunnlagOppretter.lagTilrettelegging(behandling);

        var ref = BehandlingReferanse.fra(behandling);
        var stp = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(skjæringstidspunkt).build();
        var familieHendelse = FamilieHendelse.forFødsel(LocalDate.of(2019, Month.SEPTEMBER, 1), null, List.of(new Barn()), 1);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new SvangerskapspengerGrunnlag()
            .medFamilieHendelse(familieHendelse)
            .medSvpGrunnlagEntitet(svpGrunnlagEntitet);
        var input = new UttakInput(ref, stp, null, ytelsespesifiktGrunnlag);
        var søknader = regelmodellSøknaderMapper.hentSøknader(input);

        assertThat(søknader).hasSize(1);
        var søknad = søknader.get(0);
        assertThat(søknad.getTilrettelegginger()).hasSize(1);
        var tilrettelegging = søknad.getTilrettelegginger().get(0);
        assertThat(tilrettelegging).isInstanceOf(IngenTilrettelegging.class);
    }
}
