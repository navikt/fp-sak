package no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AbstractOverstyringshåndterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandling.aksjonspunkt.Overstyringshåndterer;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.inngangsvilkaar.InngangsvilkårTjeneste;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt.MedlemskapAksjonspunktFellesTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.vilkår.aksjonspunkt.dto.OverstyringMedlemskapsvilkåretDto;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyringMedlemskapsvilkåretDto.class, adapter = Overstyringshåndterer.class)
public class MedlemskapsvilkåretOverstyringshåndterer extends AbstractOverstyringshåndterer<OverstyringMedlemskapsvilkåretDto> {

    private static final Environment ENV = Environment.current(); // TODO medlemskap2 standardisere etter omlegging

    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste;
    private InngangsvilkårTjeneste inngangsvilkårTjeneste;

    @Inject
    public MedlemskapsvilkåretOverstyringshåndterer(MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository,
                                                    SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                    MedlemskapAksjonspunktFellesTjeneste medlemskapAksjonspunktFellesTjeneste,
                                                    InngangsvilkårTjeneste inngangsvilkårTjeneste,
                                                    HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        super(historikkTjenesteAdapter, AksjonspunktDefinisjon.OVERSTYRING_AV_MEDLEMSKAPSVILKÅRET);
        this.medlemskapVilkårPeriodeRepository = medlemskapVilkårPeriodeRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.medlemskapAksjonspunktFellesTjeneste = medlemskapAksjonspunktFellesTjeneste;
        this.inngangsvilkårTjeneste = inngangsvilkårTjeneste;
    }

    MedlemskapsvilkåretOverstyringshåndterer() {
    }

    @Override
    public OppdateringResultat håndterOverstyring(OverstyringMedlemskapsvilkåretDto dto,
                                                  Behandling behandling,
                                                  BehandlingskontrollKontekst kontekst) {
        if (ENV.isProd()) {
            return legacy(dto, behandling, kontekst);

        } else {
            var avslagsårsak = Avslagsårsak.fraKode(dto.getAvslagskode());
            var oppdateringResultat = medlemskapAksjonspunktFellesTjeneste.oppdater(kontekst.getBehandlingId(), avslagsårsak, dto.getOpphørFom(),
                dto.getBegrunnelse(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);
            if (oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().size() != 1) {
                throw new IllegalArgumentException("Forventer bare ett vilkårsutfall");
            }
            var utfall = oppdateringResultat.getVilkårUtfallSomSkalLeggesTil().getFirst().getVilkårUtfallType();
            inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), VilkårType.MEDLEMSKAPSVILKÅRET, utfall,
                avslagsårsak == null ? Avslagsårsak.UDEFINERT : avslagsårsak, kontekst);
            return oppdateringResultat;
        }
    }

    private OppdateringResultat legacy(OverstyringMedlemskapsvilkåretDto dto,
                                       Behandling behandling,
                                       BehandlingskontrollKontekst kontekst) {
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

        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT : VilkårUtfallType.IKKE_OPPFYLT;
        var avslagsårsak = dto.getErVilkarOk() ? Avslagsårsak.UDEFINERT :
            Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
                .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));

        inngangsvilkårTjeneste.overstyrAksjonspunkt(behandling.getId(), VilkårType.MEDLEMSKAPSVILKÅRET, utfall, avslagsårsak, kontekst);

        lagHistorikkInnslagForOverstyrtVilkår(dto.getBegrunnelse(), dto.getErVilkarOk(), SkjermlenkeType.PUNKT_FOR_MEDLEMSKAP);

        if (utfall.equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            return OppdateringResultat.medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR);
        }

        return OppdateringResultat.utenOverhopp();
    }

    @Override
    protected void lagHistorikkInnslag(Behandling behandling, OverstyringMedlemskapsvilkåretDto dto) {

    }
}
