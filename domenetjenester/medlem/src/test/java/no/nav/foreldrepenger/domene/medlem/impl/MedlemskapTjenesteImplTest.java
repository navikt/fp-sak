package no.nav.foreldrepenger.domene.medlem.impl;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.util.Tuple;

@RunWith(CdiRunner.class)
public class MedlemskapTjenesteImplTest {

    @Rule
    public UnittestRepositoryRule rule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(rule.getEntityManager());
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository = provider.getMedlemskapVilkårPeriodeRepository();
    private FagsakRepository fagsakRepository = new FagsakRepository(rule.getEntityManager());
    private BehandlingRepository behandlingRepository = provider.getBehandlingRepository();

    @Inject
    private MedlemTjeneste tjeneste;


    @Test
    public void skal_returnere_empty_når_vilkåret_er_overstyrt_til_godkjent() {
        // Arrange
        Tuple<Behandling, Behandlingsresultat> tuple = lagBehandling();
        LocalDate now = LocalDate.now();
        Behandling behandling = tuple.getElement1();
        Behandlingsresultat behandlingsresultat = tuple.getElement2();

        VilkårResultat.Builder vilkår = VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat());
        vilkår.leggTilVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.IKKE_OPPFYLT);
        vilkår.overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT, null);

        VilkårResultat vilkårResultat = vilkår.buildFor(behandling);
        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(vilkårResultat, lås);

        MedlemskapVilkårPeriodeGrunnlagEntitet.Builder grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        MedlemskapsvilkårPeriodeEntitet.Builder builder = grBuilder.getPeriodeBuilder();
        MedlemskapsvilkårPerioderEntitet.Builder periode = builder.getBuilderForVurderingsdato(now);
        periode.medVilkårUtfall(VilkårUtfallType.IKKE_OPPFYLT);
        builder.leggTil(periode);
        grBuilder.medMedlemskapsvilkårPeriode(builder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);

        // Act
        Optional<LocalDate> localDate = tjeneste.hentOpphørsdatoHvisEksisterer(behandling.getId());

        // Assert
        assertThat(localDate).isEmpty();
    }

    private Tuple<Behandling, Behandlingsresultat> lagBehandling() {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, NavBruker.opprettNy(AktørId.dummy(), Språkkode.NB));
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);
        Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderForInngangsvilkår();
        Behandlingsresultat behandlingsresultat = behandlingsresultatBuilder.buildFor(behandling);
        behandlingRepository.lagre(behandlingsresultat.getVilkårResultat(), lås);
        behandlingRepository.lagre(behandling, lås);
        return new Tuple<>(behandling, behandlingsresultat);
    }
}
