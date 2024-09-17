package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretLøpendeDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretLøpendeOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringMedlemskapsvilkåretLøpendeDto> {

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
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
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedlemskapsvilkåretLøpendeDto dto) {
        var tilVerdi = dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;
        var fraVerdi = dto.getErVilkarOk() ? HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT;

        getHistorikkAdapter().tekstBuilder()
            .medHendelse(HistorikkinnslagType.OVERSTYRT)
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP_LØPENDE)
            .medEndretFelt(HistorikkEndretFeltType.OVERSTYRT_VURDERING, fraVerdi, tilVerdi);
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringMedlemskapsvilkåretLøpendeDto dto, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var grBuilder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
        var periodeBuilder = grBuilder.getPeriodeBuilder();

        var vilkårResultatBuilder = VilkårResultat.builderFraEksisterende(behandlingsresultatRepository.hent(behandling.getId()).getVilkårResultat());
        if (dto.getErVilkarOk()) {
            periodeBuilder.opprettOverstyringOppfylt(dto.getOverstryingsdato());
            vilkårResultatBuilder.overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);
        } else {
            var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));
            periodeBuilder.opprettOverstyringAvslag(dto.getOverstryingsdato(), avslagsårsak);
            vilkårResultatBuilder.overstyrVilkår(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, VilkårUtfallType.IKKE_OPPFYLT, avslagsårsak);
        }
        var lås = kontekst.getSkriveLås();
        grBuilder.medMedlemskapsvilkårPeriode(periodeBuilder);
        medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, grBuilder);
        behandlingRepository.lagre(vilkårResultatBuilder.buildFor(behandling), lås);
        return OppdateringResultat.utenOverhopp();
    }
}
