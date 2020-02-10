package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretLøpendeDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretLøpendeOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringMedlemskapsvilkåretLøpendeDto> {

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private BehandlingRepository behandlingRepository;

    MedlemskapsvilkåretLøpendeOverstyringshåndterer() {
        // for CDI proxy
    }

    @Inject
    public MedlemskapsvilkåretLøpendeOverstyringshåndterer(BehandlingRepositoryProvider repositoryProvider,
                                                           HistorikkTjenesteAdapter historikkAdapter) {
        super(historikkAdapter,
            AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET_LØPENDE);
        this.medlemskapVilkårPeriodeRepository = repositoryProvider.getMedlemskapVilkårPeriodeRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedlemskapsvilkåretLøpendeDto dto) {
        HistorikkEndretFeltVerdiType tilVerdi = dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;
        HistorikkEndretFeltVerdiType fraVerdi = dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT;

        getHistorikkAdapter().tekstBuilder()
            .medHendelse(HistorikkinnslagType.OVERSTYRT)
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP_LØPENDE)
            .medEndretFelt(HistorikkEndretFeltType.OVERSTYRT_VURDERING, fraVerdi, tilVerdi);
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringMedlemskapsvilkåretLøpendeDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        MedlemskapVilkårPeriodeGrunnlagEntitet.Builder grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        MedlemskapsvilkårPeriodeEntitet.Builder periodeBuilder = grBuilder.getPeriodeBuilder();

        VilkårResultat.Builder vilkårBuilder = VilkårResultat.builderFraEksisterende(behandling.getBehandlingsresultat().getVilkårResultat());
        if (dto.getErVilkarOk()) {
            periodeBuilder.opprettOverstryingOppfylt(dto.getOverstryingsdato());
            vilkårBuilder.leggTilVilkårResultat(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, VilkårUtfallType.OPPFYLT, null, null, null, false, true, null, null);
        } else {
            Avslagsårsak avslagsårsak = Avslagsårsak.fraKode(dto.getAvslagskode());
            periodeBuilder.opprettOverstryingAvslag(dto.getOverstryingsdato(), avslagsårsak);
            vilkårBuilder.leggTilVilkårResultat(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, VilkårUtfallType.IKKE_OPPFYLT, null, null, avslagsårsak, false, true,
                null, null);
        }
        BehandlingLås lås = kontekst.getSkriveLås();
        grBuilder.medMedlemskapsvilkårPeriode(periodeBuilder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);
        behandlingRepository.lagre(vilkårBuilder.buildFor(behandling), lås);
        return OppdateringResultat.utenOveropp();
    }
}
