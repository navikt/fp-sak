package no.nav.foreldrepenger.domene.uttak.svp;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.tilrettelegging.SvpGrunnlagEntitet;
import no.nav.foreldrepenger.domene.uttak.input.Barn;
import no.nav.foreldrepenger.domene.uttak.input.FamilieHendelse;
import no.nav.foreldrepenger.domene.uttak.input.SvangerskapspengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.domene.uttak.input.YtelsespesifiktGrunnlag;
import no.nav.foreldrepenger.domene.uttak.testutilities.behandling.UttakRepositoryProviderForTest;
import no.nav.svangerskapspenger.domene.søknad.IngenTilrettelegging;

public class RegelmodellSøknaderMapperTest {

    private final RegelmodellSøknaderMapper regelmodellSøknaderMapper = new RegelmodellSøknaderMapper();
    private final UttakRepositoryProviderForTest repositoryProvider = new UttakRepositoryProviderForTest();
    private final GrunnlagOppretter grunnlagOppretter = new GrunnlagOppretter(repositoryProvider);
    private final LocalDate skjæringstidspunkt = LocalDate.now();

    @Test
    public void skal_opprette_regelmodell_søknader() {
        var behandling = grunnlagOppretter.lagreBehandling();
        SvpGrunnlagEntitet svpGrunnlagEntitet = grunnlagOppretter.lagTilrettelegging(behandling);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        FamilieHendelse familieHendelse = FamilieHendelse.forFødsel(LocalDate.of(2019, Month.SEPTEMBER, 1), null, List.of(new Barn()), 1);
        YtelsespesifiktGrunnlag ytelsespesifiktGrunnlag = new SvangerskapspengerGrunnlag()
            .medFamilieHendelse(familieHendelse)
            .medSvpGrunnlagEntitet(svpGrunnlagEntitet);
        UttakInput input = new UttakInput(ref, null, ytelsespesifiktGrunnlag);
        var søknader = regelmodellSøknaderMapper.hentSøknader(input);

        assertThat(søknader).hasSize(1);
        var søknad = søknader.get(0);
        assertThat(søknad.getTilrettelegginger()).hasSize(1);
        var tilrettelegging = søknad.getTilrettelegginger().get(0);
        assertThat(tilrettelegging).isInstanceOf(IngenTilrettelegging.class);
    }
}
