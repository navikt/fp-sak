package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.EntityManagerAwareTest;
import no.nav.foreldrepenger.domene.typer.AktørId;

public class MedlemskapVilkårPeriodeRepositoryTest extends EntityManagerAwareTest {

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
    public void skal_lagre_overstyring() {
        Behandling behandling = lagBehandling();
        MedlemskapVilkårPeriodeGrunnlagEntitet.Builder builderIkkeOppfylt = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        MedlemskapsvilkårPeriodeEntitet.Builder periodeBuilderIkkeOppylt = builderIkkeOppfylt.getPeriodeBuilder();
        periodeBuilderIkkeOppylt.opprettOverstryingAvslag(LocalDate.now(), Avslagsårsak.SØKER_ER_IKKE_MEDLEM);
        builderIkkeOppfylt.medMedlemskapsvilkårPeriode(periodeBuilderIkkeOppylt);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builderIkkeOppfylt);

        Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> grunnlag = medlemskapVilkårPeriodeRepository.hentAggregatHvisEksisterer(behandling);

        assertThat(grunnlag).isPresent();
        Optional<LocalDate> localDate = medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
        assertThat(localDate).isEqualTo(Optional.of(LocalDate.now()));

        MedlemskapVilkårPeriodeGrunnlagEntitet.Builder builderOppfylt = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);

        MedlemskapsvilkårPeriodeEntitet.Builder periodeBuilderOppfylt = builderOppfylt.getPeriodeBuilder();
        periodeBuilderOppfylt.opprettOverstryingOppfylt(LocalDate.now());
        builderOppfylt.medMedlemskapsvilkårPeriode(periodeBuilderOppfylt);

        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builderOppfylt);
        Optional<LocalDate> localDate2 = medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
        assertThat(localDate2).isNotPresent();
    }

    private Behandling lagBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNyNB(AktørId.dummy()));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        Behandlingsresultat behandlingsresultat = behandlingsresultatBuilder.buildFor(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
        return behandling;
    }
}
