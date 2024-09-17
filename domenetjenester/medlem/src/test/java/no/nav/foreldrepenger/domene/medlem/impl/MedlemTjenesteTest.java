package no.nav.foreldrepenger.domene.medlem.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.dbstoette.CdiDbAwareTest;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;

@CdiDbAwareTest
class MedlemTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider provider;
    @Inject
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private MedlemTjeneste tjeneste;

    @Test
    void skal_returnere_empty_når_vilkåret_er_overstyrt_til_godkjent() {
        // Arrange
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

        var now = LocalDate.now();

        var vilkår = VilkårResultat.builderFraEksisterende(behandlingsresultat.getVilkårResultat());
        vilkår.leggTilVilkårAvslått(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallMerknad.VM_1025);
        vilkår.overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);

        var vilkårResultat = vilkår.buildFor(behandling);
        behandlingRepository.lagre(vilkårResultat, lås);

        var grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var mbuilder = grBuilder.getPeriodeBuilder();
        var periode = mbuilder.getBuilderForVurderingsdato(now);
        periode.medVilkårUtfall(VilkårUtfallType.IKKE_OPPFYLT);
        mbuilder.leggTil(periode);
        grBuilder.medMedlemskapsvilkårPeriode(mbuilder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);

        var grBuilder2 = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var mbuilder2 = grBuilder2.getPeriodeBuilder();
        var periode2 = mbuilder2.getBuilderForVurderingsdato(now);
        periode2.medVilkårUtfall(VilkårUtfallType.OPPFYLT);
        mbuilder2.leggTil(periode2);
        grBuilder2.medMedlemskapsvilkårPeriode(mbuilder2);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder2);

        // Act
        var localDate = tjeneste.hentOpphørsdatoHvisEksisterer(behandling.getId());

        // Assert
        assertThat(localDate).isEmpty();
    }


}
