package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringMedlemskapsvilkåretDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretOverstyringshåndterer extends InngangsvilkårOverstyringshåndterer<OverstyringMedlemskapsvilkåretDto> {

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    MedlemskapsvilkåretOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MedlemskapsvilkåretOverstyringshåndterer(HistorikkTjenesteAdapter historikkAdapter,
                                                    MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste) {
        super(historikkAdapter,
            AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            inngangsvilkårTjeneste);
        this.medlemskapVilkårPeriodeRepository = medlemskapVilkårPeriodeRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedlemskapsvilkåretDto dto) {
        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
    }

    @Override
    protected void precondition(Behandling behandling, OverstyringMedlemskapsvilkåretDto dto) {

        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getUtledetSkjæringstidspunkt();

        var grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var periodeBuilder = grBuilder.getPeriodeBuilder();
        var periode = periodeBuilder.getBuilderForVurderingsdato(skjæringstidspunkt);

        if (dto.getErVilkarOk()) {
            periode.medVilkårUtfall(VilkårUtfallType.OPPFYLT);
        } else {
            var avslagsårsak = VilkårUtfallMerknad.fraKode(dto.getAvslagskode());
            periode.medVilkårUtfall(VilkårUtfallType.IKKE_OPPFYLT);
            periode.medVilkårUtfallMerknad(avslagsårsak);
        }
        periodeBuilder.leggTil(periode);
        grBuilder.medMedlemskapsvilkårPeriode(periodeBuilder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);
    }
}
