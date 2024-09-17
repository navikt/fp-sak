package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

class MedlemskapVilkårPeriodeRepositoryTest extends EntityManagerAwareTest {

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;

    @BeforeEach
    void setUp() {
        var entityManager = getEntityManager();
        medlemskapVilkårPeriodeRepository = new MedlemskapVilkårPeriodeRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
    }

    @Test
    void skal_lagre_overstyring() {
        var behandling = lagBehandling();
        var builderIkkeOppfylt = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var periodeBuilderIkkeOppylt = builderIkkeOppfylt.getPeriodeBuilder();
        periodeBuilderIkkeOppylt.opprettOverstyringAvslag(LocalDate.now(), Avslagsårsak.SØKER_ER_IKKE_MEDLEM);
        builderIkkeOppfylt.medMedlemskapsvilkårPeriode(periodeBuilderIkkeOppylt);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builderIkkeOppfylt);

        var grunnlag = medlemskapVilkårPeriodeRepository.hentAggregatHvisEksisterer(behandling);

        assertThat(grunnlag).isPresent();
        var localDate = medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
        assertThat(localDate).isEqualTo(Optional.of(LocalDate.now()));

        var builderOppfylt = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);

        var periodeBuilderOppfylt = builderOppfylt.getPeriodeBuilder();
        periodeBuilderOppfylt.opprettOverstyringOppfylt(LocalDate.now());
        builderOppfylt.medMedlemskapsvilkårPeriode(periodeBuilderOppfylt);

        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builderOppfylt);
        var localDate2 = medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
        assertThat(localDate2).isNotPresent();
    }

    private Behandling lagBehandling() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();

        var lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        var behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        var behandlingsresultat = behandlingsresultatBuilder.buildFor(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }
}
